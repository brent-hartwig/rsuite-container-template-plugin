package test.com.rsicms.rsuite.containerWizard.webService;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.service.SearchService;
import com.rsicms.rsuite.containerWizard.ContainerWizard;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.FutureManagedObject;
import com.rsicms.rsuite.containerWizard.jaxb.MetadataConf;
import com.rsicms.rsuite.containerWizard.jaxb.NameValuePair;
import com.rsicms.rsuite.containerWizard.jaxb.ObjectFactory;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

public class InvokeContainerWizardWebServiceTest
    implements ContainerWizardConstants {

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
    Mockito.when(service.searchIfJobCodeIsAlreadyAssigned(user, searchService, jobCode)).thenReturn(
        containers);

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
    assertEquals(expectedTemplateMoId, futureMoList.get(0).getTemplateMoId());

  }

  /**
   * Supports unit tests that need to verify two metadata lists are equal to one another. Ignores
   * order and metadata IDs.
   * 
   * @param expectedList
   * @param actualList
   */
  private static void assertEqualMetadataLists(List<MetaDataItem> expectedList,
      List<MetaDataItem> actualList) {
    assertTrue("Actual and expected list sizes differ", expectedList.size() == actualList.size());

    // Make sure each item in the expected list is in the actual. Repeating LMD supported.
    MetaDataItem actualItem;
    boolean found;
    for (MetaDataItem expectedItem : expectedList) {
      found = false;
      Iterator<MetaDataItem> it = actualList.iterator();
      while (it.hasNext()) {
        actualItem = it.next();
        if (expectedItem.getName().equals(actualItem.getName())) {
          if (expectedItem.getValue().equals(actualItem.getValue())) {
            found = true;
            it.remove();
            break;
          }
        }
      }
      assertTrue(new StringBuilder("Actual doesn't include '").append(expectedItem.getName())
          .append("'='").append(expectedItem.getValue()).append("'.").toString(), found);
    }

  }

  /*
   * Test getMetadataList() when sending in a "starter" list of metadata.
   */
  @Test
  public void containerMetadataWithPopulatedStarterList() {

    // Get ready to populate the expected combined list
    List<MetaDataItem> expectedCombinedList = new ArrayList<MetaDataItem>();

    // Set up the metadata configuration
    ObjectFactory factory = new ObjectFactory();
    MetadataConf metadataConf = factory.createMetadataConf();
    List<NameValuePair> metadataConfList = metadataConf.getNameValuePair();
    NameValuePair nvp = factory.createNameValuePair();
    nvp.setName("confName1");
    nvp.setValue("confValue1");
    metadataConfList.add(nvp);
    expectedCombinedList.add(new MetaDataItem("confName1", "confValue1"));

    // Set up starter list
    List<MetaDataItem> starterList = new ArrayList<MetaDataItem>();
    MetaDataItem m1 = new MetaDataItem("starterName1", "starterValue1");
    MetaDataItem m2 = new MetaDataItem("starterName2", "starterValue2");
    starterList.add(m1);
    starterList.add(m2);
    expectedCombinedList.add(m1);
    expectedCombinedList.add(m2);

    assertEqualMetadataLists(expectedCombinedList, InvokeContainerWizardWebService.getMetadataList(
        metadataConf, starterList));

  }

  /*
   * Test getMetadataList() when passing in null for the starter list.
   */
  @Test
  public void containerMetadataWithNullStarterList() {

    // Get ready to populate the expected combined list
    List<MetaDataItem> expectedCombinedList = new ArrayList<MetaDataItem>();

    // Set up the metadata configuration
    ObjectFactory factory = new ObjectFactory();
    MetadataConf metadataConf = factory.createMetadataConf();
    List<NameValuePair> metadataConfList = metadataConf.getNameValuePair();
    NameValuePair nvp1 = factory.createNameValuePair();
    nvp1.setName("confName1");
    nvp1.setValue("confValue1");
    NameValuePair nvp2 = factory.createNameValuePair();
    nvp2.setName("confName2");
    nvp2.setValue("confValue2");
    metadataConfList.add(nvp1);
    metadataConfList.add(nvp2);
    expectedCombinedList.add(new MetaDataItem(nvp1.getName(), nvp1.getValue()));
    expectedCombinedList.add(new MetaDataItem(nvp2.getName(), nvp2.getValue()));

    assertEqualMetadataLists(expectedCombinedList, InvokeContainerWizardWebService.getMetadataList(
        metadataConf, null));
  }

  /*
   * Test getMetadataList() when passing in an empty starter list.
   */
  @Test
  public void containerMetadataWithEmptyStarterList() {

    // Get ready to populate the expected combined list
    List<MetaDataItem> expectedCombinedList = new ArrayList<MetaDataItem>();

    // Set up the metadata configuration
    ObjectFactory factory = new ObjectFactory();
    MetadataConf metadataConf = factory.createMetadataConf();
    List<NameValuePair> metadataConfList = metadataConf.getNameValuePair();
    NameValuePair nvp1 = factory.createNameValuePair();
    nvp1.setName("confName1");
    nvp1.setValue("confValue1");
    NameValuePair nvp2 = factory.createNameValuePair();
    nvp2.setName("confName2");
    nvp2.setValue("confValue2");
    metadataConfList.add(nvp1);
    metadataConfList.add(nvp2);
    expectedCombinedList.add(new MetaDataItem(nvp1.getName(), nvp1.getValue()));
    expectedCombinedList.add(new MetaDataItem(nvp2.getName(), nvp2.getValue()));

    assertEqualMetadataLists(expectedCombinedList, InvokeContainerWizardWebService.getMetadataList(
        metadataConf, new ArrayList<MetaDataItem>()));
  }
}
