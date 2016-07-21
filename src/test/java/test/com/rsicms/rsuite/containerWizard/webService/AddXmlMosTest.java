package test.com.rsicms.rsuite.containerWizard.webService;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.security.ACL;
import com.reallysi.rsuite.api.xml.XPathEvaluator;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.IDGenerator;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.XmlApiManager;
import com.rsicms.rsuite.containerWizard.AddXmlMoContext;
import com.rsicms.rsuite.containerWizard.AddXmlMoResult;
import com.rsicms.rsuite.containerWizard.ContainerWizard;
import com.rsicms.rsuite.containerWizard.FutureManagedObject;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

import test.helpers.ContainerWizardTestUtils;


/**
 * Unit tests to verify addXmlMos() method.
 *
 */
public class AddXmlMosTest {

  /**
   * Verify a sub mo is added successfully.
   */
  @Test
  public void addAnXmlMo() throws SAXException, IOException, ParserConfigurationException,
      RSuiteException, JAXBException, TransformerException {

    ContainerWizardConf conf = new ContainerWizardTestUtils().newContainerWizardConfForTests();

    String containerId = "22222";
    String existingMoId = "12345";

    User user = Mockito.mock(User.class);

    // to add one sub mo
    int expectedNumberOfMos = 1;
    FutureManagedObject fmo = Mockito.mock(FutureManagedObject.class);
    List<FutureManagedObject> fmoList = new ArrayList<FutureManagedObject>();
    fmoList.add(fmo);

    // to mock ACL to be used in addXmlMos() method
    ACL acl = Mockito.mock(ACL.class);
    ManagedObject templateMo = Mockito.mock(ManagedObject.class);
    Mockito.when(templateMo.getACL()).thenReturn(acl);

    ContentAssemblyNodeContainer nodeContainer = Mockito.mock(ContentAssemblyNodeContainer.class);

    IDGenerator idGenerator = Mockito.mock(IDGenerator.class);
    Mockito.when(idGenerator.allocateId()).thenReturn("11111");

    ManagedObjectService moService = Mockito.mock(ManagedObjectService.class);
    Mockito.when(moService.getManagedObject(Mockito.any(User.class), Mockito.anyString()))
        .thenReturn(templateMo);
    // mo needs to be checked out before inserting in MOUtils
    Mockito.when(moService.isCheckedOut(Mockito.any(User.class), Mockito.anyString()))
        .thenReturn(true);

    ContentAssemblyService caService = Mockito.mock(ContentAssemblyService.class);
    Mockito.when(caService.attach(Mockito.any(User.class), Mockito.anyString(), Mockito.anyString(),
        Mockito.any(ObjectAttachOptions.class))).thenReturn(templateMo);
    Mockito.when(caService.getContentAssemblyNodeContainer(user, containerId))
        .thenReturn(nodeContainer);

    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    Mockito.when(context.getManagedObjectService()).thenReturn(moService);
    Mockito.when(context.getIDGenerator()).thenReturn(idGenerator);
    Mockito.when(context.getContentAssemblyService()).thenReturn(caService);

    // to mock XmlApiManager and Transformer to be used in MOUtils.addNodesIntoExistingMo() method
    Transformer transformer = Mockito.mock(Transformer.class);
    // mocking XmlApiManager necessitates saxon9-s9api.jar
    XmlApiManager xmlManager = Mockito.mock(XmlApiManager.class);
    Mockito.when(xmlManager.getTransformer((File) null)).thenReturn(transformer);
    Mockito.when(context.getXmlApiManager()).thenReturn(xmlManager);

    // to mock XPathEvaluator and elements and associated attributes to be
    // used in addManagedObjects() method, which addXmlMos() uses
    XPathEvaluator eval = Mockito.mock(XPathEvaluator.class);
    Element elem = Mockito.mock(Element.class);
    Element[] elemArr = {elem};
    NamedNodeMap atts = Mockito.mock(NamedNodeMap.class);
    Mockito.when(elem.getAttributes()).thenReturn(atts);
    Mockito.when(eval.executeXPathToNodeArray(Mockito.anyString(), Mockito.any(Object.class)))
        .thenReturn(elemArr);

    // to mock the document tree of nodes for mo insertion in MOUtils
    Document doc = Mockito.mock(Document.class);
    Node adjacentNode = Mockito.mock(Node.class);
    Node parentNode = Mockito.mock(Node.class);
    Mockito.when(parentNode.getOwnerDocument()).thenReturn(doc);
    Mockito.when(adjacentNode.getParentNode()).thenReturn(parentNode);
    Mockito.when(eval.executeXPathToNode(Mockito.anyString(), Mockito.any(Object.class)))
        .thenReturn(adjacentNode);

    // to mock AddXmlMoContext to be used in addXmlMos() method
    AddXmlMoContext addContext = Mockito.mock(AddXmlMoContext.class);
    Mockito.when(addContext.shouldCreateAsTopLevelMos()).thenReturn(false);
    Mockito.when(addContext.getContainerId()).thenReturn(containerId);
    Mockito.when(addContext.getParentMoId()).thenReturn(containerId);
    Mockito.when(addContext.getExistingMoId()).thenReturn(existingMoId);
    Mockito.when(addContext.getInsertBeforeId()).thenReturn(existingMoId);

    ContainerWizard wizard = Mockito.mock(ContainerWizard.class);
    Mockito.when(wizard.getAddXmlMoContext()).thenReturn(addContext);
    Mockito.when(wizard.getFirstAndOnlyFutureManagedObjectList()).thenReturn(fmoList);

    // to mock the web service partially, some mock methods and some real methods
    InvokeContainerWizardWebService service = Mockito.mock(InvokeContainerWizardWebService.class);
    Mockito
        .when(service.loadMo(Mockito.any(Element.class), Mockito.any(ExecutionContext.class),
            Mockito.any(User.class), Mockito.anyString(), Mockito.any(ManagedObjectAdvisor.class)))
        .thenReturn(templateMo);
    Mockito.when(service.getObjectSource(Mockito.any(Element.class),
        Mockito.any(ExecutionContext.class), Mockito.anyString())).thenReturn(null);
    Mockito.when(service.getXPathEvaluator(context)).thenReturn(eval);
    Mockito.doCallRealMethod().when(service).addManagedObjects(context, user, eval, false,
        containerId, fmoList, acl, null, false);
    Mockito.doCallRealMethod().when(service).addXmlMos(context, user, conf, wizard);

    int actualNumberOfMos = 0;

    AddXmlMoResult addXmlMoResult = service.addXmlMos(context, user, conf, wizard);

    actualNumberOfMos += addXmlMoResult.getCount();

    // Number of MOs added is 1
    assertEquals(expectedNumberOfMos, actualNumberOfMos);

  }

}
