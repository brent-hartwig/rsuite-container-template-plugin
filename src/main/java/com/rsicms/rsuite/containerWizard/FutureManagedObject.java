package com.rsicms.rsuite.containerWizard;

import java.io.Serializable;

public class FutureManagedObject implements Serializable {

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

}
