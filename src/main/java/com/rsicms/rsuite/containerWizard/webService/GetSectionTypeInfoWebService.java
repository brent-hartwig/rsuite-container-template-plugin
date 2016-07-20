package com.rsicms.rsuite.containerWizard.webService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.rsicms.rsuite.containerWizard.ContainerWizardConfUtils;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.XmlMoConf;
import com.rsicms.rsuite.utils.webService.BaseWebService;
import com.rsicms.rsuite.utils.webService.CallArgumentUtils;

/**
 * Supporting web service responsible for serving up section type information to the container
 * contents form by interpreting the wizard's configuration, starting at a specified XML MO
 * configuration (by index).
 */
public class GetSectionTypeInfoWebService extends BaseWebService
    implements ContainerWizardConstants {

  private static Log log = LogFactory.getLog(GetSectionTypeInfoWebService.class);

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

      // We need the wizard configuration to get the section type's details.
      String confAlias = args.getFirstString(PARAM_NAME_CONF_ALIAS);
      if (StringUtils.isBlank(confAlias)) {
        return getErrorResult(
            "The '" + PARAM_NAME_CONF_ALIAS + "' parameter was not specified but is required.");
      }
      ContainerWizardConfUtils confUtils = new ContainerWizardConfUtils();
      ManagedObject confMo =
          confUtils.getContainerWizardConfMo(user, context.getManagedObjectService(), confAlias);
      log.info("conf MO ID: " + confMo.getId());
      ContainerWizardConf conf = confUtils.getContainerWizardConf(confMo);
      log.info("have conf ?= " + (conf != null));

      List<Map<String, String>> responseList = new ArrayList<Map<String, String>>();

      // Restrict by sub page idx
      if (StringUtils.isBlank(args.getFirstString(PARAM_NAME_NEXT_SUB_PAGE_IDX))) {
        return getErrorResult("The '" + PARAM_NAME_NEXT_SUB_PAGE_IDX
            + "' parameter was not specified but is required.");
      }
      Integer currentSubPageIdx = args.getFirstInteger(PARAM_NAME_NEXT_SUB_PAGE_IDX, 0);
      List<XmlMoConf> xmlMoConfList = confUtils.getXmlMoConfSubList(conf, currentSubPageIdx, true);

      for (XmlMoConf xmlMoConf : xmlMoConfList) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", xmlMoConf.getDisplayName());
        map.put("xmlTemplateType", xmlMoConf.getTemplateLmdValue());
        map.put("isRequired", String.valueOf(xmlMoConf.isRequired()));
        map.put("mayRepeat", String.valueOf(xmlMoConf.isMultiple()));
        responseList.add(map);
      }

      RestResult restResult = new RestResult();
      restResult.setResponse(responseList);
      return restResult;

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
