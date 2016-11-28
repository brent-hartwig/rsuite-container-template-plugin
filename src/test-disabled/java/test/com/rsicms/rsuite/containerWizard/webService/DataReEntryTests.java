package test.com.rsicms.rsuite.containerWizard.webService;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

/**
 * Home for all data re-entry tests. This plugin supports the ability to require the user enter a
 * value twice, and that those values match.
 */
public class DataReEntryTests {

  /**
   * Make sure the expected exception is thrown with values vary.
   */
  @Test
  public void differentValues() {

    CallArgument arg = new CallArgument("MyNumber", "123456");
    CallArgument argVerify = new CallArgument("MyNumber-Verify", "654321");

    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg);
    argList.add(argVerify);

    CallArgumentList args = new CallArgumentList(argList);

    try {
      InvokeContainerWizardWebService.performDataReEntryTest(arg, args);
    } catch (RSuiteException e) {
      assertThat("Error message should report values do not match.", e.getMessage(), containsString(
          "values do not match"));
      return;
    }

    fail("Values differ yet an exception was not thrown!");
  }

  /**
   * Verify no exception is thrown when two values are the same, no exception is thrown.
   * 
   * @throws RSuiteException Thrown when the values are not considered the same. If this exception
   *         is thrown, this test failed.
   */
  @Test
  public void sameValues() throws RSuiteException {
    CallArgument arg = new CallArgument("Arya", "Stark");
    CallArgument argVerify = new CallArgument("Arya-Verify", "Stark");

    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg);
    argList.add(argVerify);

    CallArgumentList args = new CallArgumentList(argList);

    InvokeContainerWizardWebService.performDataReEntryTest(arg, args);

    // All considered well if an exception is not thrown.
  }

  /**
   * Verify no exception is thrown when the value shouldn't be verified.
   * 
   * @throws RSuiteException Thrown when the values are not considered the same. If this exception
   *         is thrown, this test failed.
   */
  @Test
  public void onlyOneValue() throws RSuiteException {
    CallArgument arg = new CallArgument("AnUnverifiedValue", "123456");

    List<CallArgument> argList = new ArrayList<CallArgument>();
    argList.add(arg);

    CallArgumentList args = new CallArgumentList(argList);

    InvokeContainerWizardWebService.performDataReEntryTest(arg, args);

    // All considered well if an exception is not thrown.
  }

}
