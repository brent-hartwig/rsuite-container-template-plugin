package test.com.rsicms.rsuite.containerWizard.advisors.mo;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.AliasContainer;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisorContext;
import com.reallysi.rsuite.api.control.MetaDataContainer;
import com.reallysi.rsuite.api.control.VariantContainer;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.security.ACL;
import com.rsicms.rsuite.containerWizard.advisors.mo.LocalManagedObjectAdvisor;

public class LocalManagedObjectAdvisorTest {
  
  /**
   * Verify the ACL given to constructor is set on the advisor context
   * if it is root object operation.
   */
  @Test
  public void setAclIfItIsRootObjectOfOperation() throws RSuiteException {
    
    ManagedObjectAdvisorContext advisorContext = new ManagedObjectAdvisorContext() {
      ACL acl = null;
      @Override
      public void setACL(ACL arg0) {
        this.acl = arg0;
      }
      @Override
      public boolean isRootObjectOfOperation() {
        return true;
      }
      @Override
      public VariantContainer getVariantContainer() {
        return null;
      }
      @Override
      public User getUser() {
        return null;
      }
      @Override
      public File getNonXmlFile() {
        return null;
      }
      @Override
      public MetaDataContainer getMetaDataContainer() {
        return null;
      }
      @Override
      public ManagedObject getInsertParentManagedObject() {
        return null;
      }
      @Override
      public String getId() {
        return null;
      }
      @Override
      public String getExternalFileName() {
        return null;
      }
      @Override
      public Element getElement() {
        return null;
      }
      @Override
      public ContentAssemblyNodeContainer getContentAssembly() {
        return null;
      }
      @Override
      public Map<String, Object> getCallContext() {
        return null;
      }
      @Override
      public AliasContainer getAliasContainer() {
        return null;
      }
      @Override
      public ACL getACL() {
        return acl;
      }
    };
    
    // Pass ACL to constructor
    ACL originalAcl = Mockito.mock(ACL.class);
    LocalManagedObjectAdvisor lmoa = new LocalManagedObjectAdvisor(originalAcl);
    
    // advise during insert
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    lmoa.adviseDuringInsert(context, advisorContext);
    
    // Get ACL from advisor context
    ACL setAcl = advisorContext.getACL();
    assertEquals(originalAcl, setAcl);
  }
  
  /**
   * Verify the ACL given to constructor is not set on the advisor context
   * if it is not root object operation.
   */
  @Test
  public void doNotSetAclIfItIsNotRootObjectOfOperation() throws RSuiteException {
    
    ManagedObjectAdvisorContext advisorContext = new ManagedObjectAdvisorContext() {
      ACL acl = null;
      @Override
      public void setACL(ACL arg0) {
        this.acl = arg0;
      }
      @Override
      public boolean isRootObjectOfOperation() {
        return false;
      }
      @Override
      public VariantContainer getVariantContainer() {
        return null;
      }
      @Override
      public User getUser() {
        return null;
      }
      @Override
      public File getNonXmlFile() {
        return null;
      }
      @Override
      public MetaDataContainer getMetaDataContainer() {
        return null;
      }
      @Override
      public ManagedObject getInsertParentManagedObject() {
        return null;
      }
      @Override
      public String getId() {
        return null;
      }
      @Override
      public String getExternalFileName() {
        return null;
      }
      @Override
      public Element getElement() {
        return null;
      }
      @Override
      public ContentAssemblyNodeContainer getContentAssembly() {
        return null;
      }
      @Override
      public Map<String, Object> getCallContext() {
        return null;
      }
      @Override
      public AliasContainer getAliasContainer() {
        return null;
      }
      @Override
      public ACL getACL() {
        return acl;
      }
    };
    
    // Pass ACL to constructor
    ACL originalAcl = Mockito.mock(ACL.class);
    LocalManagedObjectAdvisor lmoa = new LocalManagedObjectAdvisor(originalAcl);
    
    // advise during insert
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    lmoa.adviseDuringInsert(context, advisorContext);
    
    // Get ACL from advisor context
    ACL setAcl = advisorContext.getACL();
    assertEquals(null, setAcl);
  }

}
