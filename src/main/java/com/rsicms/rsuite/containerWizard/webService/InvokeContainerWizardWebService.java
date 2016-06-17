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
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.UserType;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
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
import com.rsicms.rsuite.containerWizard.PageNavigation;
import com.rsicms.rsuite.containerWizard.advisors.mo.LocalManagedObjectAdvisor;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerConf;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.MetadataConf;
import com.rsicms.rsuite.containerWizard.jaxb.NameValuePair;
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

  protected User systemUser;

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    Date start = new Date();
    try {
      // TODO: flip back after refactoring
      // if (log.isDebugEnabled()) {
      CallArgumentUtils.logArguments(args, log);
      // }

      // TODO: start using OperationResult

      Session session = context.getSession();
      User user = session.getUser();
      this.systemUser = context.getAuthorizationService().getSystemUser();

      // Load the wizard configuration.
      // By loading each time, technically it could change mid-instance.
      ContainerWizardConfUtils confUtils = new ContainerWizardConfUtils();
      String confAlias = args.getFirstString(PARAM_NAME_CONF_ALIAS);
      ContainerWizardConf conf =
          confUtils.getContainerWizardConf(user, context.getManagedObjectService(), confAlias);

      // Re-constitute or get a new container wizard.
      ContainerWizard wizard = getContainerWizard(args.getFirstString(PARAM_NAME_CONTAINER_WIZARD));

      // Get our page navigation object.
      PageNavigation pageNav = new PageNavigation(conf, confUtils, args, log);

      // Handle cancel link, need a better indication from form submission
      if (pageNav.wasPageDismissed()) {
        return getNotificationResult(context, "Create product is canceled.", "Create Product");
      }

      // Retain values provided by the user.
      retainUserInput(context.getSearchService(), user, wizard, args,
          pageNav.getCurrentSubPageIdx());
      log.info(wizard.getInfo()); // TODO: when to stop this?

      if (pageNav.isPageRequested()) {
        return pageNav.getRestResult(wizard, confAlias);
      } else {
        // Time to create the container!
        String parentId = context.getContentAssemblyService().getRootFolder(user).getId();
        ContentAssembly ca =
            createPrimaryContainer(context, context.getSession(), conf, wizard, parentId);
        return getContainerCreatedRestResult(context, ca, parentId);
      }

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

  /**
   * Construct or reconstruct (deserialize) an instance of ContainerWizard.
   * 
   * @param serialiazedWizard If not blank, this method will attempt to deserialize the string into
   *        an instance of ContainerWizard; else, a new instance will be provided.
   * @return An instance of ContainerWizard.
   * @throws ClassNotFoundException
   * @throws IOException
   */
  public ContainerWizard getContainerWizard(String serialiazedWizard)
      throws ClassNotFoundException, IOException {
    ContainerWizard wizard;
    if (StringUtils.isBlank(serialiazedWizard)) {
      wizard = new ContainerWizard();
    } else {
      wizard = ContainerWizard.deserialize(serialiazedWizard);
    }
    return wizard;
  }

  /**
   * Retain the latest round of input from the user. Some user input may be validated.
   * 
   * @param searchService
   * @param user
   * @param wizard
   * @param args
   * @param currentSubPageIdx
   * @throws RSuiteException
   */
  public void retainUserInput(SearchService searchService, User user, ContainerWizard wizard,
      CallArgumentList args, Integer currentSubPageIdx) throws RSuiteException {

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
    if (currentSubPageIdx >= 0) {
      String[] templateMoIds = args.getValuesArray(PARAM_NAME_XML_TEMPLATE_MO_ID);
      if (templateMoIds != null) {
        FutureManagedObject fmo;
        List<FutureManagedObject> futureMoList =
            wizard.getFutureManagedObjectListByKey(String.valueOf(currentSubPageIdx));
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
  public static void performDataReEntryTest(CallArgument arg, CallArgumentList args)
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
  public void throwIfInvalidJobCode(User user, SearchService searchService, String jobCode)
      throws RSuiteException {

    List<ManagedObject> containers = searchIfJobCodeIsAlreadyAssigned(user, searchService, jobCode);
    if (containers != null && !containers.isEmpty()) {
      throw new RSuiteException(RSuiteException.ERROR_ALREADY_EXISTS,
          "Job code '" + jobCode + "' is already assigned to a product.");
    }
  }

  public List<ManagedObject> searchIfJobCodeIsAlreadyAssigned(User user,
      SearchService searchService, String jobCode) throws RSuiteException {
    return SearchUtils.searchForContentAssemblies(user, searchService, "product", LMD_NAME_JOB_CODE,
        jobCode.trim(), null, 1);
  }

  /**
   * Convert the given metadata configuration into a list, including any metadata included in the
   * second param.
   * 
   * @param metadataElem
   * @param starterList Null is okay.
   * @return A combined metadata list from the provided metadata configuration and starter list.
   */
  public static List<MetaDataItem> getMetadataList(MetadataConf metadataConf,
      List<MetaDataItem> starterList) {
    List<MetaDataItem> combinedList = new ArrayList<MetaDataItem>();
    if (starterList != null) {
      combinedList.addAll(starterList);
    }
    if (metadataConf != null) {
      for (NameValuePair nvp : metadataConf.getNameValuePair()) {
        combinedList.add(new MetaDataItem(nvp.getName(), nvp.getValue()));
      }
    }
    return combinedList;
  }

  public ContentAssembly createPrimaryContainer(ExecutionContext context, Session session,
      ContainerWizardConf conf, ContainerWizard wizard, String parentId)
      throws RSuiteException, IOException, TransformerException {

    User user = session.getUser();
    PrimaryContainer pcConf = conf.getPrimaryContainer();
    SecurityService securityService = context.getSecurityService();
    String defaultAclId = pcConf.getDefaultAclId();
    XPathEvaluator eval = getXPathEvaluator(context);

    // Create primary container; hold back on ACL until ID is known.
    ContentAssemblyCreateOptions pcOptions = new ContentAssemblyCreateOptions();
    pcOptions.setMetaDataItems(
        getMetadataList(pcConf.getMetadataConf(), wizard.getContainerMetadataAsList()));
    pcOptions.setType(pcConf.getType());
    ContentAssembly primaryContainer = context.getContentAssemblyService()
        .createContentAssembly(user, parentId, wizard.getContainerName(), pcOptions);
    log.info("Created primary container with ID " + primaryContainer.getId());

    // Construct AclMap using new container's ID, and create new roles.
    AclMap aclMap = new AclMap(context.getSecurityService(), conf, primaryContainer.getId());
    aclMap.createUndefinedContainerRoles(systemUser,
        context.getAuthorizationService().getRoleManager());

    /*
     * Grant roles to current user, reconstruct user instance, and set the container's ACL. When the
     * time comes, this is where we'll use configuration to decide which are the allowed role name
     * suffixes.
     */
    user = grantRoles(context.getAuthorizationService(), user, aclMap,
        CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT);
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
        xmlMoConfIdx++;
        addManagedObjects(context, user, eval, primaryContainer, xmlMoConfIdx, (XmlMoConf) o,
            wizard.getFutureManagedObjectListByKey(String.valueOf(xmlMoConfIdx)), aclMap,
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
   * @param allowedRoleNameSuffixes Use to restrict which roles configured in the AclMap are
   *        included in the return. Values are matched on the suffix of the role name. If no
   *        suffixes are provided, all from the AclMap are included.
   * @return The user provided, but possibly an updated instance thereof (inclusive of new roles).
   * @throws RSuiteException
   */
  public User grantRoles(AuthorizationService authService, User user, AclMap aclMap,
      String... allowedRoleNameSuffixes) throws RSuiteException {

    if (authService.isAdministrator(user)) {
      return user;
    } else if (user.getUserType() == UserType.LOCAL) {
      LocalUserManager localUserManager = authService.getLocalUserManager();

      localUserManager.updateUser(user.getUserId(), user.getFullName(), user.getEmail(),
          StringUtils.join(aclMap.getRoleNames(user, allowedRoleNameSuffixes), ","));

      // Necessary for RSuite to honor recently granted roles.
      log.info("Reloading local user accounts...");
      localUserManager.reload();

      return localUserManager.getUser(user.getUserId());
    }

    throw new RSuiteException(RSuiteException.ERROR_FUNCTIONALITY_NOT_SUPPORTED,
        "This feature does not yet support non-local users.");
  }

  public String addContainer(ContentAssemblyService caService, User user,
      ContentAssembly primaryContainer, ContainerConf conf, AclMap aclMap, String defaultAclId)
      throws RSuiteException {
    ContentAssemblyCreateOptions options = new ContentAssemblyCreateOptions();
    options.setACL(aclMap.get(conf.getAclId() != null ? conf.getAclId() : defaultAclId));
    options.setType(conf.getType());
    options.setMetaDataItems(getMetadataList(conf.getMetadataConf(), null));
    return caService.createContentAssembly(user, primaryContainer.getId(), conf.getName(), options)
        .getId();
  }

  /**
   * Copy the templates as new MOs within the primary container.
   * 
   * @param context
   * @param user
   * @param eval
   * @param primaryContainer
   * @param xmlMoConfIdx The XML MO configuratoin index, which is incorporated into ID attribute
   *        values, for container-level uniqueness.
   * @param conf
   * @param fmoList
   * @param aclMap
   * @param defaultAclId
   * @return
   * @throws RSuiteException
   * @throws IOException
   * @throws TransformerException
   */
  public List<ManagedObject> addManagedObjects(ExecutionContext context, User user,
      XPathEvaluator eval, ContentAssembly primaryContainer, int xmlMoConfIdx, XmlMoConf conf,
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
      String idAttXPath = "//*/@id";

      ManagedObject templateMo;
      Element elem;
      Node[] nodeArr;
      NamedNodeMap atts;
      String filename;
      ManagedObject mo;
      ObjectAttachOptions options = new ObjectAttachOptions();
      int idAttCnt = 0;
      for (FutureManagedObject fmo : fmoList) {
        // Make sure user can get the template.
        templateMo = moService.getManagedObject(systemUser, fmo.getTemplateMoId());

        elem = templateMo.getElement();

        // Remove all RSuite IDs.
        nodeArr = eval.executeXPathToNodeArray(nodesWithRSuiteIdAttXPath, elem);
        for (Node node : nodeArr) {
          atts = node.getAttributes();
          atts.removeNamedItem(rsuiteIdAttName);
        }

        // Make all existing ID attributes unique within the container.
        nodeArr = eval.executeXPathToNodeArray(idAttXPath, elem);
        for (Node node : nodeArr) {
          node.setNodeValue(new StringBuilder("id-").append(Integer.toString(xmlMoConfIdx))
              .append("-").append(Integer.toString(++idAttCnt)).toString());
        }

        // TODO: figure out how to make this configurable
        if (StringUtils.isNotBlank(fmo.getTitle())) {
          Node titleNode = eval.executeXPathToNode("body/product_title", elem);
          if (titleNode == null)
            titleNode = eval.executeXPathToNode("title", elem);
          titleNode.setTextContent(fmo.getTitle());
        }

        // Load MO
        filename = context.getIDGenerator().allocateId().concat(".xml");
        mo = loadMo(elem, context, user, filename, advisor);
        moList.add(mo);

        // Create reference
        caService.attach(user, primaryContainer.getId(), mo.getId(), options);
      }
    }

    return moList;
  }

  /**
   * Get the web service result applicable when the container has been created.
   * 
   * @param context
   * @param ca
   * @param parentId
   * @return Web service result for when the container has been created.
   */
  public RestResult getContainerCreatedRestResult(ExecutionContext context, ContentAssembly ca,
      String parentId) {
    RestResult rr = getNotificationResult(context,
        "Created '" + ca.getDisplayName() + "' (ID: " + ca.getId() + ")", "Create Product");
    UserInterfaceAction action = new UserInterfaceAction("rsuite:refreshManagedObjects");
    action.addProperty("objects", parentId);
    action.addProperty("children", false);
    rr.addAction(action);
    return rr;

  }

  /**
   * A wrapper around a utility method to get Managed Object gedObject
   * 
   * @param elem
   * @param context
   * @param user
   * @param filename
   * @param moAdvisor
   * @return ManagedObject
   * @throws RSuiteException
   * @throws IOException
   * @throws TransformerException
   */
  public ManagedObject loadMo(Element elem, ExecutionContext context, User user, String filename,
      ManagedObjectAdvisor moAdvisor) throws RSuiteException, IOException, TransformerException {
    return MOUtils.load(context, user, filename, getObjectSource(elem, context, filename),
        moAdvisor);
  }

  /**
   * A wrapper around a utility method to get ObjectSource
   * 
   * @param elem
   * @param context
   * @param filename
   * @return ObjectSource
   * @throws IOException
   * @throws RSuiteException
   * @throws TransformerException
   */
  public ObjectSource getObjectSource(Element elem, ExecutionContext context, String filename)
      throws IOException, RSuiteException, TransformerException {
    return MOUtils.getObjectSource(context, filename,
        DomUtils.serializeToString(context, elem, true, true, DEFAULT_CHARACTER_ENCODING),
        DEFAULT_CHARACTER_ENCODING);
  }

  /**
   * A wrapper around a static utility method to get XPathEvaluator
   * 
   * @param context
   * @return XPathEvaluator
   * @throws RSuiteException
   */
  public XPathEvaluator getXPathEvaluator(ExecutionContext context) throws RSuiteException {
    return XPathUtils.getXPathEvaluator(context, RSuiteNamespaces.MetaDataNS);
  }

}
