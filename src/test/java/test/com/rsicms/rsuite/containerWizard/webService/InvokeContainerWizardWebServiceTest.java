package test.com.rsicms.rsuite.containerWizard.webService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.service.SearchService;
import com.rsicms.rsuite.containerWizard.ContainerWizard;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
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
    service.retainUserInput(searchService, user, wizard, args);

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
    Mockito.doCallRealMethod().when(service).retainUserInput(searchService, user, wizard, args);
    Mockito.doCallRealMethod().when(service).throwIfInvalidJobCode(user, searchService, jobCode);
    Mockito.when(service.searchIfJobCodeIsAlreadyAssigned(user, searchService, jobCode))
        .thenReturn(containers);

    try {
      service.retainUserInput(searchService, user, wizard, args);
      fail("retainUserInput didn't throw RSuiteException when it's expected to.");
    } catch (RSuiteException e) {

    }

  }

}
