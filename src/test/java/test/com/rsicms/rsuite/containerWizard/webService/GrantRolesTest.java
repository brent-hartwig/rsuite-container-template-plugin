package test.com.rsicms.rsuite.containerWizard.webService;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

public class GrantRolesTest implements ContainerWizardConstants {
  
  // Stub Role to enable changes by localUserManager
  class RoleImpl implements Role {
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
  class UserImpl implements User {
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
  String productId = "12345";

  /**
   * Grant role if it is an administrator.
   */
  @Test
  public void grantRoleForAdministrator() throws RSuiteException {

    User user = Mockito.mock(User.class);
    AuthorizationService authService = Mockito.mock(AuthorizationService.class);

    // user is an administrator
    Mockito.when(authService.isAdministrator(user)).thenReturn(true);
    AclMap aclMap = Mockito.mock(AclMap.class);

    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();
    User grantedUser =
        invokeService.grantRoles(authService, user, aclMap, CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT);

    /*
     * TODO: Is this test only making sure an exception isn't thrown for admin users? Why aren't we
     * testing getRoleNames() to verify no roles are added or removed when user is an admin?
     */
    assertNotNull(grantedUser);


  }

  /**
   * Grant role if it is a local user.
   */
  @Test
  public void grantRoleForLocalUser() throws RSuiteException {

    User user = Mockito.mock(User.class);

    // user is a local user
    Mockito.when(user.getUserType()).thenReturn(UserType.LOCAL);

    LocalUserManager localUserManager = Mockito.mock(LocalUserManager.class);
    Mockito.doNothing().when(localUserManager).updateUser(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString());
    Mockito.doNothing().when(localUserManager).reload();
    Mockito.when(localUserManager.getUser(Mockito.anyString())).thenReturn(user);

    AuthorizationService authService = Mockito.mock(AuthorizationService.class);

    // user is not an administrator
    Mockito.when(authService.isAdministrator(user)).thenReturn(false);
    Mockito.when(authService.getLocalUserManager()).thenReturn(localUserManager);

    AclMap aclMap = Mockito.mock(AclMap.class);
    Mockito.when(aclMap.getRoleNames(user, CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT))
        .thenReturn(Arrays.asList("Authors", "Reviewers"));

    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();
    User grantedUser =
        invokeService.grantRoles(authService, user, aclMap, CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT);

    /*
     * TODO: Is this test only making sure an exception isn't thrown for local users? Why aren't we
     * testing getRoleNames() to verify which role(s) is added, and that none are removed?
     */
    assertNotNull(grantedUser);

  }
  
  /**
   * Grant a local user with AIC_AD role.
   */
  @Test
  public void grantUserWithAicAdRole() throws RSuiteException {
    
    String expected = productId + "_" + CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT;
    
    User originalUser = Mockito.mock(User.class);
    Mockito.when(originalUser.getUserType()).thenReturn(UserType.LOCAL);
    // original user does not have any roles
    Mockito.when(originalUser.getRoles()).thenReturn(new Role[0]);
    
    LocalUserManager localUserManager = Mockito.mock(LocalUserManager.class);
    // Mock localUserManager.updateUser() to create a User with one Role that has name
    // with passed product id and group suffix.
    Mockito.doAnswer(new Answer<User>() {
      @Override
      public User answer(InvocationOnMock invocation) throws Throwable {
        RoleImpl role = new RoleImpl();
        role.setName(productId + "_" + (String)invocation.getArguments()[3]);
        createdUser = new UserImpl();
        createdUser.addRole(role);
        return createdUser;
      }
    }).when(localUserManager).updateUser(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString());
    Mockito.doNothing().when(localUserManager).reload();
    //Mock localUserManager.getUser() to return the created user.
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
    Mockito.when(aclMap.getRoleNames(originalUser, CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT))
        .thenReturn(Arrays.asList("AIC_AD"));

    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();
    User grantedUser =
        invokeService.grantRoles(authService, originalUser, aclMap, CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT);
    
    assertEquals(originalUser.getRoles().length, 0);
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
