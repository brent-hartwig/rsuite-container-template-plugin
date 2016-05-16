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
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;
import com.rsicms.rsuite.containerWizard.ContainerWizardSearchUtils;
import com.rsicms.rsuite.utils.mo.MOUtils;
import com.rsicms.rsuite.utils.webService.BaseWebService;
import com.rsicms.rsuite.utils.webService.CallArgumentUtils;
import com.rsicms.rsuite.utils.webService.WebServiceUtilsMessageProperties;

/**
 * Serves up information on XML templates found by a specified XML template type.
 */
public class GetTemplateInfoWebService extends BaseWebService implements ContainerWizardConstants {

  private static Log log = LogFactory.getLog(GetTemplateInfoWebService.class);

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    Date start = new Date();
    try {
      // if (log.isDebugEnabled()) {
      CallArgumentUtils.logArguments(args, log);
      // }

      // We need the template type to look up all qualifying templates
      String xmlTemplateType = args.getFirstString(PARAM_NAME_XML_TEMPLATE_TYPE);
      if (StringUtils.isBlank(xmlTemplateType)) {
        return getErrorResult("The '" + PARAM_NAME_XML_TEMPLATE_TYPE
            + "' parameter was not specified but is required.");
      }

      // Users may not have permissions to the templates; it's okay (and needed) to include those in
      // the search results.
      List<ManagedObject> moList = ContainerWizardSearchUtils.getXmlTemplates(
          context.getAuthorizationService().getSystemUser(), context.getSearchService(),
          xmlTemplateType);
      List<Map<String, String>> responseList = new ArrayList<Map<String, String>>();
      for (ManagedObject mo : moList) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", MOUtils.getDisplayNameQuietly(mo));
        map.put("moid", mo.getId());
        responseList.add(map);
      }

      RestResult restResult = new RestResult();
      restResult.setResponse(responseList);
      return restResult;

    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      return getErrorResult(WebServiceUtilsMessageProperties
          .get("web.service.error.unable.to.complete", e.getMessage()));
    } finally {
      log.info("Duration in millis: " + (new Date().getTime() - start.getTime()));
    }
  }

  protected Integer getCurrentSubPageIdx(CallArgumentList args) {
    return args.getFirstInteger(PARAM_NAME_NEXT_SUB_PAGE_IDX, 0) - 1;
  }


}
