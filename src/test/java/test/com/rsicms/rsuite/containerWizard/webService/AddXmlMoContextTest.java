package test.com.rsicms.rsuite.containerWizard.webService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.browse.BrowseInfo;
import com.reallysi.rsuite.api.content.ContentObjectPath;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.service.AuthorizationService;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.SecurityService;
import com.rsicms.rsuite.containerWizard.AddXmlMoContext;
import com.rsicms.rsuite.containerWizard.ContainerWizardConfUtils;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.utils.container.ContainerUtils;
import com.rsicms.rsuite.utils.mo.MOUtils;

import test.helpers.ContainerWizardTestUtils;

/**
 * Unit tests to verify AddXmlMoContext creation.
 */
public class AddXmlMoContextTest
    implements ContainerWizardConstants {

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

  /**
   * <p>
   * Add a section of discovery.
   * <p>
   * It is a Sub mo.
   */
  @Before
  public void setUp()
      throws SAXException, IOException, ParserConfigurationException, RSuiteException,
      JAXBException {

    // FIXME: Doesn't seem thread safe, especially if a unit test can modify these variables.

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

    moService = Mockito.mock(ManagedObjectService.class);
    Mockito.when(moService.getManagedObject(user, existingMoId)).thenReturn(existingMo);
    Mockito.when(moService.getManagedObject(user, parentMoId)).thenReturn(parentMo);
    // sub mo
    Mockito.when(moService.getRootManagedObjectId(user, existingMoId)).thenReturn(parentMoId);
    Mockito.when(moService.getParentManagedObject(user, existingMo)).thenReturn(parentMo);
    Mockito.when(moService.getChildManagedObjects(Mockito.any(User.class), Mockito.anyString(),
        Mockito.anyInt(), Mockito.anyInt())).thenReturn(browseInfo);

    context = Mockito.mock(ExecutionContext.class);
    Mockito.when(context.getManagedObjectService()).thenReturn(moService);
    Mockito.when(context.getAuthorizationService()).thenReturn(auService);

  }

  /**
   * Verify AddXmlMoContext is created successfully by admin user.
   */
  @Test
  public void adminCreateAddXmlMoContext() throws RSuiteException {

    // user is admin, thus have right to add section
    Mockito.when(auService.isAdministrator(user)).thenReturn(true);

    CallArgument arg1 = new CallArgument(PARAM_NAME_RSUITE_ID, existingMoId);
    CallArgument arg2 = new CallArgument(PARAM_NAME_INSERTION_POSITION, "AFTER");
    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg1);
    argList.add(arg2);
    CallArgumentList args = new CallArgumentList(argList);

    MOUtils moUtils = Mockito.mock(MOUtils.class);
    Mockito.when(moUtils.isSubMo(any(ManagedObjectService.class), any(User.class), any(
        ManagedObject.class))).thenReturn(true);

    ContainerUtils containerUtils = Mockito.mock(ContainerUtils.class);

    AddXmlMoContext addXmlMoContext = new AddXmlMoContext(context, user, conf, confUtils, args,
        moUtils, containerUtils);

    // based on sample configuration, section index for discovery is 3
    assertEquals(3, addXmlMoContext.getXmlMoConfIdx());

  }

  /**
   * Verify AddXmlMoContext is created successfully by user with edit permission.
   */
  @Test
  public void editUserCreateAddXmlMoContext() throws RSuiteException {

    // user has edit permission, thus have right to add section
    SecurityService secService = Mockito.mock(SecurityService.class);
    Mockito.when(secService.hasEditPermission(user, parentMoId)).thenReturn(true);
    Mockito.when(context.getSecurityService()).thenReturn(secService);

    CallArgument arg1 = new CallArgument(PARAM_NAME_RSUITE_ID, existingMoId);
    CallArgument arg2 = new CallArgument(PARAM_NAME_INSERTION_POSITION, "AFTER");
    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg1);
    argList.add(arg2);
    CallArgumentList args = new CallArgumentList(argList);

    MOUtils moUtils = Mockito.mock(MOUtils.class);
    Mockito.when(moUtils.isSubMo(any(ManagedObjectService.class), any(User.class), any(
        ManagedObject.class))).thenReturn(true);

    ContainerUtils containerUtils = Mockito.mock(ContainerUtils.class);

    AddXmlMoContext addXmlMoContext = new AddXmlMoContext(context, user, conf, confUtils, args,
        moUtils, containerUtils);

    // based on sample configuration, section index for discovery is 3
    assertEquals(3, addXmlMoContext.getXmlMoConfIdx());

  }

  /**
   * Verify AddXmlMoContext is not created and thrown exception by other user.
   */
  @Test(expected = RSuiteException.class)
  public void otherUserFailToCreateAddXmlMoContext() throws RSuiteException {

    // user does not have edit permission, thus does not have right to add section
    SecurityService secService = Mockito.mock(SecurityService.class);
    Mockito.when(secService.hasEditPermission(user, parentMoId)).thenReturn(false);
    Mockito.when(context.getSecurityService()).thenReturn(secService);

    CallArgument arg1 = new CallArgument(PARAM_NAME_RSUITE_ID, existingMoId);
    CallArgument arg2 = new CallArgument(PARAM_NAME_INSERTION_POSITION, "AFTER");
    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg1);
    argList.add(arg2);
    CallArgumentList args = new CallArgumentList(argList);

    MOUtils moUtils = Mockito.mock(MOUtils.class);
    Mockito.when(moUtils.isSubMo(any(ManagedObjectService.class), any(User.class), any(
        ManagedObject.class))).thenReturn(true);

    ContainerUtils containerUtils = Mockito.mock(ContainerUtils.class);

    new AddXmlMoContext(context, user, conf, confUtils, args, moUtils, containerUtils);

  }

  /**
   * Make sure the add context instructs the wizard to create top-level MOs.
   * 
   * @throws RSuiteException
   */
  @Test
  public void onlyCreateTopLevelMos() throws RSuiteException {

    // ExecutionContext context = Mockito.mock(ExecutionContext.class);
    // Mockito.when(context.getManagedObjectService()).thenReturn(Mockito.mock(
    // ManagedObjectService.class));
    // Mockito.when(context.getAuthorizationService()).thenReturn(Mockito.mock(
    // AuthorizationService.class));

    // valid user
    SecurityService securityService = Mockito.mock(SecurityService.class);
    Mockito.when(securityService.hasEditPermission(any(User.class), any(String.class))).thenReturn(
        true);
    Mockito.when(context.getSecurityService()).thenReturn(securityService);

    CallArgumentList args = Mockito.mock(CallArgumentList.class);
    Mockito.when(args.getFirstString(PARAM_NAME_INSERTION_POSITION)).thenReturn("AFTER");
    Mockito.when(args.getFirstString(PARAM_NAME_RSUITE_ID)).thenReturn(existingMoId);

    MOUtils moUtils = Mockito.mock(MOUtils.class);
    Mockito.when(moUtils.isSubMo(any(ManagedObjectService.class), any(User.class), any(
        ManagedObject.class))).thenReturn(false);

    String expectedContainerId = "139487";
    ContentAssemblyNodeContainer container = Mockito.mock(ContentAssemblyNodeContainer.class);
    Mockito.when(container.getId()).thenReturn(expectedContainerId);

    // List<? extends ContentAssemblyItem> children = new ArrayList<ContentAssemblyItem>();
    // Mockito.when(container.getChildrenObjects()).thenReturn(children);

    ContainerUtils containerUtils = Mockito.mock(ContainerUtils.class);
    Mockito.when(containerUtils.getContentAssemblyNodeContainer(any(ContentAssemblyService.class),
        any(User.class), any(ContentObjectPath.class), any(String.class))).thenReturn(container);

    AddXmlMoContext addXmlMoContext = new AddXmlMoContext(context, user, conf, confUtils, args,
        moUtils, containerUtils);
    assertTrue("Doesn't instruct to create top-level MOs.", addXmlMoContext
        .shouldCreateAsTopLevelMos());
    assertNotEquals("Instructed to create top-level and sub MOs.", addXmlMoContext
        .shouldCreateAsTopLevelMos(), addXmlMoContext.shouldCreateAsSubMos());


  }
}
