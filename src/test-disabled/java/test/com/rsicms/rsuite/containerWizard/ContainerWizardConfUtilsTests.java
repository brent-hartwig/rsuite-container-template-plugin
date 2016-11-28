package test.com.rsicms.rsuite.containerWizard;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.rsicms.rsuite.containerWizard.ContainerWizardConfUtils;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.XmlMoConf;

public class ContainerWizardConfUtilsTests {

  /**
   * Verify a particular exception is thrown when the alias of the container wizard configuration MO
   * is null.
   * 
   * @throws RSuiteException
   */
  @Test
  public void confMoAliasIsNull() throws RSuiteException {
    User user = null;
    String alias = null;
    ManagedObjectService moService = null;

    try {
      new ContainerWizardConfUtils().getContainerWizardConfMo(user, moService, alias);
    } catch (RSuiteException e) {
      assertThat(
          "Error message should indicate not being able to find the configuration MO by the specified alias.",
          e.getMessage(), containsString(
              "The alias for the wizard configuration MO was not provided."));
      return;
    }

    fail("Null was provided for the alias yet an exception wasn't thrown.");
  }

  /**
   * Verify a particular exception is thrown when the alias of the container wizard configuration MO
   * is empty.
   * 
   * @throws RSuiteException
   */
  @Test
  public void confMoAliasIsEmpty() throws RSuiteException {
    User user = null;
    String alias = StringUtils.EMPTY;
    ManagedObjectService moService = null;

    try {
      new ContainerWizardConfUtils().getContainerWizardConfMo(user, moService, alias);
    } catch (RSuiteException e) {
      assertThat(
          "Error message should indicate not being able to find the configuration MO by the specified alias.",
          e.getMessage(), containsString(
              "The alias for the wizard configuration MO was not provided."));
      return;
    }

    fail("An empty string was provided for the alias yet an exception wasn't thrown.");
  }

  /**
   * Verify a particular exception is thrown when a container wizard configuration MO identified by
   * an alias does not exist.
   * 
   * @throws RSuiteException
   */
  @Test
  public void noConfMoExistsBecauseListIsEmpty() throws RSuiteException {
    User user = Mockito.mock(User.class);
    String alias = "anAlias";
    ManagedObjectService moService = Mockito.mock(ManagedObjectService.class);

    List<ManagedObject> moList = new ArrayList<ManagedObject>();

    Mockito.doReturn(moList).when(moService).getObjectsByAlias(user, alias);

    try {
      new ContainerWizardConfUtils().getContainerWizardConfMo(user, moService, alias);
    } catch (RSuiteException e) {
      assertThat(
          "Error message should indicate not being able to find the configuration MO by the specified alias.",
          e.getMessage(), containsString("Unable to find a wizard configuration MO with the"));
      return;
    }

    fail("Container wizard configuration MO doesn't exist yet exception not thrown.");
  }

  /**
   * Verify a particular exception is thrown when a container wizard configuration MO identified by
   * an alias does not exist.
   * 
   * @throws RSuiteException
   */
  @Test
  public void noConfMoExistsBecauseListIsNull() throws RSuiteException {
    User user = Mockito.mock(User.class);
    String alias = "anAlias";
    ManagedObjectService moService = Mockito.mock(ManagedObjectService.class);

    Mockito.doReturn(null).when(moService).getObjectsByAlias(user, alias);

    try {
      new ContainerWizardConfUtils().getContainerWizardConfMo(user, moService, alias);
    } catch (RSuiteException e) {
      assertThat(
          "Error message should indicate not being able to find the configuration MO by the specified alias.",
          e.getMessage(), containsString("Unable to find a wizard configuration MO with the"));
      return;
    }

    fail("Container wizard configuration MO doesn't exist yet exception not thrown.");
  }

  /**
   * Verify an MO is returned when there is one that matches the specified alias.
   * 
   * @throws RSuiteException
   */
  @Test
  public void exactlyOneConfMoExists() throws RSuiteException {
    User user = Mockito.mock(User.class);
    String alias = "anAlias";
    ManagedObjectService moService = Mockito.mock(ManagedObjectService.class);

    ManagedObject mockMo = Mockito.mock(ManagedObject.class);
    Mockito.doReturn("12").when(mockMo).getId();

    List<ManagedObject> moList = new ArrayList<ManagedObject>();
    moList.add(mockMo);

    Mockito.doReturn(moList).when(moService).getObjectsByAlias(user, alias);

    ManagedObject mo = new ContainerWizardConfUtils().getContainerWizardConfMo(user, moService,
        alias);
    assertNotNull(mo);
    assertEquals(mo.getId(), "12");
  }

