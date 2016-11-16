package com.rsicms.rsuite.containerWizard.form.advisor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.FormControlType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.forms.DefaultFormHandler;
import com.reallysi.rsuite.api.forms.FormInstance;
import com.reallysi.rsuite.api.forms.FormInstanceCreationContext;
import com.reallysi.rsuite.api.forms.FormParameterInstance;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.rsicms.rsuite.containerWizard.ContainerWizardConstants;

public class CreateProductFormHandler
    extends DefaultFormHandler
    implements ContainerWizardConstants {

  /**
   * Log this class is to use.
   */
  private static Log log = LogFactory.getLog(CreateProductFormHandler.class);

  @Override
  public void adjustFormInstance(FormInstanceCreationContext context, FormInstance form)
      throws RSuiteException {

    List<FormParameterInstance> params = new ArrayList<FormParameterInstance>();

    List<CallArgument> callArgs = context.getArgs().getAll();

    Integer nextSubPageIdx = 0;

    for (CallArgument curArg : callArgs) {
      // We need to make sure we only have one nextSubPageIdx value and it is the largest value
      if (PARAM_NAME_NEXT_SUB_PAGE_IDX.equals(curArg.getName())) {
        Integer tempValue = Integer.parseInt(curArg.getValue());
        if (tempValue > nextSubPageIdx) {
          nextSubPageIdx = tempValue;
        }
      } else {
        // for each non-nextSubPageIdx value, add it to the form as a hidden parameter
        FormParameterInstance fpi = new FormParameterInstance();
        fpi.setName(curArg.getName());
        fpi.addValue(curArg.getValue());
        fpi.setFormControlType(FormControlType.HIDDEN);
        params.add(fpi);
      }
    }

    // add nextSubPageIdx to the form as a hidden parameter
    FormParameterInstance fpi = new FormParameterInstance();
    fpi.setName(PARAM_NAME_NEXT_SUB_PAGE_IDX);
    fpi.addValue(Integer.toString(nextSubPageIdx));
    fpi.setFormControlType(FormControlType.HIDDEN);
    params.add(fpi);

    // This parameter is "listened" for by the JS allowing us to provide arbitrary HTML/JS/CS
    // to the end user (i.e., our own form control without going through Ember)
    fpi = new FormParameterInstance();
    fpi.setName("blank");
    fpi.addValue("blank");
    fpi.setStyleClass("createProductReport");
    fpi.setFormControlType(FormControlType.INPUT);
    params.add(fpi);

    form.setFormParams(params);

  }

}
