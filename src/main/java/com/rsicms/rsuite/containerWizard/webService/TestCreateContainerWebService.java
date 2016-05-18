package com.rsicms.rsuite.containerWizard.webService;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;
import com.rsicms.rsuite.containerWizard.ContainerWizard;
import com.rsicms.rsuite.containerWizard.ContainerWizardConfUtils;
import com.rsicms.rsuite.containerWizard.FutureManagedObject;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.utils.webService.CallArgumentUtils;

/**
 * A means to test part of the main web service using hard-coded data. Likely to be thrown away
 * after the main web service is running end-to-end.
 */
public class TestCreateContainerWebService extends InvokeContainerWizardWebService {

  private static Log log = LogFactory.getLog(TestCreateContainerWebService.class);

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    Date start = new Date();
    try {
      // if (log.isDebugEnabled()) {
      CallArgumentUtils.logArguments(args, log);
      // }

      Session session = context.getSession();
      User user = session.getUser();
      superUser = context.getAuthorizationService().getSystemUser();

      // Load the wizard configuration.
      // By loading each time, technically it could change mid-instance.
      String confAlias = args.getFirstString(PARAM_NAME_CONF_ALIAS);
      ManagedObject confMo = ContainerWizardConfUtils.getContainerWizardConfMo(user,
          context.getManagedObjectService(), confAlias);
      ContainerWizardConf conf = ContainerWizardConfUtils.getContainerWizardConf(confMo);

      // Construct or reconstruct an instance of the container wizard
      ContainerWizard wizard = new ContainerWizard();
      wizard.setContainerName(args.getFirstString(PARAM_NAME_CONTAINER_NAME));
      wizard.addContainerMetadata(LMD_NAME_JOB_CODE, args.getFirstString("lmdJobCode"));

      FutureManagedObject fmo = new FutureManagedObject();
      fmo.setTemplateMoId("31086");
      wizard.addFutureManagedObject("0", fmo);

      fmo = new FutureManagedObject();
      fmo.setTemplateMoId("31104");
      wizard.addFutureManagedObject("1", fmo);

      // fmo = new FutureManagedObject();
      // fmo.setTemplateMoId("31110");
      // wizard.addFutureManagedObject("2", fmo);
      //
      // fmo = new FutureManagedObject();
      // fmo.setTemplateMoId("31116");
      // wizard.addFutureManagedObject("2", fmo);

      log.info(wizard.getInfo());

      // Determine the job code --TODO: how to decouple this?
      Map<String, List<String>> containerMetadata = wizard.getContainerMetadata();
      if (!containerMetadata.containsKey(LMD_NAME_JOB_CODE)) {
        return getErrorResult("Job code not specified but required.");
      }
      String jobCode = containerMetadata.get(LMD_NAME_JOB_CODE).get(0);

      // Create the container
      String parentId = context.getContentAssemblyService().getRootFolder(user).getId();
      ContentAssembly ca =
          createPrimaryContainer(context, context.getSession(), conf, wizard, parentId);

      RestResult rr = getNotificationResult(context,
          "Created '" + ca.getDisplayName() + "' (ID: " + ca.getId() + ")", "Container Wizard");
      UserInterfaceAction action = new UserInterfaceAction("rsuite:refreshManagedObjects");
      action.addProperty("objects", parentId);
      action.addProperty("children", false);
      rr.addAction(action);
      return rr;

    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      // TODO: get WebServiceUtilsMessageProperties working
      // return getErrorResult(WebServiceUtilsMessageProperties
      // .get("web.service.error.unable.to.complete", e.getMessage()));
      return getErrorResult(e.getMessage());
    } finally {
      log.info("Duration in millis: " + (new Date().getTime() - start.getTime()));
    }
  }

}
