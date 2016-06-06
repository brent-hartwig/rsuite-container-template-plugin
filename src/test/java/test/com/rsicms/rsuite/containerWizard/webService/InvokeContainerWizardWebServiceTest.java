package test.com.rsicms.rsuite.containerWizard.webService;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

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

}
