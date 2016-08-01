package test.com.rsicms.rsuite.containerWizard.webService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.security.ACE;
import com.reallysi.rsuite.api.security.ACL;
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
import com.rsicms.rsuite.containerWizard.jaxb.Ace;
import com.rsicms.rsuite.containerWizard.jaxb.Acl;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerConf;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.PrimaryContainer;
import com.rsicms.rsuite.containerWizard.jaxb.XmlMoConf;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

import test.com.rsicms.rsuite.containerWizard.AclMapTests;
import test.helpers.ContainerWizardTestUtils;

/**
 * This class contains unit tests that verify various code sections of createPrimaryContainer()
 * method.
 *
 */
public class CreatePrimaryContainerTest {

  /**
   * This unit test verifies that meta data from user input and wizard configuration get passed to
   * the primary container create options (to be used by ContentAssemblyService)
   */
  @Test
  public void verifyMetaData()
      throws SAXException, IOException, ParserConfigurationException, RSuiteException,
      JAXBException {

    /*
     * Sample Container Wizard Configuration has the following meta data: <metadata-conf>
     * <name-value-pair name="Hello" value="World"/> <name-value-pair name="Status" value="New"/>
     * </metadata-conf>
     */
    ContainerWizardConf conf = new ContainerWizardTestUtils().newContainerWizardConfForTests();
    PrimaryContainer pcConf = conf.getPrimaryContainer();

    // user inputs are kept in the wizard of the wizard form
    ContainerWizard wizard = new ContainerWizard();
    wizard.setContainerName("Sample Audit Report");
    // these meta data are from user
    wizard.addContainerMetadata("JobCode", "111111");
    wizard.addContainerMetadata("ProductNumber", "1");
    wizard.addContainerMetadata("ProductTypeId", "StandardAuditReport");

    // Thus, these names should be in the mete data list of CA create options
    Set<String> nameSet = new HashSet<String>();
    nameSet.add("Hello");
    nameSet.add("Status");
    nameSet.add("JobCode");
    nameSet.add("ProductNumber");
    nameSet.add("ProductTypeId");

    // Thus, these values should be in the mete data list of CA create options
    Set<String> valueSet = new HashSet<String>();
    valueSet.add("World");
    valueSet.add("New");
    valueSet.add("111111");
    valueSet.add("1");
    valueSet.add("StandardAuditReport");

    InvokeContainerWizardWebService service = new InvokeContainerWizardWebService();
    ContentAssemblyCreateOptions resultOptions = service.getCaCreateOptions(pcConf, wizard);

    for (MetaDataItem item : resultOptions.getMetaDataItems()) {
      // names are indeed in options
      assertEquals(nameSet.contains(item.getName()), true);

      // values are indeed in options
      assertEquals(valueSet.contains(item.getValue()), true);
    }

  }

  /**
   * This unit test only verifies the primary container is created and ID can be retrieved.
   */
  @Test
  public void createPrimaryContainer() throws RSuiteException, IOException, TransformerException {

    String parentId = "12345";
    String expectedId = "22222";

    PrimaryContainer pcConf = Mockito.mock(PrimaryContainer.class);
    Mockito.when(pcConf.getDefaultAclId()).thenReturn("11111");

    XPathEvaluator eval = Mockito.mock(XPathEvaluator.class);

    Session session = Mockito.mock(Session.class);
    ContainerWizardConf conf = Mockito.mock(ContainerWizardConf.class);
    Mockito.when(conf.getPrimaryContainer()).thenReturn(pcConf);

    ContainerWizard wizard = Mockito.mock(ContainerWizard.class);
    Mockito.when(wizard.getContainerName()).thenReturn("Sample Audit Report");

    ContentAssembly primaryContainer = Mockito.mock(ContentAssembly.class);
    Mockito.when(primaryContainer.getId()).thenReturn(expectedId);

    SecurityService securityService = Mockito.mock(SecurityService.class);

    RoleManager roleManager = Mockito.mock(RoleManager.class);
    AuthorizationService authorizationService = Mockito.mock(AuthorizationService.class);
    Mockito.when(authorizationService.getRoleManager()).thenReturn(roleManager);

    ContentAssemblyService caService = Mockito.mock(ContentAssemblyService.class);
    Mockito.when(caService.createContentAssembly(Mockito.any(User.class), Mockito.anyString(),
        Mockito.anyString(), Mockito.any(ContentAssemblyCreateOptions.class))).thenReturn(
            primaryContainer);

    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    Mockito.when(context.getContentAssemblyService()).thenReturn(caService);
    Mockito.when(context.getAuthorizationService()).thenReturn(authorizationService);
    Mockito.when(context.getSecurityService()).thenReturn(securityService);

    InvokeContainerWizardWebService invokeService = Mockito.mock(
        InvokeContainerWizardWebService.class);
    Mockito.when(invokeService.getXPathEvaluator()).thenReturn(eval);
    Mockito.doCallRealMethod().when(invokeService).createPrimaryContainer(context, session, conf,
        wizard, parentId);

    ContentAssemblyNodeContainer resultPrimaryContainer = invokeService.createPrimaryContainer(
        context, session, conf, wizard, parentId);

    // primary container is created
    assertNotNull("Created primary container is null.", resultPrimaryContainer);

    // primary container ID is expected as 22222
    assertEquals(expectedId, resultPrimaryContainer.getId());

  }

