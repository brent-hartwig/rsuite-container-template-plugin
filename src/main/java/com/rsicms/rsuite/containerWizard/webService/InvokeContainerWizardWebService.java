package com.rsicms.rsuite.containerWizard.webService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.reallysi.rsuite.api.UserType;
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
import com.reallysi.rsuite.api.security.LocalUserManager;
import com.reallysi.rsuite.api.xml.RSuiteNamespaces;
import com.reallysi.rsuite.api.xml.XPathEvaluator;
import com.reallysi.rsuite.service.AuthorizationService;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.SearchService;
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
import com.rsicms.rsuite.utils.search.SearchUtils;
import com.rsicms.rsuite.utils.webService.BaseWebService;
import com.rsicms.rsuite.utils.webService.CallArgumentUtils;
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

      // Handle cancel link, need a better indication from form submission
      if (args.getFirstInteger(PARAM_NAME_NEXT_SUB_PAGE_IDX, 0) == null) {
        return getNotificationResult(context, "Create product is canceled.", "Create Product");
      }

      // Retain values provided by the user.
      retainUserInput(context.getSearchService(), user, wizard, args);
      log.info(wizard.getInfo());

      // Is there another page to display?
      RestResult restResult;
      Integer pageIdx = args.getFirstInteger(PARAM_NAME_NEXT_PAGE_IDX, -1);
      boolean reachedLastSubPage = args.getFirstBoolean(PARAM_NAME_REACHED_LAST_SUB_PAGE, false);

      // Find the end of the wizard form
      Integer configuredSubPageSize = 0;
      for (Object o : conf.getPrimaryContainer().getContainerConfOrXmlMoConf()) {
        if (o instanceof XmlMoConf)
          configuredSubPageSize++;
      }
      reachedLastSubPage = (args.getFirstInteger(PARAM_NAME_NEXT_SUB_PAGE_IDX, 0)
          + args.getFirstInteger(PARAM_NAME_SECTION_TYPE_IDX, 0)) >= configuredSubPageSize;

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
        // Create the container
        String parentId = context.getContentAssemblyService().getRootFolder(user).getId();
        ContentAssembly ca =
            createPrimaryContainer(context, context.getSession(), conf, wizard, parentId);

        RestResult rr = getNotificationResult(context,
            "Created '" + ca.getDisplayName() + "' (ID: " + ca.getId() + ")", "Create Product");
        UserInterfaceAction action = new UserInterfaceAction("rsuite:refreshManagedObjects");
        action.addProperty("objects", parentId);
        action.addProperty("children", false);
        rr.addAction(action);
        return rr;
      }

      return restResult;

    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      // TODO: get WebServiceUtilsMessageProperties working
      // return getErrorResult(WebServiceUtilsMessageProperties
      // .get("web.service.error.unable.to.complete", e.getMessage()));
      return getErrorResult(e.getMessage());
    } finally {
      log.info("Duration in millis: " + (new Date().getTime() - start.getTime()));
    }
  }

  protected Integer getCurrentSubPageIdx(CallArgumentList args) {
    return args.getFirstInteger(PARAM_NAME_NEXT_SUB_PAGE_IDX, 0) - 1;
  }

  protected void retainUserInput(SearchService searchService, User user, ContainerWizard wizard,
      CallArgumentList args) throws RSuiteException {

    /*
     * TODO: implement a better way to inject business validation logic.
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

          performDataReEntryTest(arg, args);

          if ("JobCode".equalsIgnoreCase(arg.getName())) {
            throwIfInvalidJobCode(user, searchService, arg.getValue());
          }

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

  /**
   * If the singled-out argument has a re-entry value, require the values match.
   * 
   * @param arg
   * @param args
   * @throws RSuiteException Thrown when a verify value has been provided but the values differ.
   */
  protected void performDataReEntryTest(CallArgument arg, CallArgumentList args)
      throws RSuiteException {
    if (arg != null && args != null) {
      String val2 =
          args.getFirstString(new StringBuilder(arg.getName()).append("-Verify").toString());
      if (StringUtils.isNotBlank(val2)) {
        String val1 = arg.getValue().trim();
        val2 = val2.trim();
        if (!val2.equalsIgnoreCase(val1)) {
          /*
           * TODO: would be more user friendly to not make the user start over. Could be a
           * client-side test; or, could re-display form with values provided by user plus an error
           * message.
           */
          throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
              new StringBuilder("\"").append(arg.getName()).append("\" values do not match: ")
                  .append(val1).append(", ").append(val2).toString());
        }
      }
    }
  }

  /**
   * Validate the job code.
   * 
   * @param user
   * @param searchService
   * @param jobCode
   * @throws RSuiteException Thrown if job code is invalid.
   */
  protected void throwIfInvalidJobCode(User user, SearchService searchService, String jobCode)
      throws RSuiteException {

    /*
     * Utility form validation performs these tasks if (StringUtils.isEmpty(jobCode)) { throw new
     * RSuiteException(RSuiteException.ERROR_PARAM_INVALID, "Job code is missing."); }
     * 
     * jobCode = jobCode.trim();
     * 
     * // TODO: move to JS if (!jobCode.matches("\\d+")) { throw new
     * RSuiteException(RSuiteException.ERROR_PARAM_INVALID, "Job code '" + jobCode +
     * "' is not all digits."); }
     * 
     * // TODO: move to JS if (jobCode.trim().matches("^6\\d{5}") ||
     * jobCode.trim().matches("700000")) { throw new
     * RSuiteException(RSuiteException.ERROR_PARAM_INVALID, "Job code '" + jobCode +
     * "' is in range of 600000 to 700000."); }
     */

    List<ManagedObject> containers = SearchUtils.searchForContentAssemblies(user, searchService,
        "product", LMD_NAME_JOB_CODE, jobCode.trim(), null, 1);
    if (containers != null && !containers.isEmpty()) {
      throw new RSuiteException(RSuiteException.ERROR_ALREADY_EXISTS,
          "Job code '" + jobCode + "' is already assigned to a product.");
    }
  }

  protected ContentAssembly createPrimaryContainer(ExecutionContext context, Session session,
      ContainerWizardConf conf, ContainerWizard wizard, String parentId)
      throws RSuiteException, IOException, TransformerException {

    User user = session.getUser();
    PrimaryContainer pcConf = conf.getPrimaryContainer();
    SecurityService securityService = context.getSecurityService();
    String defaultAclId = pcConf.getDefaultAclId();
    XPathEvaluator eval = XPathUtils.getXPathEvaluator(context, RSuiteNamespaces.MetaDataNS);

    // Create primary container; hold back on ACL until ID is known.
    ContentAssemblyCreateOptions pcOptions = new ContentAssemblyCreateOptions();
    pcOptions.setMetaDataItems(wizard.getContainerMetadataAsList());
    pcOptions.setType(pcConf.getType());
    ContentAssembly primaryContainer = context.getContentAssemblyService()
        .createContentAssembly(user, parentId, wizard.getContainerName(), pcOptions);
    log.info("Created primary container with ID " + primaryContainer.getId());

    // Construct AclMap using new container's ID, and create new roles.
    AclMap aclMap = new AclMap(context.getSecurityService(), conf, primaryContainer.getId());
    aclMap.createUndefinedRoles(superUser, context.getAuthorizationService().getRoleManager());

    // Grant roles to current user, reconstruct user instance, and set the container's ACL.
    user = grantRoles(context.getAuthorizationService(), user, aclMap);
    session.setUser(user); // enables CMS UI user to access the new container.
    securityService.setACL(user, primaryContainer.getId(), aclMap.get(pcConf.getAclId()));

    // Iterate through the configuration and user-input to populate new container.
    int xmlMoConfIdx = -1;
    for (Object o : pcConf.getContainerConfOrXmlMoConf()) {
      if (o instanceof ContainerConf) {
        addContainer(context.getContentAssemblyService(), user, primaryContainer, (ContainerConf) o,
            aclMap, defaultAclId);
      } else if (o instanceof XmlMoConf) {
        // Process all future MOs associated with this XML MO conf.
        addManagedObjects(context, user, eval, primaryContainer, (XmlMoConf) o,
            wizard.getFutureManagedObjectListByKey(String.valueOf(++xmlMoConfIdx)), aclMap,
            defaultAclId);
      } else {
        log.warn("Skipped unexpected object with class " + o.getClass().getSimpleName());
      }
    }

    return primaryContainer;
  }

  /**
   * Grant the roles in the given AclMap to the specified user.
   * <p>
   * TODO: This will need to change after sprint 17 as the user creating this container ought to be
   * able to specify which users get which roles (associated with this container).
   * 
   * @param roleManager
   * @param user
   * @param aclMap
   * @return The user provided, but possibly an updated instance thereof (inclusive of new roles).
   * @throws RSuiteException
   */
  protected User grantRoles(AuthorizationService authService, User user, AclMap aclMap)
      throws RSuiteException {

    if (authService.isAdministrator(user)) {
      return user;
    } else if (user.getUserType() == UserType.LOCAL) {
      LocalUserManager localUserManager = authService.getLocalUserManager();

      localUserManager.updateUser(user.getUserId(), user.getFullName(), user.getEmail(),
          StringUtils.join(aclMap.getRoleNames(user), ","));

      // Necessary for RSuite to honor recently granted roles.
      log.info("Reloading local user accounts...");
      localUserManager.reload();

      return localUserManager.getUser(user.getUserId());
    }

    throw new RSuiteException(RSuiteException.ERROR_FUNCTIONALITY_NOT_SUPPORTED,
        "This feature does not yet support non-local users.");
  }

  protected String addContainer(ContentAssemblyService caService, User user,
      ContentAssembly primaryContainer, ContainerConf conf, AclMap aclMap, String defaultAclId)
      throws RSuiteException {
    ContentAssemblyCreateOptions options = new ContentAssemblyCreateOptions();
    options.setACL(aclMap.get(conf.getAclId() != null ? conf.getAclId() : defaultAclId));
    options.setType(conf.getType());
    return caService.createCANode(user, primaryContainer.getId(), conf.getName(), options).getId();
  }

  protected List<ManagedObject> addManagedObjects(ExecutionContext context, User user,
      XPathEvaluator eval, ContentAssembly primaryContainer, XmlMoConf conf,
      List<FutureManagedObject> fmoList, AclMap aclMap, String defaultAclId)
      throws RSuiteException, IOException, TransformerException {

    List<ManagedObject> moList = new ArrayList<ManagedObject>();
    ManagedObjectService moService = context.getManagedObjectService();
    ContentAssemblyService caService = context.getContentAssemblyService();

    if (fmoList != null && fmoList.size() > 0) {
      ManagedObjectAdvisor advisor = new LocalManagedObjectAdvisor(
          aclMap.get(conf.getAclId() != null ? conf.getAclId() : defaultAclId));

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

        // TODO: figure out how to make this configurable
        if (StringUtils.isNotBlank(fmo.getTitle())) {
          Node titleNode = eval.executeXPathToNode("title", elem);
          titleNode.setTextContent(fmo.getTitle());
          Node productTitleNode = eval.executeXPathToNode("body/product_title", elem);
          if (productTitleNode != null)
            productTitleNode.setTextContent(fmo.getTitle());
        }

        // Load MO
        filename = context.getIDGenerator().allocateId().concat(".xml");
        mo = MOUtils.load(context, user, filename,
            MOUtils.getObjectSource(context, filename,
                DomUtils.serializeToString(context, elem, true, true, DEFAULT_CHARACTER_ENCODING),
                DEFAULT_CHARACTER_ENCODING),
            advisor);
        moList.add(mo);

        // Create reference
        caService.attach(user, primaryContainer.getId(), mo.getId(), options);
      }
    }

    return moList;
  }

}
