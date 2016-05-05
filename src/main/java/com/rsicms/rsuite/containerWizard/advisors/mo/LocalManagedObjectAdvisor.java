package com.rsicms.rsuite.containerWizard.advisors.mo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.control.DefaultManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisorContext;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.security.ACL;

/**
 * A local MO advisor used to set the ACL of MOs created by this plugin.
 */
public class LocalManagedObjectAdvisor extends DefaultManagedObjectAdvisor {

  private static Log log = LogFactory.getLog(LocalManagedObjectAdvisor.class);

  protected ACL acl;

  public LocalManagedObjectAdvisor(ACL acl) throws RSuiteException {
    this.acl = acl;
  }

  @Override
  public void adviseDuringInsert(ExecutionContext context,
      ManagedObjectAdvisorContext insertContext) throws RSuiteException {
    try {
      // We only care about the top-level MOs, at this time.
      if (insertContext.isRootObjectOfOperation()) {
        insertContext.setACL(acl);
      }
    } catch (Exception e) {
      log.warn("Unable to set the ACL on new MO with ID " + insertContext.getId(), e);
    }
  }

}
