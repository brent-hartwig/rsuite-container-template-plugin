package com.rsicms.rsuite.containerWizard;

/**
 * All known execution modes of the container wizard.
 */
public enum ExecutionMode {

  ADD_XML_MO(), CREATE_CONTAINER(), UNKNOWN();

  /**
   * Get an execution mode value from a string. When the provided value is not recognized, the
   * UNKNOWN enum value is return.
   * 
   * @param mode
   * @return ExecutionMode value best corresponding to the provided string value.
   */
  public static ExecutionMode get(String mode) {
    for (ExecutionMode em : ExecutionMode.values()) {
      if (em.name().equalsIgnoreCase(mode)) {
        return em;
      }
    }
    return UNKNOWN;
  }

}