  /**
   * Verify a particular exception is thrown when more than one container wizard configuration MOs
   * are returned for the same alias.
   * 
   * @throws RSuiteException
   */
  @Test
  public void moreThanOneConfMoExists() throws RSuiteException {
    User user = Mockito.mock(User.class);
    String alias = "anAlias";
    ManagedObjectService moService = Mockito.mock(ManagedObjectService.class);

    List<ManagedObject> moList = new ArrayList<ManagedObject>();
    moList.add(Mockito.mock(ManagedObject.class));
    moList.add(Mockito.mock(ManagedObject.class));

    Mockito.doReturn(moList).when(moService).getObjectsByAlias(user, alias);

    try {
      new ContainerWizardConfUtils().getContainerWizardConfMo(user, moService, alias);
    } catch (RSuiteException e) {
      assertThat("Error message should indicate more than one was found with the same alias.", e
          .getMessage(), containsString("when exactly one is required"));
      return;
    }

    fail(
        "More than one container wizard configuration MO with same alias yet exception not thrown.");
  }

  /**
   * Verify that it is able to unmarshal xml configuration element into a java Container Wizard Conf
   * object using JAXB.
   */
  @Test
  public void ableToUnmarshallElementToContainerWizardConf()
      throws ParserConfigurationException, SAXException, IOException, URISyntaxException,
      RSuiteException, JAXBException {

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();

    // will re-visit to add more nodes
    Element elem = doc.createElementNS("http://www.rsicms.com/rsuite/ns/conf/container-wizard",
        "container-wizard-conf");
    elem.setAttribute("name", "Sample Container Wizard Configuration");

    ManagedObject mo = Mockito.mock(ManagedObject.class);
    Mockito.when(mo.getElement()).thenReturn(elem);
    ContainerWizardConf conf = new ContainerWizardConfUtils().getContainerWizardConf(mo);

    assertNotNull(conf);
    assertEquals(conf.getName(), "Sample Container Wizard Configuration");
  }

  /**
   * Verify that the unmarshaled java Container Wizard object can return a list of XmlMoConf
   * objects.
   */
  @Test
  public void getXmlMoConfs()
      throws ParserConfigurationException, SAXException, IOException, RSuiteException,
      JAXBException {

    String expectedXmlMoConfName = "Cover";

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();

    Element containerWizardConf = doc.createElementNS(
        "http://www.rsicms.com/rsuite/ns/conf/container-wizard", "container-wizard-conf");

    Element primaryContainer = doc.createElementNS(
        "http://www.rsicms.com/rsuite/ns/conf/container-wizard", "primary-container");

    Element xmlMoConf = doc.createElementNS("http://www.rsicms.com/rsuite/ns/conf/container-wizard",
        "xml-mo-conf");
    xmlMoConf.setAttribute("display-name", expectedXmlMoConfName);

    primaryContainer.appendChild(xmlMoConf);
    containerWizardConf.appendChild(primaryContainer);

    ManagedObject mo = Mockito.mock(ManagedObject.class);
    Mockito.when(mo.getElement()).thenReturn(containerWizardConf);
    ContainerWizardConfUtils confUtils = new ContainerWizardConfUtils();
    ContainerWizardConf conf = confUtils.getContainerWizardConf(mo);

    List<XmlMoConf> resultXmlMoConfs = confUtils.getXmlMoConfList(conf);

    assertEquals(resultXmlMoConfs.get(0).getDisplayName(), expectedXmlMoConfName);

  }

  /**
   * Verify that the unmarshaled java Container Wizard object can return a sub list of XmlMoConf
   * objects.
   */
  @Test
  public void getXmlMoConfsSubList()
      throws ParserConfigurationException, SAXException, IOException, RSuiteException,
      JAXBException {

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();

    Element containerWizardConf = doc.createElementNS(
        "http://www.rsicms.com/rsuite/ns/conf/container-wizard", "container-wizard-conf");

    Element primaryContainer = doc.createElementNS(
        "http://www.rsicms.com/rsuite/ns/conf/container-wizard", "primary-container");

    int startIdx = 3;
    int totalMoConfs = 10;
    int expectedSize = totalMoConfs - startIdx;

    for (int i = 0; i < totalMoConfs; i++) {
      Element elem = doc.createElementNS("http://www.rsicms.com/rsuite/ns/conf/container-wizard",
          "xml-mo-conf");
      primaryContainer.appendChild(elem);
    }

    containerWizardConf.appendChild(primaryContainer);

    ManagedObject mo = Mockito.mock(ManagedObject.class);
    Mockito.when(mo.getElement()).thenReturn(containerWizardConf);
    ContainerWizardConfUtils confUtils = new ContainerWizardConfUtils();
    ContainerWizardConf conf = confUtils.getContainerWizardConf(mo);

    List<XmlMoConf> resultXmlMoConfs = confUtils.getXmlMoConfSubList(conf, startIdx, false);

    assertEquals(resultXmlMoConfs.size(), expectedSize);

  }

}
