package com.rsicms.rsuite.containerWizard.webService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.UserType;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectCheckInOptions;
import com.reallysi.rsuite.api.control.ObjectReferenceMoveOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;
import com.reallysi.rsuite.api.security.ACL;
import com.reallysi.rsuite.api.security.LocalUserManager;
import com.reallysi.rsuite.api.xml.RSuiteNamespaces;
import com.reallysi.rsuite.api.xml.XPathEvaluator;
import com.reallysi.rsuite.service.AuthorizationService;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.SearchService;
import com.reallysi.rsuite.service.SecurityService;
import com.rsicms.rsuite.containerWizard.AclMap;
import com.rsicms.rsuite.containerWizard.AddXmlMoContext;
import com.rsicms.rsuite.containerWizard.AddXmlMoResult;
import com.rsicms.rsuite.containerWizard.ContainerWizard;
import com.rsicms.rsuite.containerWizard.ContainerWizardConfUtils;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.ExecutionMode;
import com.rsicms.rsuite.containerWizard.FutureManagedObject;
import com.rsicms.rsuite.containerWizard.PageNavigation;
import com.rsicms.rsuite.containerWizard.advisors.mo.LocalManagedObjectAdvisor;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerConf;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.MetadataConf;
import com.rsicms.rsuite.containerWizard.jaxb.NameValuePair;
import com.rsicms.rsuite.containerWizard.jaxb.PrimaryContainer;
import com.rsicms.rsuite.containerWizard.jaxb.XmlMoConf;
import com.rsicms.rsuite.utils.container.visitor.ChildrenInfoContainerVisitor;
import com.rsicms.rsuite.utils.mo.MOUtils;
import com.rsicms.rsuite.utils.search.SearchUtils;
import com.rsicms.rsuite.utils.webService.BaseWebService;
import com.rsicms.rsuite.utils.webService.CallArgumentUtils;
import com.rsicms.rsuite.utils.xml.DomUtils;
import com.rsicms.rsuite.utils.xml.XPathUtils;

/**
 * The wizard's primary web service, responsible for interpreting the specified container wizard's
 * configuration, retaining user input, setting up for the next form and web service submission, and
 * finally processing all of the user's input.
 * <p>
 * Parameters:
 * <ul>
 * <li>{@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_EXECUTION_MODE}
 * : Specify which mode to execute in. See {@link ExecutionMode} for choices.</li>
 * <li>{@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_OPERATION_NAME}
 * : Optionally, set the UI name of the operation performed by this web service. This value can
 * appear in messages to the user. The value should align with the execution mode, but isn't
 * dictated by the wizard. Examples include "Add Section" and "Create Product". The default is
 * {@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#DEFAULT_OPERATION_NAME}.</li>
 * <li>{@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_CONF_ALIAS}:
 * Specify the RSuite alias of the container wizard configuration MO to use.</li>
 * <li>{@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_CONTAINER_NAME}
 * : The name of the container to create.</li>
 * <li>Begins with
 * {@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_PREFIX_LMD}: Part
 * of what this wizard container considers user input. Parameters starting with this prefix are
 * accepted as container layered metadata. Caller may configure these as hidden or visible form
 * controls.</li>
 * <li>Ends with
 * {@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_SUFFIX_VERIFY}:
 * When a LMD parameter also has this parameter the wizard will require the two parameter values
 * match. It's a way to require the user enter the same value twice.</li>
 * <li>
 * {@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_XML_TEMPLATE_MO_ID}:
 * A repeating parameter that identifies the XML template to copy as a new MO. It is paired with the
 * {@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_SECTION_TITLE}.
 * </li>
 * <li>{@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_SECTION_TITLE}:
 * A repeating parameter that specifies the title of a new MO. It is paired with the
 * {@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_XML_TEMPLATE_MO_ID}
 * parameters.</li>
 * <li>
 * {@value com.rsicms.rsuite.containerWizard.ContainerWizardConstants#PARAM_NAME_CONTAINER_WIZARD}: A
 * serialized instance of {@link ContainerWizard}. This web service sets and maintains this value.
 * The caller is only responsible for not preventing it from being passed in.</li>
 * </ul>
 */