  /**
   * After primary container being created and ID being able to be retrieved, product specific roles
   * could be created since container ID is available. This unit test verifies that AclMap creates
   * the correct product specific roles, which will be used by SecurityService to set ACL for the
   * product (container and associated contents).
   */
  @Test
  public void verifyCreateUndefinedContainerRole()
      throws SAXException, IOException, ParserConfigurationException, RSuiteException,
      JAXBException {

    // ID of new product container can be retrieved.
    String newContainerId = "22222";

    ContainerWizardConf conf = new ContainerWizardTestUtils().newContainerWizardConfForTests();
    AclMapTests aclMapTests = new AclMapTests();

    SecurityService securityService = Mockito.mock(SecurityService.class);

    int index = 0;
    ACE[][] builtACEs = new ACE[conf.getAcls().getAcl().size()][];
    ACL[] acls = new ACL[conf.getAcls().getAcl().size()];

    for (Acl aAcl : conf.getAcls().getAcl()) {
      builtACEs[index] = new ACE[conf.getAcls().getAcl().size()];
      Ace aAce = null;
      for (int i = 0; i < aAcl.getAce().size(); i++) {
        aAce = aAcl.getAce().get(i);

        // Thus, product specific roles will be 22222_SMEs, 22222_Reviewers, 22222_Managers
        // SMEs, Reviewers and Managers are configured in the sample wizard configuration.
        String roleName = AclMap.getContainerRoleName(newContainerId, aAce.getProjectRole());

        ACE rACE = aclMapTests.new ACEImpl(aclMapTests.new RoleImpl(roleName));

        Mockito.when(securityService.constructACE(roleName, aAce.getContentPermissions().replace(
            StringUtils.SPACE, StringUtils.EMPTY))).thenReturn(rACE);

        builtACEs[index][i] = rACE;
      }

      acls[index] = aclMapTests.new ACLImpl(builtACEs[index]);

      index++;
    }

    Mockito.when(securityService.constructACL(Mockito.any(ACE[].class))).thenReturn(acls[0],
        acls[1], acls[2]);

    AclMap aclMap = new AclMap(securityService, conf, newContainerId);

    User user = Mockito.mock(User.class);
    RoleManager roleManager = Mockito.mock(RoleManager.class);
    Mockito.doNothing().when(roleManager).createRole(Mockito.any(User.class), Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString());

    List<String> resultRoleNamesList = aclMap.createUndefinedContainerRoles(user, roleManager);
    Set<String> resultRoleNameSet = new HashSet<String>(resultRoleNamesList);

    Set<String> expectedRoleNameSet = new HashSet<String>();
    expectedRoleNameSet.add("22222_SMEs");
    expectedRoleNameSet.add("22222_Reviewers");
    expectedRoleNameSet.add("22222_Managers");

    // Verify that result role names are 22222_SMEs, 22222_Reviewers, 22222_Managers
    assertEquals(resultRoleNameSet.contains(expectedRoleNameSet), expectedRoleNameSet.contains(
        resultRoleNameSet));

  }

