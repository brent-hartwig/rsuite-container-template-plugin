package test.com.rsicms.rsuite.containerWizard.webService;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.service.AuthorizationService;
import com.reallysi.rsuite.service.SearchService;
import com.rsicms.rsuite.containerWizard.ContainerWizard;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.FutureManagedObject;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

public class InvokeContainerWizardWebServiceTest implements ContainerWizardConstants {

  /**
   * Verify the container name is set in container wizard.
   */
  @Test
  public void setContainerName() throws RSuiteException {

    String expectedContainerName = "Container Name";

    ContainerWizard wizard = new ContainerWizard();
    CallArgument arg = new CallArgument(PARAM_NAME_CONTAINER_NAME, expectedContainerName);
    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg);
    CallArgumentList args = new CallArgumentList(argList);
    SearchService searchService = Mockito.mock(SearchService.class);
    User user = Mockito.mock(User.class);

    InvokeContainerWizardWebService service = new InvokeContainerWizardWebService();
    service.retainUserInput(searchService, user, wizard, args, -1);

    String resultContainerName = wizard.getContainerName();
    assertEquals(resultContainerName, expectedContainerName);

  }

  /**
   * Verify that RSuiteException is thrown when jobCode is already assigned.
   */
  @Test
  public void jobCodeIsAlreadyAssigned() throws RSuiteException {

    String jobCode = "111111";
    List<ManagedObject> containers = new ArrayList<ManagedObject>();
    ManagedObject mo = Mockito.mock(ManagedObject.class);
    containers.add(mo);
    CallArgument arg = new CallArgument("lmdJobCode", jobCode);
    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg);
    CallArgumentList args = new CallArgumentList(argList);

    ContainerWizard wizard = Mockito.mock(ContainerWizard.class);
    SearchService searchService = Mockito.mock(SearchService.class);
    User user = Mockito.mock(User.class);

    InvokeContainerWizardWebService service = Mockito.mock(InvokeContainerWizardWebService.class);
    Mockito.doCallRealMethod().when(service).retainUserInput(searchService, user, wizard, args, -1);
    Mockito.doCallRealMethod().when(service).throwIfInvalidJobCode(user, searchService, jobCode);
    Mockito.when(service.searchIfJobCodeIsAlreadyAssigned(user, searchService, jobCode))
        .thenReturn(containers);

    try {
      service.retainUserInput(searchService, user, wizard, args, -1);
    } catch (RSuiteException e) {
      assertThat("Error message should report is already assigned to a product.", e.getMessage(),
          containsString("is already assigned to a product"));
      return;
    }

    fail("No or other exception is thrown!");
  }

  /**
   * Verify the future MO list is set in container wizard.
   */
  @Test
  public void setFutureMos() throws RSuiteException {

    String expectedTemplateMoId = "12345";

    ContainerWizard wizard = new ContainerWizard();
    CallArgument arg1 = new CallArgument(PARAM_NAME_XML_TEMPLATE_MO_ID, expectedTemplateMoId);
    CallArgument arg2 = new CallArgument(PARAM_NAME_NEXT_SUB_PAGE_IDX, "1");
    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg1);
    argList.add(arg2);
    CallArgumentList args = new CallArgumentList(argList);
    SearchService searchService = Mockito.mock(SearchService.class);
    User user = Mockito.mock(User.class);

    InvokeContainerWizardWebService service = new InvokeContainerWizardWebService();
    service.retainUserInput(searchService, user, wizard, args, 0);

    List<FutureManagedObject> futureMoList = wizard.getFutureManagedObjectListByKey("0");
    assertEquals(futureMoList.get(0).getTemplateMoId(), expectedTemplateMoId);

  }

  @Test
  public void stillDevelopingThisTest() throws RSuiteException {
    // Set up the web service's execution context.
    User user = Mockito.mock(User.class);
    Session session = new Session("Unit Test", user);
    AuthorizationService authService = Mockito.mock(AuthorizationService.class);
    Mockito.when(authService.getSystemUser()).thenReturn(user);
    RemoteApiExecutionContext context = Mockito.mock(RemoteApiExecutionContext.class);
    Mockito.when(context.getSession()).thenReturn(session);
    Mockito.when(context.getAuthorizationService()).thenReturn(authService);

    // Set up the web service's arguments.
    List<CallArgument> argList = new ArrayList<CallArgument>();
    // CallArgument arg = new CallArgument("MyNumber", "123456");
    // argList.add(arg);

    CallArgumentList args = new CallArgumentList(argList);

    InvokeContainerWizardWebService webService =
        Mockito.mock(InvokeContainerWizardWebService.class);
    Mockito.doCallRealMethod().when(webService).execute(context, args);

    RemoteApiResult result = webService.execute(context, args);
    Assert.assertNotNull("Web service return is null", result);
    Assert.assertThat(new StringBuilder("Received instance of ").append(result.getClass().getName())
        .append(" when ").append(MessageDialogResult.class.getName()).append(" was expected.")
        .toString(), result, new IsInstanceOf(MessageDialogResult.class));

  }
}
