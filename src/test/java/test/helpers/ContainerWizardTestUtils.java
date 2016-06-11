package test.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mockito.Mockito;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.rsicms.rsuite.containerWizard.ContainerWizardConfUtils;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;

/**
 * Container wizard-related utility methods to facilitate testing.
 */
public class ContainerWizardTestUtils {

  /**
   * Construct an instance of ContainerWizardConf to be used by a test.
   * 
   * @param confUtils
   * @return Instance of ContainerWizardConf to be used by a test.
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws RSuiteException
   * @throws JAXBException
   */
  public ContainerWizardConf newContainerWizardConfForTests() throws SAXException, IOException,
      ParserConfigurationException, RSuiteException, JAXBException {

    InputStream inputStream = this.getClass().getClassLoader()
        .getResourceAsStream("SampleContainerWizardConfiguration.xml");
    assertNotNull("Unable to load the test's wizard configuration file.", inputStream);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    assertTrue("Document builder isn't namespace aware.", db.isNamespaceAware());
    Element elem = db.parse(inputStream).getDocumentElement();

    assertNotNull("The test's wizard configuration file's Element is null.", elem);
    assertEquals("Unexpected product configuration document element local name.",
        "container-wizard-conf", elem.getTagName());
    assertEquals("Unexpected product configuration document element namespace.",
        "http://www.rsicms.com/rsuite/ns/conf/container-wizard", elem.getNamespaceURI());

    ManagedObject mo = Mockito.mock(ManagedObject.class);
    Mockito.when(mo.getElement()).thenReturn(elem);

    ContainerWizardConfUtils confUtils = new ContainerWizardConfUtils();
    ContainerWizardConf conf = confUtils.getContainerWizardConf(mo);
    assertNotNull("The product configuration instance is null", conf);
    return conf;

  }

}
