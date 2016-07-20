package test.com.rsicms.rsuite.containerWizard.webService;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.browse.BrowseInfo;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.service.AuthorizationService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.rsicms.rsuite.containerWizard.AddXmlMoContext;
import com.rsicms.rsuite.containerWizard.AddXmlMoResult;
import com.rsicms.rsuite.containerWizard.ContainerWizard;
import com.rsicms.rsuite.containerWizard.ContainerWizardConfUtils;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

import test.helpers.ContainerWizardTestUtils;

/**
 * Unit tests to verify adding sections
 *
 */
public class AddXmlMosTest implements ContainerWizardConstants {

  private static boolean setUpIsDone = false;
  private static String existingMoId = "12345";
  private static String parentMoId = "11111";
  ContainerWizardConf conf = null;
  ExecutionContext context = null;
  AuthorizationService auService = null;
  ManagedObjectService moService = null;
  ManagedObject existingMo = null;
  ManagedObject parentMo = null;
  BrowseInfo browseInfo = null;
  ContainerWizardConfUtils confUtils = null;
  User user = null;

  @Before
  public void setUp() throws SAXException, IOException, ParserConfigurationException,
      RSuiteException, JAXBException {

    if (setUpIsDone) {
      return;
    }

    conf = new ContainerWizardTestUtils().newContainerWizardConfForTests();
    confUtils = new ContainerWizardConfUtils();

    user = Mockito.mock(User.class);
    Mockito.when(user.getUserId()).thenReturn("gao");

    existingMo = Mockito.mock(ManagedObject.class);
    // to add discovery section
    Mockito.when(existingMo.getLocalName()).thenReturn("discovery");
    Mockito.when(existingMo.getId()).thenReturn(existingMoId);
    // allow to add after
    Mockito.when(existingMo.isLastChild()).thenReturn(false);

    parentMo = Mockito.mock(ManagedObject.class);
    Mockito.when(parentMo.getId()).thenReturn(parentMoId);
    Mockito.when(parentMo.getCheckedOutUser()).thenReturn("jerry.aic");

    ManagedObject mo1 = Mockito.mock(ManagedObject.class);
    Mockito.when(mo1.getId()).thenReturn(existingMoId);
    // to add discovery section
    Mockito.when(mo1.getLocalName()).thenReturn("discovery 1");
    ManagedObject mo2 = Mockito.mock(ManagedObject.class);
    Mockito.when(mo2.getId()).thenReturn("12347");
    Mockito.when(mo2.getLocalName()).thenReturn("discovery 2");
    List<ManagedObject> moList = new ArrayList<ManagedObject>();
    moList.add(mo1);
    moList.add(mo2);

    browseInfo = Mockito.mock(BrowseInfo.class);
    Mockito.when(browseInfo.getTotal()).thenReturn(2L);
    Mockito.when(browseInfo.getManagedObjects()).thenReturn(moList);

    auService = Mockito.mock(AuthorizationService.class);
    // user is admin, thus have right to add section
    Mockito.when(auService.isAdministrator(user)).thenReturn(true);

    moService = Mockito.mock(ManagedObjectService.class);
    Mockito.when(moService.getManagedObject(user, existingMoId)).thenReturn(existingMo);
    // sub mo
    Mockito.when(moService.getRootManagedObjectId(user, existingMoId)).thenReturn(parentMoId);
    Mockito.when(moService.getParentManagedObject(user, existingMo)).thenReturn(parentMo);
    Mockito.when(moService.getChildManagedObjects(Mockito.any(User.class), Mockito.anyString(),
        Mockito.anyInt(), Mockito.anyInt())).thenReturn(browseInfo);

    context = Mockito.mock(ExecutionContext.class);
    Mockito.when(context.getManagedObjectService()).thenReturn(moService);
    Mockito.when(context.getAuthorizationService()).thenReturn(auService);

    setUpIsDone = true;

  }

  /**
   * Verify AddXmlMoContext is created successfully and XmlMoConfIdx is correct based on sample
   * configuration for the following conditions: use is admin, thus can add sections section is
   * discovery use sub mo
   */
  @Test
  public void createAddXmlMoContext() throws RSuiteException {

    CallArgument arg1 = new CallArgument(PARAM_NAME_RSUITE_ID, existingMoId);
    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg1);
    CallArgumentList args = new CallArgumentList(argList);

    AddXmlMoContext addXmlMoContext = new AddXmlMoContext(context, user, conf, confUtils, args);

    // based on sample configuration, section index for discovery is 3
    int expectedXmlMoConfIdx = 3;
    assertEquals(addXmlMoContext.getXmlMoConfIdx(), expectedXmlMoConfIdx);

  }

  /**
   * TODO
   */
  @Test
  public void addXmlMos() throws SAXException, IOException, ParserConfigurationException,
      RSuiteException, JAXBException, TransformerException {

    ContainerWizard wizard = Mockito.mock(ContainerWizard.class);

    InvokeContainerWizardWebService service = Mockito.mock(InvokeContainerWizardWebService.class);
    // Mockito.doCallRealMethod().when(service).addXmlMos(context, user, conf, wizard);

    @SuppressWarnings("unused")
    AddXmlMoResult result = service.addXmlMos(context, user, conf, wizard);

  }

}