  /**
   * Verify a list of ca nodes are added as content.
   */
  @Test
  public void addContainer()
      throws RSuiteException, SAXException, IOException, ParserConfigurationException,
      JAXBException {

    String newContainerId = "22222";

    // There are two <container-conf> elements configured in sample wizard configuration.
    int expectedNumberOfContainers = 2;

    ContainerWizardConf conf = new ContainerWizardTestUtils().newContainerWizardConfForTests();
    String defaultAclId = conf.getPrimaryContainer().getDefaultAclId();

    ContentAssemblyService caService = Mockito.mock(ContentAssemblyService.class);
    User user = Mockito.mock(User.class);
    ContentAssembly primaryContainer = Mockito.mock(ContentAssembly.class);

    AclMap aclMap = Mockito.mock(AclMap.class);
    ContentAssembly caNode = Mockito.mock(ContentAssembly.class);

    Mockito.when(caNode.getId()).thenReturn(defaultAclId);

    Mockito.when(primaryContainer.getId()).thenReturn(newContainerId);
    Mockito.when(caService.createContentAssembly(Mockito.any(User.class), Mockito.anyString(),
        Mockito.anyString(), Mockito.any(ContentAssemblyCreateOptions.class))).thenReturn(caNode);

    InvokeContainerWizardWebService service = new InvokeContainerWizardWebService();

    int resultNumberOfContainers = 0;
    for (Object o : conf.getPrimaryContainer().getContainerConfOrXmlMoConf()) {

      if (o instanceof ContainerConf) {

        resultNumberOfContainers++;
        String resultId = service.addContainer(caService, user, primaryContainer, (ContainerConf) o,
            aclMap, defaultAclId);

        // Container is a managed object
        assertEquals(resultId, "mo");
      }

    }

    // Number of containers added is 2
    assertEquals(resultNumberOfContainers, expectedNumberOfContainers);

  }

  /**
   * Verify a list of managed objects are added as content.
   */
  @Test
  public void addManagedObjects()
      throws SAXException, IOException, ParserConfigurationException, RSuiteException,
      JAXBException, TransformerException {

    ContainerWizardConf conf = new ContainerWizardTestUtils().newContainerWizardConfForTests();

    // There are six <xml-mo-conf> elements configured in sample wizard configuration.
    int expectedNumberOfMos = 6;

    ManagedObject templateMo = Mockito.mock(ManagedObject.class);

    IDGenerator idGenerator = Mockito.mock(IDGenerator.class);
    Mockito.when(idGenerator.allocateId()).thenReturn("11111");

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
    Element elem = Mockito.mock(Element.class);
    Element[] elemArr = {elem};
    NamedNodeMap atts = Mockito.mock(NamedNodeMap.class);
    Mockito.when(elem.getAttributes()).thenReturn(atts);
    Mockito.when(eval.executeXPathToNodeArray(Mockito.anyString(), Mockito.any(Object.class)))
        .thenReturn(elemArr);

    String containerId = "22222";
    FutureManagedObject fmo = Mockito.mock(FutureManagedObject.class);

    List<FutureManagedObject> fmoList = new ArrayList<FutureManagedObject>();
    fmoList.add(fmo);
    ACL acl = Mockito.mock(ACL.class);

    InvokeContainerWizardWebService service = Mockito.mock(InvokeContainerWizardWebService.class);
    Mockito.when(service.loadMo(Mockito.any(Element.class), Mockito.any(ExecutionContext.class),
        Mockito.any(User.class), Mockito.anyString(), Mockito.any(ManagedObjectAdvisor.class)))
        .thenReturn(templateMo);
    Mockito.when(service.getObjectSource(Mockito.any(Element.class), Mockito.any(
        ExecutionContext.class), Mockito.anyString())).thenReturn(null);
    Mockito.doCallRealMethod().when(service).addManagedObjects(context, user, eval, true,
        containerId, fmoList, acl, null, false);

    int actualNumberOfMos = 0;
    for (Object o : conf.getPrimaryContainer().getContainerConfOrXmlMoConf()) {
      if (o instanceof XmlMoConf) {
        actualNumberOfMos += service.addManagedObjects(context, user, eval, true, containerId,
            fmoList, acl, null, false).getCount();
      }
    }

    // Number of MOs added is 6
    assertEquals(expectedNumberOfMos, actualNumberOfMos);

  }

}
