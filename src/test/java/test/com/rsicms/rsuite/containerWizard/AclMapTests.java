package test.com.rsicms.rsuite.containerWizard;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.rsicms.rsuite.containerWizard.AclMap;

public class AclMapTests {

  public final static String DELIM = "_";

  // @Test
  // public void createUndefinedRoles() {
  // RoleManager roleManager = Mockito.mock(RoleManager.class);
  // Mockito.
  // }

  /**
   * Verify receipt of expected role name when input only includes alphanumeric characters.
   */
  @Test
  public void roleNameWithOnlyAlphaNumChars() {
    String prefix = "1prefix9prefix1234";
    String base = "11base55base77";

    String actualName = AclMap.getRoleName(prefix, base);
    String expectedName = new StringBuilder(prefix).append(DELIM).append(base).toString();

    assertEquals(actualName, expectedName);
  }

  /**
   * Verify receipt of expected role name when input includes non-alphanumeric characters.
   */
  @Test
  public void roleNameWithNonAlphaNumChars() {
    String prefix = "my$Pre*f)ix3";
    String base = "7my!@#$%^&*()BaseRoleName";

    String cleanPrefix = prefix.replaceAll("[^\\p{Alnum}]", DELIM);
    String cleanBase = base.replaceAll("[^\\p{Alnum}]", DELIM);

    String actualName = AclMap.getRoleName(prefix, base);
    String expectedName = new StringBuilder(cleanPrefix).append(DELIM).append(cleanBase).toString();

    assertEquals(actualName, expectedName);
  }

}
