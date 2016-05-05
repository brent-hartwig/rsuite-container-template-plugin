package com.rsicms.rsuite.containerWizard.datatype;

import java.util.List;

import com.reallysi.rsuite.api.DataTypeOptionValue;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.forms.DataTypeProviderOptionValuesContext;
import com.reallysi.rsuite.api.forms.DefaultDataTypeOptionValuesProviderHandler;

public class CloseOnlyOptionValuesProviderHandler
    extends DefaultDataTypeOptionValuesProviderHandler {

  @Override
  public void provideOptionValues(DataTypeProviderOptionValuesContext context,
      List<DataTypeOptionValue> optionValues) throws RSuiteException {

    optionValues.add(new DataTypeOptionValue("cancel", "Close"));

  }

}
