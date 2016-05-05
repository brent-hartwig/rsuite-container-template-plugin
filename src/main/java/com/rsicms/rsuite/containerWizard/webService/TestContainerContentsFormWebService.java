package com.rsicms.rsuite.containerWizard.webService;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;
import com.rsicms.rsuite.utils.webService.CallArgumentUtils;

/**
 * A developer's means to raise the container contents form without directly using the main web
 * service.
 */
public class TestContainerContentsFormWebService extends InvokeContainerWizardWebService {

  private static Log log = LogFactory.getLog(TestContainerContentsFormWebService.class);

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    Date start = new Date();
    try {
      CallArgumentUtils.logArguments(args, log);

      RestResult result = new RestResult();

      UserInterfaceAction wizardPage =
          new UserInterfaceAction("rsuite-container-wizard-plugin:wizardPage");

      wizardPage.addProperty("confAlias", args.getFirstString(PARAM_NAME_CONF_ALIAS));

      // TODO: what's the purpose of this?
      wizardPage.addProperty(PARAM_NAME_NEXT_PAGE_IDX, 15);

      Integer nextSubPageIdx = args.getFirstInteger(PARAM_NAME_NEXT_SUB_PAGE_IDX);
      log.info("Request sub page " + nextSubPageIdx);
      wizardPage.addProperty("nextSubPageIdx", nextSubPageIdx);

      wizardPage.addProperty("passThruTest", "valueFromWebService");

      wizardPage.addProperty("remoteApiName", args.getFirstString(PARAM_NAME_API_NAME));
      result.addAction(wizardPage);
      return result;

    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      throw e;
    } finally {
      log.info("Duration in millis: " + (new Date().getTime() - start.getTime()));
    }
  }

}
