package com.rsicms.rsuite.containerWizard;

import java.io.IOException;

import com.rsicms.rsuite.utils.messsageProps.LibraryMessageProperties;

/**
 * Serves up formatted messages from a messages properties file whose name base name matches this
 * class' name, extension is ".properties", and may be found in the same JAR file.
 */
public class ContainerWizardMessageProperties
    extends LibraryMessageProperties {

  public ContainerWizardMessageProperties()
      throws IOException {
    super(ContainerWizardMessageProperties.class);
  }

}