public class InvokeContainerWizardWebService extends BaseWebService
    implements ContainerWizardConstants {

  private static Log log = LogFactory.getLog(InvokeContainerWizardWebService.class);

  protected String opName;
  protected User systemUser;
  protected XPathEvaluator eval;

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    Date start = new Date();
    try {
      // TODO: when to flip back?
      // if (log.isDebugEnabled()) {
      CallArgumentUtils.logArguments(args, log);
      // }

      // TODO: start using OperationResult

      Session session = context.getSession();
      User user = session.getUser();
      this.systemUser = context.getAuthorizationService().getSystemUser();
      this.eval = new XPathUtils().getXPathEvaluator(context.getXmlApiManager(),
          RSuiteNamespaces.MetaDataNS);

      // Determine the execution mode (e.g., create container or add XML MO) and the
      // insertion position, which is only applicable to one mode.
      ExecutionMode mode = ExecutionMode.get(args.getFirstString(PARAM_NAME_EXECUTION_MODE));

      // Load the wizard configuration.
      // By loading each time, technically it could change mid-instance.
      ContainerWizardConfUtils confUtils = new ContainerWizardConfUtils();
      String confAlias = args.getFirstString(PARAM_NAME_CONF_ALIAS);
      ContainerWizardConf conf =
          confUtils.getContainerWizardConf(user, context.getManagedObjectService(), confAlias);

      // Re-constitute or get a new container wizard.
      ContainerWizard wizard = getContainerWizard(args.getFirstString(PARAM_NAME_CONTAINER_WIZARD),
          args.getFirstString(PARAM_NAME_OPERATION_NAME, DEFAULT_OPERATION_NAME));
      this.opName = wizard.getOperationName();

      if (ExecutionMode.ADD_XML_MO == mode && !wizard.isInAddXmlMoMode()) {
        wizard.setAddXmlMoContext(new AddXmlMoContext(context, user, conf, confUtils, args));
      }

      // Get our page navigation object.
      PageNavigation pageNav = new PageNavigation(wizard, conf, confUtils, args, log);

      // Handle cancel link, need a better indication from form submission
      if (pageNav.wasPageDismissed()) {
        return getNotificationResult(context, "Operation was canceled.", opName);
      }

      // Retain values provided by the user.
      retainUserInput(context.getSearchService(), user, wizard, args,
          pageNav.getCurrentSubPageIdx());
      log.info(wizard.getInfo()); // TODO: when to stop this?

      if (pageNav.isPageRequested()) {
        return pageNav.getRestResult(confAlias);
      } else if (wizard.isInAddXmlMoMode()) {
        AddXmlMoResult addXmlMoResult = addXmlMos(context, user, conf, wizard);
        AddXmlMoContext addContext = wizard.getAddXmlMoContext();
        String refreshId = addContext.shouldCreateAsTopLevelMos() ? addContext.getContainerId()
            : addContext.getParentMoId();
        return getMosAddedRestResult(context, addXmlMoResult, refreshId);
      } else {
        // Time to create the container!
        // FIXME: Presumes home container desired.
        String parentId = context.getContentAssemblyService().getRootFolder(user).getId();
        ContentAssemblyNodeContainer ca =
            createPrimaryContainer(context, context.getSession(), conf, wizard, parentId);
        return getContainerCreatedRestResult(context, ca, parentId);
      }

    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      // TODO: get WebServiceUtilsMessageProperties working
      // return getErrorResult(WebServiceUtilsMessageProperties
      // .get("web.service.error.unable.to.complete", e.getMessage()));
      return getErrorResult(e.getMessage(), opName);
    } finally {
      log.info("Duration in millis: " + (new Date().getTime() - start.getTime()));
    }
  }

  /**
   * Get the XPathEvaluator used by this web service. This is only public to facilitate unit
   * testing.
   * 
   * @return The XPathEvaluator used by this web service.
   */
  public XPathEvaluator getXPathEvaluator() {
    return eval;
  }

  /**
   * Construct or reconstruct (deserialize) an instance of ContainerWizard.
   * 
   * @param serialiazedWizard If not blank, this method will attempt to deserialize the string into
   *        an instance of ContainerWizard; else, a new instance will be provided.
   * @param opName The operation's display name, which is only provided to new container wizard
   *        instances.
   * @return An instance of ContainerWizard.
   * @throws ClassNotFoundException
   * @throws IOException
   */
  public ContainerWizard getContainerWizard(String serialiazedWizard, String opName)
      throws ClassNotFoundException, IOException {
    ContainerWizard wizard;
    if (StringUtils.isBlank(serialiazedWizard)) {
      wizard = new ContainerWizard();
      wizard.setOperationName(opName);
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
      String val2 = args.getFirstString(
          new StringBuilder(arg.getName()).append(PARAM_NAME_SUFFIX_VERIFY).toString());
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
   * @param metadataConf
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

  /**
   * Intended for the wizard's "add XML MO" mode, this is when the MOs are created and attached to
   * the container.
   * 
   * @param context
   * @param user
   * @param conf
   * @param wizard
   * @return An instance of AddXmlMoResult. When top-level MOs are created, it contains a list of
   *         those MOs. Else, the result only tells you the number of sub-MOs added.
   * @throws RSuiteException
   * @throws TransformerException
   * @throws IOException
   */
  public AddXmlMoResult addXmlMos(ExecutionContext context, User user, ContainerWizardConf conf,
      ContainerWizard wizard) throws RSuiteException, IOException, TransformerException {

    AddXmlMoContext addContext = wizard.getAddXmlMoContext();

    // Presume the new MO's ACL should match that of the existing MO.
    ACL acl = context.getManagedObjectService().getManagedObject(user, addContext.getExistingMoId())
        .getACL();

    // A touch of configuration only required when creating sub-MOs...
    String adjacentSubMoId = null;
    boolean insertBefore = false;
    if (addContext.shouldCreateAsSubMos()) {
      if (StringUtils.isNotBlank(addContext.getInsertBeforeId())) {
        adjacentSubMoId = addContext.getInsertBeforeId();
        insertBefore = true;
      } else {
        adjacentSubMoId = addContext.getInsertAfterId();
        insertBefore = false;
      }
    }

    // Create the MOs. When creating top-level MOs, the references will be created in the wrong
    // spot; this is corrected below.
    String parentId = addContext.shouldCreateAsTopLevelMos() ? addContext.getContainerId()
        : addContext.getParentMoId();
    AddXmlMoResult addXmlMoResult = addManagedObjects(context, user, getXPathEvaluator(),
        addContext.shouldCreateAsTopLevelMos(), parentId,
        wizard.getFirstAndOnlyFutureManagedObjectList(), acl, adjacentSubMoId, insertBefore);

    /*
     * When top-level MOs are created, move the references.
     * 
     * Because of RCS-4634 (present in RSuite 4.1.15), allow addManagedObjects() to create
     * references to the new MOs at the bottom of the container, the use moveReference to get them
     * to the correct location. Once RCS-4634 is fixed, this could be made more efficient.
     */
    if (addContext.shouldCreateAsTopLevelMos()) {
      ContentAssemblyService caService = context.getContentAssemblyService();
      ChildrenInfoContainerVisitor visitor = new ChildrenInfoContainerVisitor(context, user);
      visitor.visitContentAssemblyNodeContainer(
          caService.getContentAssemblyNodeContainer(user, parentId));
      ObjectReferenceMoveOptions options = new ObjectReferenceMoveOptions();
      options.setBeforeId(visitor.getMoRef(addContext.getInsertBeforeId()).getId());
      for (ManagedObject mo : addXmlMoResult.getManagedObjects()) {
        caService.moveReference(user, visitor.getMoRef(mo.getId()).getId(), parentId, options);
      }
    } else if (addContext.shouldCheckInParentMo()) {
      ObjectCheckInOptions options = new ObjectCheckInOptions();
      options.setVersionType(VersionType.MINOR);
      options.setVersionNote("Completed '" + opName + "' request.");
      context.getManagedObjectService().checkIn(user, addContext.getParentMoId(), options);
    }

    return addXmlMoResult;
  }

  /**
   * Construct a REST result after finishing this use of the container wizard in the "add XML MO"
   * mode.
   * 
   * @param context
   * @param addXmlMoResult
   * @param containerId
   * @return REST result for the container wizard after completing execution of the "add XML MO"
   *         mode.
   * @throws RSuiteException
   */
  public RestResult getMosAddedRestResult(ExecutionContext context, AddXmlMoResult addXmlMoResult,
      String containerId) throws RSuiteException {
    StringBuilder sb =
        new StringBuilder("Created ").append(addXmlMoResult.getCount()).append(" piece");
    if (addXmlMoResult.getCount() != 1) {
      sb.append("s");
    }
    sb.append(" of content.");
    RestResult rr = getNotificationResult(context, sb.toString(), opName);
    UserInterfaceAction action = new UserInterfaceAction("rsuite:refreshManagedObjects");
    action.addProperty("objects", containerId);
    action.addProperty("children", true);
    rr.addAction(action);
    return rr;
  }

  public ContentAssemblyNodeContainer createPrimaryContainer(ExecutionContext context,
      Session session, ContainerWizardConf conf, ContainerWizard wizard, String parentId)
      throws RSuiteException, IOException, TransformerException {

    User user = session.getUser();
    PrimaryContainer pcConf = conf.getPrimaryContainer();
    SecurityService securityService = context.getSecurityService();
    String defaultAclId = pcConf.getDefaultAclId();

    // Create primary container; hold back on ACL until ID is known.
    ContentAssemblyCreateOptions pcOptions = getCaCreateOptions(pcConf, wizard);
    ContentAssemblyNodeContainer primaryContainer = context.getContentAssemblyService()
        .createContentAssembly(user, parentId, wizard.getContainerName(), pcOptions);
    log.info("Created primary container with ID " + primaryContainer.getId());

    // Construct AclMap using new container's ID, and create new roles.
    AclMap aclMap = new AclMap(context.getSecurityService(), conf,
        AclMap.getContainerRoleNamePrefix(primaryContainer));
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

        XmlMoConf xmlMoConf = (XmlMoConf) o;

        ACL acl = aclMap.get(xmlMoConf.getAclId() != null ? xmlMoConf.getAclId() : defaultAclId);

        addManagedObjects(context, user, getXPathEvaluator(), true, primaryContainer.getId(),
            wizard.getFutureManagedObjectListByKey(String.valueOf(xmlMoConfIdx)), acl, null, false);
      } else {
        log.warn("Skipped unexpected object with class " + o.getClass().getSimpleName());
      }
    }

    return primaryContainer;
  }

  /**
   * Grant the roles in the given AclMap to the specified user. The wizard does not provide a way to
   * grant the roles to additional users.
   * 
   * @param authService
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
      ContentAssemblyNodeContainer primaryContainer, ContainerConf conf, AclMap aclMap,
      String defaultAclId) throws RSuiteException {
    ContentAssemblyCreateOptions options = new ContentAssemblyCreateOptions();
    options.setACL(aclMap.get(conf.getAclId() != null ? conf.getAclId() : defaultAclId));
    options.setType(conf.getType());
    options.setMetaDataItems(getMetadataList(conf.getMetadataConf(), null));
    return caService
        .createContentAssembly(user, primaryContainer.getId(), conf.getDisplayName(), options)
        .getId();
  }

  /**
   * Copy the templates as new MOs within the primary container.
   * 
   * @param context
   * @param user
   * @param eval
   * @param createTopLevelMos Submit true to create top-level MOs, and attach to the container
   *        identified by the parentId parameter. Submit false to create sub-MOs within the MO
   *        identified by parentId.
   * @param parentId The ID of either a container or MO. Use the createTopLevelMos parameter to make
   *        this distinction.
   * @param fmoList
   * @param acl Only used when creating top-level MOs. When creating sub-MOs, RSuite decides the ACL
   *        (likely that of the parent MO).
   * @param adjacentSubMoId Only used when creating sub-MOs. The value needs to be the ID of an
   *        existing sub-MO within the parent/ancestor that the new MOs are to be added before or
   *        after.
   * @param insertBefore Only used when creating sub-MOs. Submit true to add new sub-MOs before the
   *        node identified by adjacentNodeXPath; else, submit false to inserted after it.
   * @return An instance of AddXmlMoResult. When top-level MOs are created, it contains a list of
   *         those MOs. Else, the result only tells you the number of sub-MOs added.
   * @throws RSuiteException
   * @throws IOException
   * @throws TransformerException
   */
  public AddXmlMoResult addManagedObjects(ExecutionContext context, User user, XPathEvaluator eval,
      boolean createTopLevelMos, String parentId, List<FutureManagedObject> fmoList, ACL acl,
      String adjacentSubMoId, boolean insertBefore)
      throws RSuiteException, IOException, TransformerException {

    AddXmlMoResult result = new AddXmlMoResult();
    ManagedObjectService moService = context.getManagedObjectService();
    ContentAssemblyService caService = context.getContentAssemblyService();
    List<Node> newSubMoNodeList = new ArrayList<Node>();

    if (fmoList != null && fmoList.size() > 0) {
      ManagedObjectAdvisor advisor = new LocalManagedObjectAdvisor(acl);

      String rsuiteIdAttName =
          new StringBuilder(RSuiteNamespaces.MetaDataNS.getPrefix()).append(":rsuiteId").toString();
      String nodesWithRSuiteIdAttXPath =
          new StringBuilder("//*[@").append(rsuiteIdAttName).append("]").toString();
      String idAttName = "id";

      ManagedObject templateMo;
      Element templateElem;
      Node[] nodeArr;
      NamedNodeMap atts;
      Node att;
      String id;
      String filename;
      ManagedObject mo;
      ObjectAttachOptions attachOptions = new ObjectAttachOptions();
      long timestamp = System.currentTimeMillis();
      int idAttCnt = 0;
      for (FutureManagedObject fmo : fmoList) {
        // Make sure user can get the template.
        templateMo = moService.getManagedObject(systemUser, fmo.getTemplateMoId());

        templateElem = templateMo.getElement();

        // Remove all RSuite IDs.
        nodeArr = eval.executeXPathToNodeArray(nodesWithRSuiteIdAttXPath, templateElem);
        for (Node node : nodeArr) {
          atts = node.getAttributes();
          atts.removeNamedItem(rsuiteIdAttName);
        }

        // Set a container-unique ID on every element. May need to make this configurable.
        nodeArr = eval.executeXPathToNodeArray("//*", templateElem);
        for (Node node : nodeArr) {
          id = new StringBuilder("id-").append(timestamp).append("-")
              .append(Integer.toString(++idAttCnt)).toString();
          atts = node.getAttributes();
          att = atts.getNamedItem(idAttName);
          if (att == null) {
            ((Element) node).setAttribute(idAttName, id);
          } else {
            att.setNodeValue(id);
          }
        }

        // TODO: figure out how to make this configurable
        if (StringUtils.isNotBlank(fmo.getTitle())) {
          Node titleNode = eval.executeXPathToNode("body/product_title", templateElem);
          if (titleNode == null)
            titleNode = eval.executeXPathToNode("title", templateElem);
          titleNode.setTextContent(fmo.getTitle());
        }

        // Load as new top-level MO or hold for a bit to add as a sub-MO.
        if (createTopLevelMos) {
          filename = context.getIDGenerator().allocateId().concat(".xml");
          mo = loadMo(templateElem, context, user, filename, advisor);
          result.add(mo);

          // Create reference
          if (StringUtils.isNotBlank(parentId)) {
            caService.attach(user, parentId, mo.getId(), attachOptions);
          }
        } else {
          newSubMoNodeList.add(templateElem);
          result.increment();
        }
      }

      // When we didn't create top-level MOs, it's time to add the new MOs as sub-MOs.
      if (!createTopLevelMos && newSubMoNodeList.size() > 0) {
        String xpath = new StringBuilder("//*[@").append(rsuiteIdAttName).append("='")
            .append(adjacentSubMoId).append("']").toString();
        MOUtils.addNodesIntoExistingMo(moService, user, parentId, xpath, insertBefore, eval,
            newSubMoNodeList, true, context.getXmlApiManager().getTransformer((File) null));
      }
    }

    return result;
  }

  /**
   * Get the web service result applicable when the container has been created.
   * 
   * @param context
   * @param ca
   * @param parentId
   * @return Web service result for when the container has been created.
   */
  public RestResult getContainerCreatedRestResult(ExecutionContext context,
      ContentAssemblyNodeContainer ca, String parentId) {
    RestResult rr = getNotificationResult(context,
        "Created '" + ca.getDisplayName() + "' (ID: " + ca.getId() + ")", opName);
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
    Transformer transformer = context.getXmlApiManager().getTransformer((File) null);
    return MOUtils.getObjectSource(context, filename,
        DomUtils.serializeToString(transformer, elem, true, true, DEFAULT_CHARACTER_ENCODING),
        DEFAULT_CHARACTER_ENCODING);
  }

  public ContentAssemblyCreateOptions getCaCreateOptions(PrimaryContainer pcConf,
      ContainerWizard wizard) {

    ContentAssemblyCreateOptions pcOptions = new ContentAssemblyCreateOptions();
    pcOptions.setMetaDataItems(
        getMetadataList(pcConf.getMetadataConf(), wizard.getContainerMetadataAsList()));
    pcOptions.setType(pcConf.getType());
    return pcOptions;

  }

}
