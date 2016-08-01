package com.rsicms.rsuite.containerWizard;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.utils.container.ContainerUtils;
import com.rsicms.rsuite.utils.container.visitor.ChildrenInfoContainerVisitor;
import com.rsicms.rsuite.utils.mo.MOUtils;

/**
 * Provides context to the "add XML MO" execution mode of the container wizard.
 */
public class AddXmlMoContext
    implements Serializable, ContainerWizardConstants {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unused")
  private static Log log = LogFactory.getLog(AddXmlMoContext.class);

  private String containerId;
  private String parentMoId;
  private boolean checkInParentMo;
  private String existingMoId;
  private String insertBeforeId;
  private String insertAfterId;
  private int xmlMoConfIdx;

  /**
   * Construct an instance, inspecting the parameters in order to make all getter methods
   * immediately ready for use.
   * 
   * @param context
   * @param user
   * @param conf
   * @param confUtils
   * @param args
   * @throws RSuiteException
   */
  public AddXmlMoContext(
      ExecutionContext context, User user, ContainerWizardConf conf,
      ContainerWizardConfUtils confUtils, CallArgumentList args)
      throws RSuiteException {

    ManagedObjectService moService = context.getManagedObjectService();

    boolean insertBefore = InsertionPosition.get(args.getFirstString(
        PARAM_NAME_INSERTION_POSITION)) == InsertionPosition.BEFORE;

    this.existingMoId = args.getFirstString(PARAM_NAME_RSUITE_ID);
    if (StringUtils.isBlank(this.existingMoId)) {
      throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
          "The ID of the object to insert before or after was not provided.");
    }

    ContentAssemblyNodeContainer container = null;
    ManagedObject existingMo = moService.getManagedObject(user, this.existingMoId);
    String existingMoLocalName = existingMo.getLocalName();
    String localNameBefore = null;
    String localNameAfter = null;
    boolean isSubMo = MOUtils.isSubMo(moService, user, existingMo);
    ManagedObject parentMo = null;
    ManagedObject siblingMo = null;
    if (isSubMo) {
      parentMo = moService.getParentManagedObject(user, existingMo);
      this.parentMoId = parentMo.getId();

      // Verify user may add content in the parent MO.
      throwIfNotAuthorized(context, user, parentMo.getId(), args);

      // Require check out, and remember if we should check it in at the end.
      this.checkInParentMo = !user.getUserId().equals(parentMo.getCheckedOutUser());
      MOUtils.checkout(context, user, parentMo.getId());

      if (insertBefore) {
        this.insertBeforeId = existingMo.getId();
        localNameAfter = existingMoLocalName;
        if (!existingMo.isFirstChild()) {
          siblingMo = MOUtils.getPrecedingSubMo(moService, user, existingMo);
          if (siblingMo != null) {
            localNameBefore = siblingMo.getLocalName();
          }
        }
      } else {
        this.insertAfterId = existingMo.getId();
        localNameBefore = existingMoLocalName;
        if (!existingMo.isLastChild()) {
          siblingMo = MOUtils.getFollowingSubMo(moService, user, existingMo);
          if (siblingMo != null) {
            localNameAfter = siblingMo.getLocalName();
          }
        }
      }
    } else {
      // When not a sub-MO, require a container, of a specific type.
      container = ContainerUtils.getContentAssemblyNodeContainer(context
          .getContentAssemblyService(), user, args.getFirstContentObjectPath(user), conf
              .getPrimaryContainer().getType());
      if (container == null) {
        throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
            "Unable to determine the container.");
      }
      this.containerId = container.getId();

      // Verify user may add content in this container.
      throwIfNotAuthorized(context, user, container.getId(), args);

      // Visit container in order to answer some questions.
      ChildrenInfoContainerVisitor visitor = new ChildrenInfoContainerVisitor(context, user);
      visitor.visitContentAssemblyNodeContainer(container);

      // Let's see what we found.
      ContentAssemblyItem siblingCaItem = insertBefore ? visitor.getAnySiblingBefore(existingMoId)
          : visitor.getAnySiblingAfter(existingMoId);
      ManagedObjectReference siblingMoRef = insertBefore ? visitor.getMoRefSiblingBefore(
          existingMoId) : visitor.getMoRefSiblingAfter(existingMoId);
      if (siblingMoRef != null) {
        siblingMo = context.getManagedObjectService().getManagedObject(user, siblingMoRef
            .getTargetId());
      }

      // Determine the ID of the object to insert before. When null, the system will be expected to
      // add at the bottom. As there could be CA refs or CANodes between two MO refs, using the
      // sibling CA item here, which covers all three.
      if (insertBefore) {
        this.insertBeforeId = this.existingMoId;
      } else if (siblingCaItem != null) {
        this.insertBeforeId = siblingCaItem.getId();
      } else {
        this.insertBeforeId = null;
      }

      if (insertBefore && siblingMo != null) {
        localNameBefore = siblingMo.getLocalName();
      } else if (!insertBefore) {
        localNameBefore = existingMoLocalName;
      }

      if (!insertBefore && siblingMo != null) {
        localNameAfter = siblingMo.getLocalName();
      } else if (insertBefore) {
        localNameAfter = existingMoLocalName;
      }
    }

    // TODO: we also need to know the last allowed index, and get that to the section
    // type web service.
    this.xmlMoConfIdx = confUtils.getFirstAllowedXmlMoConfIndex(conf, localNameBefore,
        localNameAfter);

    if (this.xmlMoConfIdx < 0) {
      throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
          "No additional content is allowed " + (insertBefore ? "before" : "after")
              + " the specified content.");
    }
  }

  /**
   * Determine if this user is authorized to add content within the specified object.
   * 
   * @param context
   * @param user
   * @param id ID of MO or container.
   * @param args
   * @return True if authorized; false if not.
   * @throws RSuiteException
   */
  public boolean isAuthorized(ExecutionContext context, User user, String id, CallArgumentList args)
      throws RSuiteException {
    // Admins are authorized.
    if (context.getAuthorizationService().isAdministrator(user)) {
      return true;
    }

    // Require the non-admin have edit permission
    return context.getSecurityService().hasEditPermission(user, id);
  }

  /**
   * Determine if this user is not authorized to add content within the specified object.
   * 
   * @param context
   * @param user
   * @param id ID of MO or container.
   * @param args
   * @return True if unauthorized; false if authorized.
   * @throws RSuiteException
   */
  public boolean isNotAuthorized(ExecutionContext context, User user, String id,
      CallArgumentList args) throws RSuiteException {
    return !isAuthorized(context, user, id, args);
  }

  /**
   * Throw exception when user is not authorized to add content within the specified object.
   * 
   * @param context
   * @param user
   * @param id ID of MO or container.
   * @param args
   * @throws RSuiteException Thrown when user is not authorized, or when the RSuite API throws an
   *         exception.
   */
  public void throwIfNotAuthorized(ExecutionContext context, User user, String id,
      CallArgumentList args) throws RSuiteException {
    if (isNotAuthorized(context, user, id, args)) {
      ManagedObject mo = context.getManagedObjectService().getManagedObject(user, id);
      throw new RSuiteException("The user account with ID '" + user.getUserId()
          + "' is not authorized to add content within '" + MOUtils.getDisplayNameQuietly(mo)
          + "' (ID: " + mo.getId() + ").");
    }
  }

  /**
   * The ID of the container to add within, when starting with a top-level MO.
   * 
   * @return The ID of the container to add within, when starting with a top-level MO.
   */
  public String getContainerId() {
    return containerId;
  }

  /**
   * The ID of the parent MO to add within, when starting with a sub-MO.
   * 
   * @return The ID of the parent MO to add within, when starting with a sub-MO.
   */
  public String getParentMoId() {
    return parentMoId;
  }

  /**
   * Find out if the new MOs should be top-level MOs.
   * 
   * @return True when the new MOs should be created as top-level MOs and attached to the container.
   */
  public boolean shouldCreateAsTopLevelMos() {
    return StringUtils.isNotBlank(getContainerId());
  }

  /**
   * Find out if the new MOs should be sub-MOs.
   * 
   * @return True when the new MOs should be created as sub-MOs of the parent MO.
   */
  public boolean shouldCreateAsSubMos() {
    return StringUtils.isNotBlank(getParentMoId());
  }

  /**
   * Find out if the parent MO should be checked in once the operation is complete.
   * 
   * @return True if the parent MO should be checked in once the operation is complete.
   */
  public boolean shouldCheckInParentMo() {
    return checkInParentMo;
  }

  /**
   * The ID of the existing MO to add before or after.
   * 
   * @return The ID of the existing MO to add before or after.
   */
  public String getExistingMoId() {
    return existingMoId;
  }

  /**
   * Get the ID of the object the new content should be added before. Always use this when creating
   * top-level MOs, even when it returns null. If this returns null when creating sub-MOs, use
   * {@link #getInsertAfterId()}.
   * 
   * @return The ID of the object to insert before. It is not a reference ID. The caller may need to
   *         derive the reference. May be null.
   */
  public String getInsertBeforeId() {
    return insertBeforeId;
  }

  /**
   * Get the ID of the object the new content should be added after. Never use this when creating
   * top-level MOs. It should only be used when creating sub-MOs and {@link #getInsertBeforeId()}
   * returns null.
   * 
   * @return The ID of the object to insert after. It should only be null when creating top-level
   *         MOs or when {@link #getInsertBeforeId()} returns null.
   */
  public String getInsertAfterId() {
    return insertAfterId;
  }

  /**
   * The container wizard's XML MO configuration index.
   * 
   * @return The container wizard's XML MO configuration index.
   */
  public int getXmlMoConfIdx() {
    return xmlMoConfIdx;
  }

}
