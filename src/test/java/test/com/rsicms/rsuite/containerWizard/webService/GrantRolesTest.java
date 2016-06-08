package test.com.rsicms.rsuite.containerWizard.webService;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.UserType;
import com.reallysi.rsuite.api.security.LocalUserManager;
import com.reallysi.rsuite.service.AuthorizationService;
import com.rsicms.rsuite.containerWizard.AclMap;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

public class GrantRolesTest {
  
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
    User grantedUser = invokeService.grantRoles(authService, user, aclMap);
    
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
    Mockito.doNothing().when(localUserManager).updateUser(Mockito.anyString(), 
        Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    Mockito.doNothing().when(localUserManager).reload();
    Mockito.when(localUserManager.getUser(Mockito.anyString())).thenReturn(user);
        
    AuthorizationService authService = Mockito.mock(AuthorizationService.class);
    
    // user is not an administrator
    Mockito.when(authService.isAdministrator(user)).thenReturn(false);
    Mockito.when(authService.getLocalUserManager()).thenReturn(localUserManager);
        
    AclMap aclMap = Mockito.mock(AclMap.class);
    Mockito.when(aclMap.getRoleNames(user)).thenReturn(Arrays.asList("Authors", "Reviewers"));
    
    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();
    User grantedUser = invokeService.grantRoles(authService, user, aclMap);
    
    assertNotNull(grantedUser);
    
  }
  
  /**
   * Throw exception if it is not administrator nor a local user.
   * @throws RSuiteException 
   */
  @Test
  public void throwExceptionForNoneAdministratorNoneLocalUser() throws RSuiteException {
    
    User user = Mockito.mock(User.class);
    
    // user is not a local user
    Mockito.when(user.getUserType()).thenReturn(UserType.LDAP);
    
    AuthorizationService authService = Mockito.mock(AuthorizationService.class);
    
    // user is not an administrator
    Mockito.when(authService.isAdministrator(user)).thenReturn(false);
    
    AclMap aclMap = Mockito.mock(AclMap.class);
    
    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();
    
    try {
      invokeService.grantRoles(authService, user, aclMap);
    } catch (RSuiteException e) {
      assertThat("Error message should indicate This feature does not yet support non-local users.",
          e.getMessage(), containsString("This feature does not yet support non-local users"));
      return;
    }
    
    fail("No or other exception is thrown.");
    
  }

}
