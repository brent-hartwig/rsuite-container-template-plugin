package test.com.rsicms.rsuite.containerWizard.webService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.security.RoleManager;
import com.reallysi.rsuite.api.xml.XPathEvaluator;
import com.reallysi.rsuite.service.AuthorizationService;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.IDGenerator;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.SecurityService;
import com.rsicms.rsuite.containerWizard.AclMap;
import com.rsicms.rsuite.containerWizard.ContainerWizard;
import com.rsicms.rsuite.containerWizard.FutureManagedObject;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerConf;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.PrimaryContainer;
import com.rsicms.rsuite.containerWizard.jaxb.XmlMoConf;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

public class CreatePrimaryContainerTest {

  /**
   * Verify a CA node is added.
   */
  @Test
  public void addContainer() throws RSuiteException {

    String expectedId = "11111";

    ContentAssemblyService caService = Mockito.mock(ContentAssemblyService.class);
    User user = Mockito.mock(User.class);
    ContentAssembly primaryContainer = Mockito.mock(ContentAssembly.class);
    ContainerConf conf = Mockito.mock(ContainerConf.class);
    AclMap aclMap = Mockito.mock(AclMap.class);
    ContentAssemblyNodeContainer caNode = Mockito.mock(ContentAssemblyNodeContainer.class);

    Mockito.when(caNode.getId()).thenReturn(expectedId);
    Mockito.when(conf.getName()).thenReturn("Journal");
    Mockito.when(primaryContainer.getId()).thenReturn(expectedId);
    Mockito
        .when(caService.createCANode(Mockito.any(User.class), Mockito.anyString(),
            Mockito.anyString(), Mockito.any(ContentAssemblyCreateOptions.class)))
        .thenReturn(caNode);

    InvokeContainerWizardWebService invokeService = new InvokeContainerWizardWebService();
    String resultId =
        invokeService.addContainer(caService, user, primaryContainer, conf, aclMap, expectedId);

    assertEquals(resultId, expectedId);

  }

  /**
   * Verify a list of managed objects are added.
   */
  @Test
  public void addManagedObjects() throws RSuiteException, IOException, TransformerException {

    int expectedSize = 1;
    String expectedId = "11111";

    ManagedObject templateMo = Mockito.mock(ManagedObject.class);

    IDGenerator idGenerator = Mockito.mock(IDGenerator.class);
    Mockito.when(idGenerator.allocateId()).thenReturn(expectedId);

    ManagedObjectService moService = Mockito.mock(ManagedObjectService.class);
    Mockito.when(moService.getManagedObject(Mockito.any(User.class), Mockito.anyString()))
        .thenReturn(templateMo);

    ContentAssemblyService caService = Mockito.mock(ContentAssemblyService.class);
    Mockito.when(caService.attach(Mockito.any(User.class), Mockito.anyString(), Mockito.anyString(),
        Mockito.any(ObjectAttachOptions.class))).thenReturn(templateMo);

    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    Mockito.when(context.getManagedObjectService()).thenReturn(moService);
    Mockito.when(context.getIDGenerator()).thenReturn(idGenerator);
    Mockito.when(context.getContentAssemblyService()).thenReturn(caService);

    User user = Mockito.mock(User.class);
    XPathEvaluator eval = Mockito.mock(XPathEvaluator.class);
    Node node = Mockito.mock(Node.class);
    Node[] nodeArr = {node};
    NamedNodeMap atts = Mockito.mock(NamedNodeMap.class);
    Mockito.when(node.getAttributes()).thenReturn(atts);
    Mockito.when(eval.executeXPathToNodeArray(Mockito.anyString(), Mockito.any(Object.class)))
        .thenReturn(nodeArr);

    ContentAssembly primaryContainer = Mockito.mock(ContentAssembly.class);
    XmlMoConf conf = Mockito.mock(XmlMoConf.class);
    FutureManagedObject fmo = Mockito.mock(FutureManagedObject.class);

    List<FutureManagedObject> fmoList = new ArrayList<FutureManagedObject>();
    fmoList.add(fmo);
    AclMap aclMap = Mockito.mock(AclMap.class);

    InvokeContainerWizardWebService invokeService =
        Mockito.mock(InvokeContainerWizardWebService.class);
    Mockito
        .when(invokeService.loadMo(Mockito.any(Element.class), Mockito.any(ExecutionContext.class),
            Mockito.any(User.class), Mockito.anyString(), Mockito.any(ManagedObjectAdvisor.class)))
        .thenReturn(templateMo);
    Mockito.when(invokeService.getObjectSource(Mockito.any(Element.class),
        Mockito.any(ExecutionContext.class), Mockito.anyString())).thenReturn(null);
    Mockito.doCallRealMethod().when(invokeService).addManagedObjects(context, user, eval,
        primaryContainer, conf, fmoList, aclMap, expectedId);

    List<ManagedObject> resultMoList = invokeService.addManagedObjects(context, user, eval,
        primaryContainer, conf, fmoList, aclMap, expectedId);

    assertEquals(resultMoList.size(), expectedSize);

  }

  /**
   * Verify a primary container is created.
   */
  @Test
  public void createPrimaryContainer() throws RSuiteException, IOException, TransformerException {

    String parentId = "12345";

    PrimaryContainer pcConf = Mockito.mock(PrimaryContainer.class);
    Mockito.when(pcConf.getDefaultAclId()).thenReturn("11111");

    XPathEvaluator eval = Mockito.mock(XPathEvaluator.class);

    Session session = Mockito.mock(Session.class);
    ContainerWizardConf conf = Mockito.mock(ContainerWizardConf.class);
    Mockito.when(conf.getPrimaryContainer()).thenReturn(pcConf);

    ContainerWizard wizard = Mockito.mock(ContainerWizard.class);
    Mockito.when(wizard.getContainerName()).thenReturn("Journal");

    ContentAssembly primaryContainer = Mockito.mock(ContentAssembly.class);

    SecurityService securityService = Mockito.mock(SecurityService.class);

    RoleManager roleManager = Mockito.mock(RoleManager.class);
    AuthorizationService authorizationService = Mockito.mock(AuthorizationService.class);
    Mockito.when(authorizationService.getRoleManager()).thenReturn(roleManager);

    ContentAssemblyService caService = Mockito.mock(ContentAssemblyService.class);
    Mockito
        .when(caService.createContentAssembly(Mockito.any(User.class), Mockito.anyString(),
            Mockito.anyString(), Mockito.any(ContentAssemblyCreateOptions.class)))
        .thenReturn(primaryContainer);

    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    Mockito.when(context.getContentAssemblyService()).thenReturn(caService);
    Mockito.when(context.getAuthorizationService()).thenReturn(authorizationService);
    Mockito.when(context.getSecurityService()).thenReturn(securityService);

    InvokeContainerWizardWebService invokeService =
        Mockito.mock(InvokeContainerWizardWebService.class);
    Mockito.when(invokeService.getXPathEvaluator(context)).thenReturn(eval);
    Mockito.doCallRealMethod().when(invokeService).createPrimaryContainer(context, session, conf,
        wizard, parentId);

    ContentAssembly resultContentAssembly =
        invokeService.createPrimaryContainer(context, session, conf, wizard, parentId);

    assertNotNull("Created primary container is null.", resultContentAssembly);

  }

}
