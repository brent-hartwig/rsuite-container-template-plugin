package com.rsicms.rsuite.containerWizard;

import java.util.List;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.service.SearchService;
import com.rsicms.rsuite.utils.search.SearchUtils;

public class ContainerWizardSearchUtils
    implements ContainerWizardConstants {

  public static List<ManagedObject> getXmlTemplates(User user, SearchService searchService,
      String xmlTemplateType) throws RSuiteException {
    String query = new StringBuilder(SearchUtils.XPATH_ANY_ELEMENT).append(SearchUtils
        .getLayeredMetadataXPathPredicate(LMD_NAME_XML_TEMPLATE_TYPE, xmlTemplateType)).toString();
    return SearchUtils.searchForObjects(user, searchService, query, -1);
  }

}
