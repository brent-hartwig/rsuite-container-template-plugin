package com.rsicms.rsuite.containerWizard;

/**
 * Supported insertion positions. Introduced in support of {@link ExecutionMode#ADD_XML_MO}.
 */
public enum InsertionPosition {

  BEFORE(),
  AFTER();

  /**
   * Get one of this enum's values from a string. Defaults to BELOW.
   * 
   * @param position
   * @return One of this enum's values.
   */
  public static InsertionPosition get(String position) {
    for (InsertionPosition ip : InsertionPosition.values()) {
      if (ip.name().equalsIgnoreCase(position)) {
        return ip;
      }
    }
    return AFTER;
  }

}
