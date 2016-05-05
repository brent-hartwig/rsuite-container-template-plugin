package com.rsicms.rsuite.containerWizard.webService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.ContentAssemblyBuilder;
import com.reallysi.rsuite.api.control.ContentAssemblyBuilderResults;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.InvokeWebServiceAction;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;
import com.reallysi.rsuite.api.security.ACL;
import com.reallysi.rsuite.api.xml.RSuiteNamespaces;
import com.reallysi.rsuite.api.xml.XPathEvaluator;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.SecurityService;
import com.rsicms.rsuite.containerWizard.AclMap;
import com.rsicms.rsuite.containerWizard.ContainerWizard;
import com.rsicms.rsuite.containerWizard.ContainerWizardConfUtils;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.FutureManagedObject;
import com.rsicms.rsuite.containerWizard.advisors.mo.LocalManagedObjectAdvisor;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerConf;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.Page;
import com.rsicms.rsuite.containerWizard.jaxb.PrimaryContainer;
import com.rsicms.rsuite.containerWizard.jaxb.XmlMoConf;
import com.rsicms.rsuite.utils.mo.MOUtils;
import com.rsicms.rsuite.utils.webService.BaseWebService;
import com.rsicms.rsuite.utils.webService.CallArgumentUtils;
import com.rsicms.rsuite.utils.webService.WebServiceUtilsMessageProperties;
import com.rsicms.rsuite.utils.xml.DomUtils;
import com.rsicms.rsuite.utils.xml.XPathUtils;

