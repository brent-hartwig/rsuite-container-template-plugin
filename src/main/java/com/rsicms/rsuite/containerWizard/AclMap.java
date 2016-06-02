package com.rsicms.rsuite.containerWizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class AclMap extends HashMap<String, ACL> {

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(AclMap.class);

  protected static final String UNDERSCORE = "_";

  public AclMap(SecurityService securityService, ContainerWizardConf conf,
      String projectRoleNamePrefix) throws RSuiteException {
    super();

    if (conf != null && conf.getAcls() != null && conf.getAcls().getAcl() != null) {
      ACE[] rsuiteAceArr;
      for (Acl acl : conf.getAcls().getAcl()) {
        rsuiteAceArr = new ACE[acl.getAce().size()];
        Ace ace;
        for (int i = 0; i < acl.getAce().size(); i++) {
          ace = acl.getAce().get(i);
          rsuiteAceArr[i] =
              securityService.constructACE(getRoleName(projectRoleNamePrefix, ace.getProjectRole()),
                  ace.getContentPermissions().replaceAll(StringUtils.SPACE, StringUtils.EMPTY));
        }
        put(acl.getId(), securityService.constructACL(rsuiteAceArr));
      }
    }
  }

  public void createUndefinedRoles(User user, RoleManager roleManager) throws RSuiteException {
    // Create list of roles associated to this map.
    List<String> roleNames = new ArrayList<String>();
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
        log.info("Defined the '" + roleName + "' role");
      }
    }
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
   * This could be made into a utility method by passing the map or its collection in.
   * 
   * @param user When a local user, the local user's roles are also included in the return. If not
   *        desired, pass in null (or use the signature that does).
   * @return A list of role names associated with this AclMap.
   */
  public List<String> getRoleNames(User user) {
    List<String> names = new ArrayList<String>();

    // Conditionally add the given user's roles.
    if (user != null && user.getUserType() == UserType.LOCAL) {
      for (Role role : user.getRoles()) {
        if (!names.contains(role.getName())) {
          names.add(role.getName());
        }
      }
    }

    // Add the roles in this map.
    Iterator<ACL> aclIt = values().iterator();
    Iterator<ACE> aceIt;
    ACL acl;
    ACE ace;
    String name;
    while (aclIt.hasNext()) {
      acl = aclIt.next();
      aceIt = acl.iterator();
      while (aceIt.hasNext()) {
        ace = aceIt.next();
        name = ace.getRole().getName();
        if (!names.contains(name)) {
          names.add(name);
        }
      }
    }

    return names;
  }

  public static String getRoleName(String projectRoleNamePrefix, String baseRoleName) {
    return new StringBuilder(projectRoleNamePrefix).append(UNDERSCORE).append(baseRoleName)
        .toString().replaceAll("[^\\p{Alnum}]", UNDERSCORE);
  }

}
