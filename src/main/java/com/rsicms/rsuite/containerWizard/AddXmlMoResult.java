package com.rsicms.rsuite.containerWizard;

import java.util.ArrayList;
import java.util.List;

import com.reallysi.rsuite.api.ManagedObject;

/**
 * Means to describe newly added MOs when the MOs are only available sometimes. When adding
 * top-level MOs, the MOs are known. When adding sub-MOs, we only know the count.
 */
public class AddXmlMoResult {

  private List<ManagedObject> moList = new ArrayList<ManagedObject>();
  private int cnt = 0;

  /**
   * Denote the provided MO as a new one. No need to call {@link #increment()} when calling this
   * method.
   * 
   * @param mo
   */
  public void add(ManagedObject mo) {
    moList.add(mo);
    increment();
  }

  /**
   * The list of new MOs. Will be empty if sub-MOs were added.
   * 
   * @return The list of new MOs. Will be empty if sub-MOs were added.
   */
  public List<ManagedObject> getManagedObjects() {
    return moList;
  }

  /**
   * Add one to the new MO counter. No need to call this if using {@link #add(ManagedObject)}.
   */
  public void increment() {
    cnt++;
  }

  /**
   * If there are MOs, get the number of MOs; else, return the count maintained by
   * {@link #increment()}.
   * 
   * @return The number of added MOs.
   */
  public int getCount() {
    if (moList != null && moList.size() > 0) {
      return moList.size();
    }
    return cnt;
  }

}
