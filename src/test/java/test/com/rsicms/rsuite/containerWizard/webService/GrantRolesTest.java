package test.com.rsicms.rsuite.containerWizard.webService;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.UserType;
import com.reallysi.rsuite.api.security.ExecPermission;
import com.reallysi.rsuite.api.security.LocalUserManager;
import com.reallysi.rsuite.api.security.Role;
import com.reallysi.rsuite.service.AuthorizationService;
import com.rsicms.rsuite.containerWizard.AclMap;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

public class GrantRolesTest
    implements ContainerWizardConstants {

  // Stub Role to enable changes by localUserManager
  class RoleImpl
      implements Role {
    String name = null;

    public void setName(String name) {
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

  // Stub User to enable changes by localUserManager
  class UserImpl
      implements User {
    List<Role> keptRoles = new ArrayList<Role>();

    public void addRole(Role role) {
      keptRoles.add(role);
    }

    @Override
    public boolean hasRole(String arg0) {
      return false;
    }

    @Override
    public boolean hasExecPermission(String arg0) {
      return false;
    }

    @Override
    public boolean hasExecPermission(ExecPermission arg0) {
      return false;
    }

    @Override
    public Role[] getRoles() {
      return (Role[]) keptRoles.toArray(new Role[keptRoles.size()]);
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public ExecPermission[] getExecPermissions() {
      return null;
    }

    @Override
    public boolean isLocalUser() {
      return false;
    }

    @Override
    public UserType getUserType() {
      return null;
    }

    @Override
    public String getUserId() {
      return null;
    }

    @Override
    public String getUserID() {
      return null;
    }

    @Override
    public String getUserDN() {
      return null;
    }

    @Override
    public String getPassword() {
      return null;
    }

    @Override
    public String[] getGroups() {
      return null;
    }

    @Override
    public String getFullName() {
      return null;
    }

    @Override
    public String getEmail() {
      return null;
    }
  };

  UserImpl createdUser;
  String productNumber = "12345";

  /**
   * Grant role if it is an administrator.
   */
  @Test
  public void grantRoleForAdministrator() throws RSuiteException {

    User user = Mockito.mock(User.class);
    AuthorizationService authService = Mockito.mock(AuthorizationService.class);
    Mockito.when(user.getRoles()).thenReturn(new Role[0]);
    int originalRoleSize = user.getRoles().length;

    // user is an administrator
    Mockito.when(authService.isAdministrator(user)).thenReturn(true);
    AclMap aclMap = Mockito.mock(AclMap.class);

    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();
    User grantedUser = invokeService.grantRoles(authService, user, aclMap,
        CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT);

    // The same Admin user should be returned
    assertEquals(user == grantedUser, true);

    // No new roles are added
    assertEquals(grantedUser.getRoles().length, originalRoleSize);

  }

  /**
   * Grant role if it is a local user.
   */
  @Test
  public void grantRoleForLocalUser() throws RSuiteException {

    String expectedName1 = productNumber + "_" + "Authors";
    String expectedName2 = productNumber + "_" + "Reviewers";

    User originalUser = Mockito.mock(User.class);

    // user is a local user
    Mockito.when(originalUser.getUserType()).thenReturn(UserType.LOCAL);
    Mockito.when(originalUser.getRoles()).thenReturn(new Role[0]);
    int originalRoleSize = originalUser.getRoles().length;

    LocalUserManager localUserManager = Mockito.mock(LocalUserManager.class);
    // Mock localUserManager.updateUser() to create a User with Roles that have names
    // with passed product number and group suffixes.
    Mockito.doAnswer(new Answer<User>() {
      @Override
      public User answer(InvocationOnMock invocation) throws Throwable {
        createdUser = new UserImpl();
        String inputedSuffixes = (String) invocation.getArguments()[3];
        String[] suffixes = inputedSuffixes.split(",");
        for (String suffix : suffixes) {
          RoleImpl role = new RoleImpl();
          role.setName(productNumber + "_" + suffix);
          createdUser.addRole(role);
        }
        return createdUser;
      }
    }).when(localUserManager).updateUser(Mockito.anyString(), Mockito.anyString(), Mockito
        .anyString(), Mockito.anyString());
    Mockito.doNothing().when(localUserManager).reload();
    // Mock localUserManager.getUser() to return the created user.
    Mockito.when(localUserManager.getUser(Mockito.anyString())).thenAnswer(new Answer<User>() {
      @Override
      public User answer(InvocationOnMock invocation) throws Throwable {
        return createdUser;
      }
    });

    AuthorizationService authService = Mockito.mock(AuthorizationService.class);

    // user is not an administrator
    Mockito.when(authService.isAdministrator(originalUser)).thenReturn(false);
    Mockito.when(authService.getLocalUserManager()).thenReturn(localUserManager);

    AclMap aclMap = Mockito.mock(AclMap.class);
    Mockito.when(aclMap.getRoleNames(originalUser, "Authors", "Reviewers")).thenReturn(Arrays
        .asList("Authors", "Reviewers"));

    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();
    User grantedUser = invokeService.grantRoles(authService, originalUser, aclMap, "Authors",
        "Reviewers");

    // Granted two roles
    assertEquals(grantedUser.getRoles().length, originalRoleSize + 2);
    // Role name is productNumber_Authors
    assertEquals(grantedUser.getRoles()[0].getName(), expectedName1);
    // Role name is productNumber_Reviewers
    assertEquals(grantedUser.getRoles()[1].getName(), expectedName2);

  }

  /**
   * Grant a local user with AIC_AD role.
   */
  @Test
  public void grantUserWithAicAdRole() throws RSuiteException {

    String expected = productNumber + "_" + CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT;

    User originalUser = Mockito.mock(User.class);
    Mockito.when(originalUser.getUserType()).thenReturn(UserType.LOCAL);
    // original user does not have any roles
    Mockito.when(originalUser.getRoles()).thenReturn(new Role[0]);
    int originalRoleSize = originalUser.getRoles().length;

    LocalUserManager localUserManager = Mockito.mock(LocalUserManager.class);
    // Mock localUserManager.updateUser() to create a User with one Role that has name
    // with passed product number and group suffix.
    Mockito.doAnswer(new Answer<User>() {
      @Override
      public User answer(InvocationOnMock invocation) throws Throwable {
        createdUser = new UserImpl();
        String inputedSuffixes = (String) invocation.getArguments()[3];
        String[] suffixes = inputedSuffixes.split(",");
        for (String suffix : suffixes) {
          RoleImpl role = new RoleImpl();
          role.setName(productNumber + "_" + suffix);
          createdUser.addRole(role);
        }
        return createdUser;
      }
    }).when(localUserManager).updateUser(Mockito.anyString(), Mockito.anyString(), Mockito
        .anyString(), Mockito.anyString());
    Mockito.doNothing().when(localUserManager).reload();
    // Mock localUserManager.getUser() to return the created user.
    Mockito.when(localUserManager.getUser(Mockito.anyString())).thenAnswer(new Answer<User>() {
      @Override
      public User answer(InvocationOnMock invocation) throws Throwable {
        return createdUser;
      }
    });

    AuthorizationService authService = Mockito.mock(AuthorizationService.class);

    // user is not an administrator
    Mockito.when(authService.isAdministrator(originalUser)).thenReturn(false);
    Mockito.when(authService.getLocalUserManager()).thenReturn(localUserManager);

    AclMap aclMap = Mockito.mock(AclMap.class);
    Mockito.when(aclMap.getRoleNames(originalUser, CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT)).thenReturn(
        Arrays.asList("AIC_AD"));

    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();
    User grantedUser = invokeService.grantRoles(authService, originalUser, aclMap,
        CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT);

    // No roles on original user
    assertEquals(originalUser.getRoles().length, 0);
    // Granted only one role
    assertEquals(grantedUser.getRoles().length, originalRoleSize + 1);
    // Role name is productNumber_AIC_AD
    assertEquals(grantedUser.getRoles()[0].getName(), expected);

  }

  /**
   * Throw exception if it is not administrator nor a local user.
   * 
   * @throws RSuiteException
   */
  @Test
  public void throwExceptionForNonAdministratorNonLocalUser() throws RSuiteException {

    User user = Mockito.mock(User.class);

    // user is not a local user
    Mockito.when(user.getUserType()).thenReturn(UserType.LDAP);

    AuthorizationService authService = Mockito.mock(AuthorizationService.class);

    // user is not an administrator
    Mockito.when(authService.isAdministrator(user)).thenReturn(false);

    AclMap aclMap = Mockito.mock(AclMap.class);

    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();

    try {
      invokeService.grantRoles(authService, user, aclMap, CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT);
    } catch (RSuiteException e) {
      assertThat("Error message should indicate This feature does not yet support non-local users.",
          e.getMessage(), containsString("This feature does not yet support non-local users"));
      return;
    }

    fail("No or other exception is thrown.");

  }

}
