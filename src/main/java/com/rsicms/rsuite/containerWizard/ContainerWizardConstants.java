package com.rsicms.rsuite.containerWizard;

public interface ContainerWizardConstants {

  String DEFAULT_CHARACTER_ENCODING = "UTF-8";

  String DEFAULT_OPERATION_NAME = "Create Product";

  String PARAM_NAME_PREFIX_LMD = "lmd";

  String PARAM_NAME_OPERATION_NAME = "operationName";
  String PARAM_NAME_EXECUTION_MODE = "executionMode";
  String PARAM_NAME_INSERTION_POSITION = "insertionPosition";
  String PARAM_NAME_RSUITE_ID = "rsuiteId";
  String PARAM_NAME_CONTAINER_NAME = "containerName";
  String PARAM_NAME_API_NAME = "apiName";
  String PARAM_NAME_CONF_ALIAS = "confAlias";
  String PARAM_NAME_CONTAINER_WIZARD = "containerWizard";
  String PARAM_NAME_NEXT_PAGE_IDX = "nextPageIdx";
  String PARAM_NAME_NEXT_SUB_PAGE_IDX = "nextSubPageIdx";
  String PARAM_NAME_SECTION_TYPE_IDX = "sectionTypeIdx";
  String PARAM_NAME_SECTION_TYPE = "sectionType";
  String PARAM_NAME_XML_TEMPLATE_MO_ID = "templateId";
  String PARAM_NAME_XML_TEMPLATE_TYPE = "xmlTemplateType";
  String PARAM_NAME_SECTION_TITLE = "title";

  String LMD_NAME_XML_TEMPLATE_TYPE = "XmlTemplateType";
  String LMD_NAME_JOB_CODE = "JobCode";
  String LMD_NAME_PRODUCT_ID = "ProductId";

  String DATA_TYPE_NAME_LOADING = "rsuite-container-wizard-plugin-dt-loading";
  String DATA_TYPE_NAME_CLOSE_AND_CONTINUE = "rsuite-container-wizard-plugin-dt-close-and-continue";
  String DATA_TYPE_NAME_CLOSE_ONLY = "rsuite-container-wizard-plugin-dt-close";

  /**
   * The suffix of the container role name to grant to the user that creates the container.
   * 
   * TODO: make configurable
   */
  String CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT = "AIC_AD";

}
