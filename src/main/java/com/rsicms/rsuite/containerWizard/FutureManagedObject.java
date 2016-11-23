package com.rsicms.rsuite.containerWizard;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

public class FutureManagedObject
    implements Serializable {

  private static final long serialVersionUID = 1L;

  private String templateMoId;
  private String title;

  public FutureManagedObject() {}

  public String getTemplateMoId() {
    return templateMoId;
  }

  public void setTemplateMoId(String templateMoId) {
    this.templateMoId = templateMoId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Find out if this future MO's input is valid. We've run into a couple scenarios where invalid
   * input is collected. Rather that not collect it, we're flagging as invalid which may give us
   * additional options later (i.e., inform the user).
   * 
   * @return True if considered valid; else, false.
   */
  public boolean isInputValid() {
    return StringUtils.isNotBlank(templateMoId) && !"null".equalsIgnoreCase(templateMoId);
  }

}
