package test.com.rsicms.rsuite.containerWizard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.rsicms.rsuite.containerWizard.ContainerWizardConfUtils;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.PageNavigation;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;

import test.helpers.ContainerWizardTestUtils;

public class PageNavigationTests implements ContainerWizardConstants {

  /**
   * Number of sub-pages in the sample container wizard configuration instance. This value may need
   * to be updated when the sample is updated.
   */
  private static int numberOfSubPages = 10;

  /*
   * Test PageNavigation.hasReachedSubPage()
   */
  @Test
  public void reachedLastSubPage() throws SAXException, IOException, ParserConfigurationException,
      RSuiteException, JAXBException {

    // Set up parameters for PageNavigation
    ContainerWizardConf containerWizardConf =
        new ContainerWizardTestUtils().newContainerWizardConfForTests();
    ContainerWizardConfUtils confUtils = new ContainerWizardConfUtils();
    Log log = null;

    // Test default web service parameters
    List<CallArgument> argList = new ArrayList<CallArgument>();
    CallArgumentList args = new CallArgumentList(argList);
    PageNavigation pageNav = new PageNavigation(containerWizardConf, confUtils, args, log);
    assertFalse("Unexpectedly reached last sub page when using defaults",
        pageNav.hasReachedLastSubPage());

    // Test sub page range plus one on each end
    for (int subPageIdx = -1; subPageIdx <= numberOfSubPages + 1; subPageIdx++) {
      for (int sectionTypeIdx = -1; sectionTypeIdx <= numberOfSubPages + 1; sectionTypeIdx++) {
        argList = new ArrayList<CallArgument>();
        argList.add(new CallArgument(PARAM_NAME_NEXT_SUB_PAGE_IDX, Integer.toString(subPageIdx)));
        argList
            .add(new CallArgument(PARAM_NAME_SECTION_TYPE_IDX, Integer.toString(sectionTypeIdx)));
        args = new CallArgumentList(argList);
        pageNav = new PageNavigation(containerWizardConf, confUtils, args, log);
        // Presently expects < 0 as has not reached last sub page; not sure if that's good.
        if (subPageIdx + sectionTypeIdx < numberOfSubPages) {
          assertFalse(
              new StringBuilder("Unexpectedly reached last sub page when sub page index is ")
                  .append(subPageIdx).append(", section type index is ").append(sectionTypeIdx)
                  .append(" and number of sub pages is ").append(numberOfSubPages).toString(),
              pageNav.hasReachedLastSubPage());
        } else {
          assertTrue(
              new StringBuilder(
                  "Unexpectedly did NOT reached last sub page when sub page index is ")
                      .append(subPageIdx).append(", section type index is ").append(sectionTypeIdx)
                      .append(" and number of sub pages is ").append(numberOfSubPages).toString(),
              pageNav.hasReachedLastSubPage());
        }
      }
    }
  }
}