public class InvokeContainerWizardWebService extends BaseWebService
    implements ContainerWizardConstants {

  private static Log log = LogFactory.getLog(InvokeContainerWizardWebService.class);

  protected User superUser;

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    Date start = new Date();
    try {
      // if (log.isDebugEnabled()) {
      CallArgumentUtils.logArguments(args, log);
      // }

      Session session = context.getSession();
      User user = session.getUser();
      this.superUser = context.getAuthorizationService().getSystemUser();

      // Load the wizard configuration.
      // By loading each time, technically it could change mid-instance.
      String confAlias = args.getFirstString(PARAM_NAME_CONF_ALIAS);
      ManagedObject confMo = ContainerWizardConfUtils.getContainerWizardConfMo(user,
          context.getManagedObjectService(), confAlias);
      ContainerWizardConf conf = ContainerWizardConfUtils.getContainerWizardConf(confMo);

      // Construct or reconstruct an instance of the container wizard
      ContainerWizard wizard;
      String serialiazedWizard = args.getFirstString(PARAM_NAME_CONTAINER_WIZARD);
      if (StringUtils.isBlank(serialiazedWizard)) {
        wizard = new ContainerWizard();
      } else {
        wizard = ContainerWizard.deserialize(serialiazedWizard);
      }

      // Retain values provided by the user.
      retainUserInput(wizard, args);
      log.info(wizard.getInfo());

      // Is there another page to display?
      RestResult restResult;
      Integer pageIdx = args.getFirstInteger(PARAM_NAME_NEXT_PAGE_IDX, -1);
      boolean reachedLastSubPage = args.getFirstBoolean(PARAM_NAME_REACHED_LAST_SUB_PAGE, false);
      if (pageIdx >= 0) {
        // Bump the page index up by one if we've processed the last sub page.
        if (reachedLastSubPage) {
          pageIdx++;
        }

        List<Page> pageList = conf.getPages().getPage();
        if (pageIdx >= pageList.size()) {
          return getErrorResult("The wizard configuration defines " + pageList.size()
              + " pages. Unable to process page " + (pageIdx + 1) + ".");
        }

        Page page = pageList.get(pageIdx);
        log.info("page is null ?= " + (page == null));
        boolean pageHasSubPages = page.isSubPages();
        Integer subPageIdx = args.getFirstInteger(PARAM_NAME_NEXT_SUB_PAGE_IDX, 0);
        log.info("reachedLastSubPage: " + reachedLastSubPage);

        restResult = new RestResult();

        log.info("form id: " + page.getFormId());
        log.info("action id: " + page.getActionId());

        // Set up page advancement for next web service invocation.
        Integer nextPageIdx = -1;
        boolean setNextPageIdx = false;
        boolean setNextSubPageIdx = false;
        if (!reachedLastSubPage && (pageHasSubPages || subPageIdx > 0)) {
          // Stay on the same page.
          nextPageIdx = pageIdx;
          // Only tell the form. Form needs to provide to the web service.
          setNextSubPageIdx = true;
        } else if (pageList.size() > pageIdx + 1) {
          nextPageIdx = pageIdx + 1;
        }
        // Only set the page index param if there is a next page.
        if (nextPageIdx >= 0) {
          setNextPageIdx = true;
        }

        if (StringUtils.isNotBlank(page.getFormId())) {

          log.info("Requesting a form...");

          InvokeWebServiceAction serviceAction =
              new InvokeWebServiceAction(args.getFirstString(PARAM_NAME_API_NAME));
          serviceAction.setFormId(page.getFormId());

          serviceAction.addServiceParameter(PARAM_NAME_CONF_ALIAS, confAlias);
          serviceAction.addServiceParameter(PARAM_NAME_CONTAINER_WIZARD, wizard.serialize());

          if (setNextPageIdx) {
            log.info("Setting next page index to " + nextPageIdx);
            serviceAction.addServiceParameter(PARAM_NAME_NEXT_PAGE_IDX,
                String.valueOf(nextPageIdx));
          }
          if (setNextSubPageIdx) {
            log.info("Setting next sub page index to " + subPageIdx);
            serviceAction.addFormParameter(PARAM_NAME_NEXT_SUB_PAGE_IDX,
                String.valueOf(subPageIdx));
          }

          // Make the web service string params also available to the form.
          for (CallArgument arg : serviceAction.getServiceParameters().values()) {
            if (StringUtils.isNotBlank(arg.getValue())) {
              serviceAction.addFormParameter(arg.getName(), arg.getValue());
            }
          }

          restResult.addAction(serviceAction);
        } else if (StringUtils.isNotBlank(page.getActionId())) {

          log.info("Requesting an action");

          RestResult result = new RestResult();
          UserInterfaceAction wizardPage = new UserInterfaceAction(page.getActionId());

          // TODO: use constants
          wizardPage.addProperty(PARAM_NAME_CONF_ALIAS, args.getFirstString(PARAM_NAME_CONF_ALIAS));
          wizardPage.addProperty("remoteApiName", args.getFirstString(PARAM_NAME_API_NAME));
          wizardPage.addProperty(PARAM_NAME_CONTAINER_WIZARD, wizard.serialize());

          // if (setNextPageIdx) {
          // log.info("Setting nextPageIdx on form");
          // wizardPage.addProperty(PARAM_NAME_NEXT_PAGE_IDX, String.valueOf(nextPageIdx));
          // }

          if (setNextPageIdx) {
            log.info("Setting next page index to " + nextPageIdx);
            wizardPage.addProperty(PARAM_NAME_NEXT_PAGE_IDX, nextPageIdx);
          }

          // TODO: use setNextSubPageIdx?
          if (page.isSubPages()) {
            Integer nextSubPageIdx = args.getFirstInteger(PARAM_NAME_NEXT_SUB_PAGE_IDX, 0);

            Integer nextSubPageOffset = args.getFirstInteger("sectionTypeIdx", 0);
            nextSubPageIdx += nextSubPageOffset;

            log.info("Setting next sub page index to " + nextSubPageIdx + " (offset was "
                + nextSubPageOffset + ")");
            wizardPage.addProperty(PARAM_NAME_NEXT_SUB_PAGE_IDX, nextSubPageIdx);
          }

          result.addAction(wizardPage);
          return result;
        } else {
          // Configuration error (no form or action).
          return getErrorResult("Page does not declare a form or an action.");
        }


      } else {
        // Determine the job code --TODO: how to decouple this?
        Map<String, List<String>> containerMetadata = wizard.getContainerMetadata();
        if (!containerMetadata.containsKey(LMD_NAME_JOB_CODE)) {
          return getErrorResult("Job code not specified but required.");
        }
        String jobCode = containerMetadata.get(LMD_NAME_JOB_CODE).get(0);

        // Construct the ACL map
        AclMap aclMap = new AclMap(context.getSecurityService(), conf, jobCode);
        aclMap.createUndefinedRoles(superUser, context.getAuthorizationService().getRoleManager());

        // Create the container
        String parentId = context.getContentAssemblyService().getRootFolder(user).getId();
        ContentAssembly ca = createContainer(context, user, conf, wizard, parentId, aclMap);

        RestResult rr = getNotificationResult(context,
            "Created '" + ca.getDisplayName() + "' (ID: " + ca.getId() + ")", "Container Wizard");
        UserInterfaceAction action = new UserInterfaceAction("rsuite:refreshManagedObjects");
        action.addProperty("objects", parentId);
        action.addProperty("children", false);
        rr.addAction(action);
        return rr;
      }

      return restResult;

    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      return getErrorResult(WebServiceUtilsMessageProperties
          .get("web.service.error.unable.to.complete", e.getMessage()));
    } finally {
      log.info("Duration in millis: " + (new Date().getTime() - start.getTime()));
    }
  }

  protected Integer getCurrentSubPageIdx(CallArgumentList args) {
    return args.getFirstInteger(PARAM_NAME_NEXT_SUB_PAGE_IDX, 0) - 1;
  }

  protected void retainUserInput(ContainerWizard wizard, CallArgumentList args) {

    /*
     * TODO: how to inject business validation logic?
     */

    // Container name
    String containerName = args.getFirstString(PARAM_NAME_CONTAINER_NAME);
    if (StringUtils.isNotBlank(containerName)) {
      wizard.setContainerName(containerName);
    }

    // Container metadata
    List<CallArgument> lmdArgs = CallArgumentUtils.getArgumentsWithSameNamePrefix(args,
        PARAM_NAME_PREFIX_LMD, true, true, false);
    if (lmdArgs != null && lmdArgs.size() > 0) {
      for (CallArgument arg : lmdArgs) {
        if (!arg.isFile() && !arg.isFileItem()) {
          wizard.addContainerMetadata(arg.getName(), arg.getValue());
        }
      }
    }

    // Check for the configuration of future MOs.
    Integer subPageIdx = getCurrentSubPageIdx(args);
    if (subPageIdx >= 0) {
      String[] templateMoIds = args.getValuesArray(PARAM_NAME_XML_TEMPLATE_MO_ID);
      if (templateMoIds != null) {
        FutureManagedObject fmo;
        List<FutureManagedObject> futureMoList =
            wizard.getFutureManagedObjectListByKey(String.valueOf(subPageIdx));
        String[] titles = args.getValuesArray(PARAM_NAME_SECTION_TITLE);
        for (int i = 0; i < templateMoIds.length; i++) {
          String templateMoId = templateMoIds[i];
          fmo = new FutureManagedObject();
          fmo.setTemplateMoId(templateMoId);
          if (i < titles.length) {
            fmo.setTitle(titles[i]);
          }
          futureMoList.add(fmo);
        }
      }
    }

  }

  protected ContentAssembly createContainer(ExecutionContext context, User user,
      ContainerWizardConf conf, ContainerWizard wizard, String parentId, AclMap aclMap)
      throws RSuiteException, IOException, TransformerException {

    PrimaryContainer pcConf = conf.getPrimaryContainer();
    String defaultAclId = pcConf.getDefaultAclId();

    // Set up the primary container's options
    ContentAssemblyCreateOptions pcOptions = new ContentAssemblyCreateOptions();
    pcOptions.setACL(aclMap.get(pcConf.getAclId()));
    pcOptions.setMetaDataItems(wizard.getContainerMetadataAsList());
    pcOptions.setType(pcConf.getType());

    ContentAssemblyBuilder caBuilder = context.getContentAssemblyService()
        .constructContentAssemblyBuilder(user, parentId, wizard.getContainerName(), pcOptions);

    XPathEvaluator eval = XPathUtils.getXPathEvaluator(context, RSuiteNamespaces.MetaDataNS);

    // Iterate through everything we're to create in the new container
    int xmlMoConfIdx = -1;
    Map<String, ACL> postCommitAclUpdateMap = new HashMap<String, ACL>();
    for (Object o : pcConf.getContainerConfOrXmlMoConf()) {
      if (o instanceof ContainerConf) {
        addContainer(caBuilder, (ContainerConf) o, aclMap, defaultAclId, postCommitAclUpdateMap);
      } else if (o instanceof XmlMoConf) {
        // Process all future MOs associated with this XML MO conf.
        addManagedObjects(context, user, eval, caBuilder, (XmlMoConf) o,
            wizard.getFutureManagedObjectListByKey(String.valueOf(++xmlMoConfIdx)), aclMap,
            defaultAclId);
      } else {
        log.warn("Skipped unexpected object with class " + o.getClass().getSimpleName());
      }
    }

    // Ask RSuite to create the container.
    ContentAssemblyBuilderResults results = caBuilder.commit();
    List<RSuiteException> exList = results.getExceptionList();
    if (exList != null && exList.size() > 0) {
      StringBuilder sb = new StringBuilder("Encountered ").append(exList.size())
          .append(" exception(s) while attempting to create the container.");
      for (int i = 0; i < exList.size(); i++) {
        RSuiteException ex = exList.get(i);
        sb.append(" ").append(i).append(".) ").append(ex.getMessage());
      }
      throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR, sb.toString());
    } else {
      // RCS-4415: Update ACLs after successful commit.
      if (postCommitAclUpdateMap.size() > 0) {
        SecurityService securityService = context.getSecurityService();
        for (Map.Entry<String, ACL> entry : postCommitAclUpdateMap.entrySet()) {
          securityService.setACL(superUser, entry.getKey(), entry.getValue());
        }
      }

      return results.getContentAssembly();
    }
  }

  protected String addContainer(ContentAssemblyBuilder caBuilder, ContainerConf conf, AclMap aclMap,
      String defaultAclId, Map<String, ACL> postCommitAclUpdateMap) throws RSuiteException {
    ContentAssemblyCreateOptions options = new ContentAssemblyCreateOptions();
    // RCS-4415: ACL provided here is ignored; set after commit().
    ACL acl = aclMap.get(conf.getAclId() != null ? conf.getAclId() : defaultAclId);
    options.setACL(acl);
    options.setType(conf.getType());
    String id = caBuilder.createCANode(caBuilder.getId(), conf.getName(), options);
    postCommitAclUpdateMap.put(id, acl);
    return id;
  }

  protected List<ManagedObject> addManagedObjects(ExecutionContext context, User user,
      XPathEvaluator eval, ContentAssemblyBuilder caBuilder, XmlMoConf conf,
      List<FutureManagedObject> fmoList, AclMap aclMap, String defaultAclId)
      throws RSuiteException, IOException, TransformerException {

    List<ManagedObject> moList = new ArrayList<ManagedObject>();
    ManagedObjectService moService = context.getManagedObjectService();

    if (fmoList != null && fmoList.size() > 0) {
      ACL rsuiteAcl = aclMap.get(conf.getAclId() != null ? conf.getAclId() : defaultAclId);
      ManagedObjectAdvisor advisor = new LocalManagedObjectAdvisor(rsuiteAcl);

      String rsuiteIdAttName =
          new StringBuilder(RSuiteNamespaces.MetaDataNS.getPrefix()).append(":rsuiteId").toString();
      String nodesWithRSuiteIdAttXPath =
          new StringBuilder("//*[@").append(rsuiteIdAttName).append("]").toString();

      ManagedObject templateMo;
      Element elem;
      Node[] nodeArr;
      NamedNodeMap atts;
      String filename;
      ManagedObject mo;
      ObjectAttachOptions options = new ObjectAttachOptions();
      for (FutureManagedObject fmo : fmoList) {
        // Make sure user can get the template.
        templateMo = moService.getManagedObject(superUser, fmo.getTemplateMoId());

        elem = templateMo.getElement();

        // Remove all RSuite IDs.
        nodeArr = eval.executeXPathToNodeArray(nodesWithRSuiteIdAttXPath, elem);
        for (Node node : nodeArr) {
          atts = node.getAttributes();
          atts.removeNamedItem(rsuiteIdAttName);
        }

        // TODO: Add title

        // Load MO
        filename = context.getIDGenerator().allocateId().concat(".xml");
        mo = MOUtils.load(context, user, filename,
            MOUtils.getObjectSource(context, filename,
                DomUtils.serializeToString(context, elem, true, true, DEFAULT_CHARACTER_ENCODING),
                DEFAULT_CHARACTER_ENCODING),
            advisor);
        moList.add(mo);

        // Give to builder
        caBuilder.attach(caBuilder.getId(), mo.getId(), options);
      }
    }

    return moList;
  }

}
