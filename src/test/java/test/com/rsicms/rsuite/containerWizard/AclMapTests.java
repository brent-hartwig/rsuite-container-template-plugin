package test.com.rsicms.rsuite.containerWizard;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;

import com.reallysi.rsuite.api.Principal;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.security.ACE;
import com.reallysi.rsuite.api.security.ACL;
import com.reallysi.rsuite.api.security.ContentPermission;
import com.reallysi.rsuite.api.security.Role;
import com.reallysi.rsuite.api.security.RoleManager;
import com.reallysi.rsuite.service.SecurityService;
import com.rsicms.rsuite.containerWizard.AclMap;
import com.rsicms.rsuite.containerWizard.jaxb.Ace;
import com.rsicms.rsuite.containerWizard.jaxb.Acl;
import com.rsicms.rsuite.containerWizard.jaxb.Acls;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;

public class AclMapTests {

  // Stub RSuite security role implementation for security service
  public class RoleImpl implements Role {
    String name = null;

    public RoleImpl(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getDescription() {
      return null;
    }
  };

  // Stub RSuite security ACE implementation for security service
  public class ACEImpl implements ACE {
    Role role = null;

    public ACEImpl(Role role) {
      this.role = role;
    }

    @Override
    public boolean isRoleAny() {
      return false;
    }

    @Override
    public boolean hasNoPermissions() {
      return false;
    }

    @Override
    public boolean grantsPermission(ContentPermission contentpermission) {
      return false;
    }

    @Override
    public Role getRole() {
      return role;
    }

    @Override
    public String getPermissionsAsCommaDelimited() {
      return null;
    }

    @Override
    public Set<ContentPermission> getPermissions() {
      return null;
    }

    @Override
    public int getPermissionMask() {
      return 0;
    }
  };

  // Stub RSuite security ACL implementation for security service
  public class ACLImpl implements ACL {
    ACE[] aces = null;

    public ACLImpl(ACE[] aces) {
      this.aces = aces;
    }

    @Override
    public Iterator<ACE> iterator() {
      return new SimpleIterator();
    }

    class SimpleIterator implements Iterator<ACE> {
      private int cursor = 0;

      @Override
      public boolean hasNext() {
        return this.cursor < size();
      }

      @Override
      public ACE next() {
        if (!this.hasNext()) {
          throw new NoSuchElementException();
        }
        return aces[cursor++];
      }

      @Override
      public void remove() {}
    }

    @Override
    public int size() {
      return aces.length;
    }

    @Override
    public boolean grantsPermission(ContentPermission contentpermission, Principal principal) {
      return false;
    }

    @Override
    public String getPermissionsAsCommaDelimited(Role[] arole) {
      return null;
    }

    @Override
    public String getPermissionsAsCommaDelimited() {
      return null;
    }

    @Override
    public int getPermissionMask(Role[] arole) {
      return 0;
    }

    @Override
    public int getPermissionMask(Role role) {
      return 0;
    }

    @Override
    public ACL copy() {
      return null;
    }
  };

  public final static String DELIM = "_";

  /**
   * Verify an undefined role is created with the new container id and role name.
   */
  @Test
  public void createAnUndefinedRole() throws RSuiteException {

    String newContainerId = "12345";

    // Mock access control in container wizard configuration
    Ace containerAce = new Ace();
    containerAce.setContentPermissions("list, view, edit");
    containerAce.setProjectRole("Authors");
    Acl containerAcl = new Acl();
    containerAcl.setId("non-feedback-containers");
    containerAcl.getAce().add(containerAce);
    Acls containerAcls = new Acls();
    containerAcls.getAcl().add(containerAcl);

    Role role = new RoleImpl(newContainerId + DELIM + "Authors");

    ACE ace = new ACEImpl(role);

    ACE[] aces = new ACE[1];
    aces[0] = ace;
    ACL acl = new ACLImpl(aces);

    String roleName = AclMap.getContainerRoleName(newContainerId, containerAce.getProjectRole());

    SecurityService securityService = Mockito.mock(SecurityService.class);
    Mockito
        .when(securityService.constructACE(roleName,
            containerAce.getContentPermissions().replaceAll(StringUtils.SPACE, StringUtils.EMPTY)))
        .thenReturn(ace);
    Mockito.when(securityService.constructACL(aces)).thenReturn(acl);
    ContainerWizardConf conf = Mockito.mock(ContainerWizardConf.class);
    Mockito.when(conf.getAcls()).thenReturn(containerAcls);

    // Construct aclMap with mocked security service and container wizard configuration
    AclMap aclMap = new AclMap(securityService, conf, newContainerId);

    User user = Mockito.mock(User.class);
    RoleManager roleManager = Mockito.mock(RoleManager.class);
    Mockito.doNothing().when(roleManager).createRole(user, roleName, roleName, roleName);

    // call createUndefinedRoles with mocked user and roleManager
    List<String> resultRoleNames = aclMap.createUndefinedContainerRoles(user, roleManager);

    String resultRoleName = resultRoleNames.get(0);
    String expectedRoleName = new StringBuilder(newContainerId).append(DELIM)
        .append(containerAce.getProjectRole()).toString();
    assertEquals(resultRoleName, expectedRoleName);

    // createRole is a void method, but we can verify it's really called once in the unit test.
    Mockito.verify(roleManager, Mockito.times(1)).createRole(user, roleName, roleName, roleName);

  }

  /**
   * Verify receipt of expected role name when input only includes alphanumeric characters.
   */
  @Test
  public void roleNameWithOnlyAlphaNumChars() {
    String prefix = "1prefix9prefix1234";
    String base = "11base55base77";

    String actualName = AclMap.getContainerRoleName(prefix, base);
    String expectedName = new StringBuilder(prefix).append(DELIM).append(base).toString();

    assertEquals(expectedName, actualName);
  }

  /**
   * Verify receipt of expected role name when input includes non-alphanumeric characters.
   */
  @Test
  public void roleNameWithNonAlphaNumChars() {
    String prefix = "my$Pre*f)ix3";
    String base = "7my!@#$%^&*()BaseRoleName";

    String cleanPrefix = prefix.replaceAll("[^\\p{Alnum}]", DELIM);
    String cleanBase = base.replaceAll("[^\\p{Alnum}]", DELIM);

    String actualName = AclMap.getContainerRoleName(prefix, base);
    String expectedName = new StringBuilder(cleanPrefix).append(DELIM).append(cleanBase).toString();

    assertEquals(expectedName, actualName);
  }

}
