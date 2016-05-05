package com.rsicms.rsuite.containerWizard.datatype;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.DataTypeOptionValue;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.forms.DataTypeProviderOptionValuesContext;
import com.reallysi.rsuite.api.forms.DefaultDataTypeOptionValuesProviderHandler;

public class LoadingOptionValuesProviderHandler extends DefaultDataTypeOptionValuesProviderHandler {

  @Override
  public void provideOptionValues(DataTypeProviderOptionValuesContext context,
      List<DataTypeOptionValue> optionValues) throws RSuiteException {

    optionValues.add(new DataTypeOptionValue(StringUtils.EMPTY, StringUtils.EMPTY));
    optionValues.add(new DataTypeOptionValue("loading", "Loading..."));

  }

}
