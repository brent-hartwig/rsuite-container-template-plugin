package com.rsicms.rsuite.containerWizard;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.service.AuthorizationService;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.utils.container.ContainerUtils;
import com.rsicms.rsuite.utils.container.visitor.ChildrenInfoContainerVisitor;

/**
 * Provides context to the "add XML MO" execution mode of the container wizard.
 */
public class AddXmlMoContext implements Serializable, ContainerWizardConstants {

  private static final long serialVersionUID = 1L;

  private String containerId;
  private String existingMoId;
  private String existingMoRefId;
  private String insertBeforeId;
  private int xmlMoConfIdx;

  public AddXmlMoContext(ExecutionContext context, User user, ContainerWizardConf conf,
      ContainerWizardConfUtils confUtils, CallArgumentList args) throws RSuiteException {

    boolean insertBefore = InsertionPosition
        .get(args.getFirstString(PARAM_NAME_INSERTION_POSITION)) == InsertionPosition.BEFORE;

    this.existingMoId = args.getFirstString(PARAM_NAME_RSUITE_ID);
    if (StringUtils.isBlank(this.existingMoId)) {
      throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
          "The ID of the object to insert before or after was not provided.");
    }
    ManagedObject existingMo =
        context.getManagedObjectService().getManagedObject(user, this.existingMoId);
    String existingMoLocalName = existingMo.getLocalName();

    // Determine the container by walking up the request's path objects until reaching a container
    // of the correct type.
    ContentAssemblyNodeContainer container =
        ContainerUtils.getContentAssemblyNodeContainer(context.getContentAssemblyService(), user,
            args.getFirstContentObjectPath(user), conf.getPrimaryContainer().getType());
    if (container == null) {
      throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
          "Unable to determine the container.");
    }
    this.containerId = container.getId();

    // With the container, we can perform the optional security check.
    throwIfNotAuthorized(context.getAuthorizationService(), user, container, args);

    // Visit container in order to answer some questions we have.
    ChildrenInfoContainerVisitor visitor = new ChildrenInfoContainerVisitor(context, user);
    visitor.visitContentAssemblyNodeContainer(container);

    // Let's see what we found.
    this.existingMoRefId = visitor.getMoRef(this.existingMoId).getId();
    ContentAssemblyItem siblingCaItem = insertBefore ? visitor.getAnySiblingBefore(existingMoId)
        : visitor.getAnySiblingAfter(existingMoId);
    ManagedObjectReference siblingMoRef = insertBefore ? visitor.getMoRefSiblingBefore(existingMoId)
        : visitor.getMoRefSiblingAfter(existingMoId);
    ManagedObject siblingMo = null;
    if (siblingMoRef != null) {
      siblingMo =
          context.getManagedObjectService().getManagedObject(user, siblingMoRef.getTargetId());
    }

    // Determine the ID of the object to insert before. When null, the system will be expected to
    // add at the bottom. As there could be CA refs or CANodes between two MO refs, using the
    // sibling CA item here, which covers all three.
    if (insertBefore) {
      this.insertBeforeId = this.existingMoRefId;
    } else if (siblingCaItem != null) {
      this.insertBeforeId = siblingCaItem.getId();
    } else {
      this.insertBeforeId = null;
    }

    String localNameBefore = null;
    if (insertBefore && siblingMo != null) {
      localNameBefore = siblingMo.getLocalName();
    } else if (!insertBefore) {
      localNameBefore = existingMoLocalName;
    }

    String localNameAfter = null;
    if (!insertBefore && siblingMo != null) {
      localNameAfter = siblingMo.getLocalName();
    } else if (insertBefore) {
      localNameAfter = existingMoLocalName;
    }

    this.xmlMoConfIdx =
        confUtils.getFirstAllowedXmlMoConfIndex(conf, localNameBefore, localNameAfter);

    if (this.xmlMoConfIdx < 0) {
      throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
          "No additional content is allowed " + (insertBefore ? "before" : "after")
              + " the specified content.");
    }
  }

  /**
   * Determine if this user is authorized to add content to this container.
   * 
   * @param authService
   * @param user
   * @param container
   * @param args
   * @return True if authorized; false if not.
   * @throws RSuiteException
   */
  public boolean isAuthorized(AuthorizationService authService, User user,
      ContentAssemblyNodeContainer container, CallArgumentList args) throws RSuiteException {
    // Admins pass this test.
    if (authService.isAdministrator(user)) {
      return true;
    }

    // Only perform this check if at least one role was passed in.
    String names = args.getFirstString(PARAM_NAME_ALLOWED_CONTAINER_ROLES);
    if (StringUtils.isNotBlank(names)) {
      String[] arr = names.split(",");
      String prefix = AclMap.getContainerRoleNamePrefix(container);
      for (String basename : arr) {
        if (user.hasRole(AclMap.getContainerRoleName(prefix, basename))) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Determine if this user is not authorized to add content within the provided container.
   * 
   * @param authService
   * @param user
   * @param container
   * @param args
   * @return True if unauthorized; false if authorized.
   * @throws RSuiteException
   */
  public boolean isNotAuthorized(AuthorizationService authService, User user,
      ContentAssemblyNodeContainer container, CallArgumentList args) throws RSuiteException {
    return !isAuthorized(authService, user, container, args);
  }

  /**
   * Throw exception when user is not authorized to add content with the provided container.
   * 
   * @param authService
   * @param user
   * @param container
   * @param args
   * @throws RSuiteException Thrown when user is not authorized, or when the RSuite API throws an
   *         exception.
   */
  public void throwIfNotAuthorized(AuthorizationService authService, User user,
      ContentAssemblyNodeContainer container, CallArgumentList args) throws RSuiteException {
    if (isNotAuthorized(authService, user, container, args)) {
      throw new RSuiteException("The user account with ID '" + user.getUserId()
          + "' is not authorized to add content within '" + container.getDisplayName() + "' (ID: "
          + container.getId() + ").");
    }
  }

  /**
   * @return The ID of the container to add within.
   */
  public String getContainerId() {
    return containerId;
  }

  /**
   * @return The ID of the existing MO to add before or after.
   */
  public String getExistingMoId() {
    return existingMoId;
  }

  /**
   * @return The ID of the existing MO reference to add before or after.
   */
  public String getExistingMoRefId() {
    return existingMoRefId;
  }

  /**
   * @return The ID of the object to insert before. This could be the ID of an MO ref, CA ref or
   *         CANode. It could also be null.
   */
  public String getInsertBeforeId() {
    return insertBeforeId;
  }

  /**
   * @return The container wizard's XML MO configuration index.
   */
  public int getXmlMoConfIdx() {
    return xmlMoConfIdx;
  }

}
