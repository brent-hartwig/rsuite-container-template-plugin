package com.rsicms.rsuite.containerWizard;

public interface ContainerWizardConstants {

  final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

  final String DEFAULT_OPERATION_NAME = "Create Product";

  final String PARAM_NAME_PREFIX_LMD = "lmd";
  final String PARAM_NAME_SUFFIX_VERIFY = "-Verify";

  final String PARAM_NAME_OPERATION_NAME = "operationName";
  final String PARAM_NAME_EXECUTION_MODE = "executionMode";
  final String PARAM_NAME_INSERTION_POSITION = "insertionPosition";
  final String PARAM_NAME_RSUITE_ID = "rsuiteId";
  final String PARAM_NAME_CONTAINER_NAME = "containerName";
  final String PARAM_NAME_API_NAME = "apiName";
  final String PARAM_NAME_CONF_ALIAS = "confAlias";
  final String PARAM_NAME_CONTAINER_WIZARD = "containerWizard";
  final String PARAM_NAME_NEXT_PAGE_IDX = "nextPageIdx";
  final String PARAM_NAME_NEXT_SUB_PAGE_IDX = "nextSubPageIdx";
  final String PARAM_NAME_SECTION_TYPE_IDX = "sectionTypeIdx";
  final String PARAM_NAME_SECTION_TYPE = "sectionType";
  final String PARAM_NAME_XML_TEMPLATE_MO_ID = "templateId";
  final String PARAM_NAME_XML_TEMPLATE_TYPE = "xmlTemplateType";
  final String PARAM_NAME_SECTION_TITLE = "title";

  final String LMD_NAME_XML_TEMPLATE_TYPE = "XmlTemplateType";
  final String LMD_NAME_JOB_CODE = "JobCode";
  final String LMD_NAME_PRODUCT_ID = "ProductId";

  /**
   * The suffix of the container role name to grant to the user that creates the container.
   * 
   * TODO: make configurable
   */
  final String CONTAINER_ROLE_NAME_SUFFIX_TO_GRANT = "AIC_AD";

}
