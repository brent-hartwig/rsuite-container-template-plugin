package com.rsicms.rsuite.containerWizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.UserType;
import com.reallysi.rsuite.api.security.ACE;
import com.reallysi.rsuite.api.security.ACL;
import com.reallysi.rsuite.api.security.Role;
import com.reallysi.rsuite.api.security.RoleDescriptor;
import com.reallysi.rsuite.api.security.RoleManager;
import com.reallysi.rsuite.service.SecurityService;
import com.rsicms.rsuite.containerWizard.jaxb.Ace;
import com.rsicms.rsuite.containerWizard.jaxb.Acl;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;

/**
 * Serve up a map of RSuite ACLs defined by a container wizard configuration instance.
 */
public class AclMap
    extends HashMap<String, ACL> {

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(AclMap.class);

  protected static final String UNDERSCORE = "_";
  protected static final String SPACE = " ";

  /**
   * Construct and populate an ACL map from the provided container wizard configuration and the
   * container role name's prefix.
   * 
   * @param securityService
   * @param conf
   * @param containerRoleNamePrefix
   * @throws RSuiteException
   */
  public AclMap(
      SecurityService securityService, ContainerWizardConf conf, String containerRoleNamePrefix)
      throws RSuiteException {
    super();

    if (conf != null && conf.getAcls() != null && conf.getAcls().getAcl() != null) {
      ACE[] rsuiteAceArr;
      for (Acl acl : conf.getAcls().getAcl()) {
        rsuiteAceArr = new ACE[acl.getAce().size() + 1];
        Ace ace;
        for (int i = 0; i < acl.getAce().size(); i++) {
          ace = acl.getAce().get(i);
          rsuiteAceArr[i] = securityService.constructACE(getContainerRoleName(
              containerRoleNamePrefix, ace.getProjectRole()), ace.getContentPermissions()
                  .replaceAll(SPACE, StringUtils.EMPTY));
        }

        // FIXME: Below, we're hard-coding a project-agnostic role of "Viewer". If anyone else uses
        // this code, they'll probably want to make this part configurable.
        rsuiteAceArr[acl.getAce().size()] = securityService.constructACE("Viewers", "list, view"
            .replaceAll(SPACE, StringUtils.EMPTY));

        put(acl.getId(), securityService.constructACL(rsuiteAceArr));
      }
    }
  }

  /**
   * Create any roles known by this map but not by RSuite.
   * 
   * @param user
   * @param roleManager
   * @return Names of roles created by this method. May be empty, but not null.
   * @throws RSuiteException
   */
  public List<String> createUndefinedContainerRoles(User user, RoleManager roleManager)
      throws RSuiteException {
    // Create list of roles associated to this map.
    List<String> roleNames = new ArrayList<String>();
    List<String> resultRoleNames = new ArrayList<String>();
    for (Map.Entry<String, ACL> entry : this.entrySet()) {
      ACL acl = entry.getValue();
      Iterator<ACE> it = acl.iterator();
      while (it.hasNext()) {
        ACE ace = it.next();
        if (!roleNames.contains(ace.getRole().getName())) {
          roleNames.add(ace.getRole().getName());
        }
      }
    }

    // See which ones need to be created.
    List<RoleDescriptor> existingRoles = roleManager.getRoles();
    for (String roleName : roleNames) {
      log.info("Processing '" + roleName + "'...");
      boolean found = false;
      for (RoleDescriptor existingRole : existingRoles) {
        if (existingRole.getRole().getName().equalsIgnoreCase(roleName)) {
          found = true;
          break;
        }
      }
      if (!found) {
        roleManager.createRole(user, roleName, roleName, roleName);
        resultRoleNames.add(roleName);
        log.info("Defined the '" + roleName + "' role");
      }
    }
    return resultRoleNames;
  }

  /**
   * Get a list of role names from ACLs.
   * <p>
   * This could be made into a utility method by passing the map or its collection in.
   * 
   * @return A list of role names associated with this AclMap.
   */
  public List<String> getRoleNames() {
    return getRoleNames(null);
  }

  /**
   * Get a list of distinct role names from this AclMap, plus those of the included user.
   * <p>
   * Expecting this is only called for non-admins, although no check is performed herein.
   * <p>
   * The second parameter may be used to restrict which roles from the AclMap are included in the
   * response.
   * <p>
   * This could be made into a utility method by passing the map or its collection in.
   * 
   * @param user When a local user, the local user's roles are also included in the return. If not
   *        desired, pass in null (or use the signature that does).
   * @param allowedRoleNameSuffixes Use to restrict which roles configured in the AclMap are
   *        included in the return. Values are matched on the suffix of the role name. If no
   *        suffixes are provided, all from the AclMap are included.
   * @return The user's current roles plus all qualifying ones from the AclMap.
   */
  public List<String> getRoleNames(User user, String... allowedRoleNameSuffixes) {
    List<String> names = new ArrayList<String>();

    // Conditionally add the given user's roles.
    if (user != null && user.getUserType() == UserType.LOCAL) {
      for (Role role : user.getRoles()) {
        if (!names.contains(role.getName())) {
          names.add(role.getName());
        }
      }
    }

    // Iterate through the AclMap's ACLs, only adding roles that the user doesn't
    // already have, and that match the specified role name suffixes.
    Iterator<ACL> aclIt = values().iterator();
    Iterator<ACE> aceIt;
    ACL acl;
    ACE ace;
    String name;
    boolean add;
    while (aclIt.hasNext()) {
      acl = aclIt.next();
      aceIt = acl.iterator();
      while (aceIt.hasNext()) {
        add = false;
        ace = aceIt.next();
        name = ace.getRole().getName();
        if (!names.contains(name)) {
          if (allowedRoleNameSuffixes == null || allowedRoleNameSuffixes.length == 0) {
            add = true;
          } else {
            for (String suffix : allowedRoleNameSuffixes) {
              if (name.endsWith(suffix)) {
                add = true;
                break;
              }
            }
          }

          if (add) {
            names.add(name);
          }
        }
      }
    }

    return names;
  }

  /**
   * @param container
   * @return The prefix to use for container roles.
   */
  public static String getContainerRoleNamePrefix(ContentAssemblyNodeContainer container) {
    return container.getId();
  }

  /**
   * Implements the logic of combining the container role name prefix with the base role name.
   * 
   * @param containerRoleNamePrefix
   * @param baseRoleName
   * @return A container name using the prefix and base name.
   */
  public static String getContainerRoleName(String containerRoleNamePrefix, String baseRoleName) {
    return new StringBuilder(containerRoleNamePrefix).append(UNDERSCORE).append(baseRoleName.trim())
        .toString().replaceAll("[^\\p{Alnum}]", UNDERSCORE);
  }

}
