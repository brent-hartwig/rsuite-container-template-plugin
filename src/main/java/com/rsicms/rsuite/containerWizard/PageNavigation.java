package com.rsicms.rsuite.containerWizard;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.result.InvokeWebServiceAction;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.Page;
import com.rsicms.rsuite.containerWizard.webService.InvokeContainerWizardWebService;

public class PageNavigation
    implements ContainerWizardConstants {

  private static Log defaultLog = LogFactory.getLog(InvokeContainerWizardWebService.class);

  private ContainerWizard wizard;
  private ContainerWizardConf conf;
  private ContainerWizardConfUtils confUtils;
  private CallArgumentList args;
  private Log log;

  private Integer pageIdx;
  private Integer nextPageIdx;
  private boolean reachedLastSubPage;
  private boolean shouldSetNextPageIdx;
  private boolean shouldSetNextSubPageIdx;

  public PageNavigation(
      ContainerWizard wizard, ContainerWizardConf conf, ContainerWizardConfUtils confUtils,
      CallArgumentList args, Log log)
      throws RSuiteException {
    this.wizard = wizard;
    this.conf = conf;
    this.confUtils = confUtils;
    this.args = args;
    this.log = log == null ? defaultLog : log;

    determineReachedLastSubPage(); // *before* determining page index

    determinePageIdx();

    // Conditionally initialize the rest of this instance.
    if (isPageRequested() && hasRequestedPage()) {
      Page page = getRequestedPageConf();

      // Set up page advancement for next web service invocation.
      nextPageIdx = -1;
      if (!reachedLastSubPage && (page.isSubPages() || getNextSubPageIdx() > 0)) {
        // Stay on the same page.
        nextPageIdx = pageIdx;
        shouldSetNextSubPageIdx = true;
      } else if (getPageList().size() > pageIdx + 1) {
        nextPageIdx = pageIdx + 1;
      }

      // Only set the page index param if there is a next page.
      if (nextPageIdx >= 0) {
        shouldSetNextPageIdx = true;
      }
    }
  }

  /**
   * Get the int value from a call argument. In RSuite 4.1.15,
   * CallArgumentList#getFirstInteger(String) would throw a NPE when the argument value was not a
   * number (e.g., "NaN").
   * 
   * @param arg
   * @param defaultValue
   * @return An int value of the given call argument; when not set or unable to convert to an int,
   *         the provided default value is returned.
   */
  public int getIntValue(CallArgument arg, int defaultValue) {
    if (arg != null) {
      String val = arg.getValue();
      if (StringUtils.isNotBlank(val)) {
        try {
          return Integer.parseInt(val);
        } catch (NumberFormatException e) {
          // allow to pass through
        }
      }
    }
    return defaultValue;
  }

  /**
   * Determine whether we're reached the last sub page.
   */
  private void determineReachedLastSubPage() {
    // Assumes there's only one page with sub-pages.
    Integer configuredSubPageSize = Integer.valueOf(confUtils.getXmlMoConfCount(conf));
    this.reachedLastSubPage = (getIntValue(args.getFirst(PARAM_NAME_NEXT_SUB_PAGE_IDX), 0)
        + getIntValue(args.getFirst(PARAM_NAME_SECTION_TYPE_IDX), 0)) >= configuredSubPageSize;
  }

  /**
   * Find out if the last sub-page has been displayed.
   * 
   * @return True the last sub-page has been displayed, indicating the next page should be served up
   *         (or user input processed if there are no more pages).
   */
  public boolean hasReachedLastSubPage() {
    return reachedLastSubPage;
  }

  private void determinePageIdx() {

    pageIdx = getIntValue(args.getFirst(PARAM_NAME_NEXT_PAGE_IDX), -1);

    // Bump the page index up by one when a page is being requested and we've processed the last sub
    // page.
    if (isPageRequested() && hasReachedLastSubPage()) {
      pageIdx++;
    }

  }

  /**
   * Get the next requested page index.
   * 
   * @return If -1, no page is being requested.
   */
  public Integer getPageIdx() {
    return pageIdx;
  }

  /**
   * Find out if a page is requested. This is not one in the same with the requested page being
   * available.
   * 
   * @see #hasRequestedPage()
   * @return True if a page is being requested.
   */
  public boolean isPageRequested() {

    // If we're in add XML MO mode and have at least one future MO, a new page is not desired.
    if (wizard.isInAddXmlMoMode() && wizard.hasFutureManagedObjects()) {
      return false;
    }

    return getPageIdx() >= 0;
  }

  /**
   * Find out if the wizard's configuration has the requested page.
   * 
   * @return True if it does.
   */
  public boolean hasRequestedPage() {
    return getPageIdx() < getPageList().size();
  }

  /**
   * Get the configuration of the requested page.
   * 
   * @return The requested page's configuration.
   */
  public Page getRequestedPageConf() {
    return getPageList().get(getPageIdx());
  }

  /**
   * Get the list of pages.
   * 
   * @return List of pages from the wizard's configuration.
   */
  public List<Page> getPageList() {
    return conf.getPages().getPage();
  }

  public Integer getCurrentSubPageIdx() {
    return getNextSubPageIdx() - 1;
  }

  /**
   * Get the sub page index.
   * 
   * @return The sub page index.
   */
  public Integer getNextSubPageIdx() {
    return getIntValue(args.getFirst(PARAM_NAME_NEXT_SUB_PAGE_IDX), 0);
  }

  /**
   * Find out if the page was dismissed/closed.
   * 
   * @return True if the page was dismissed.
   */
  public boolean wasPageDismissed() {
    return getNextSubPageIdx() == null;
  }

  /**
   * Get the REST result for this page navigation.
   * 
   * @param confAlias
   * @return The rest result for this page navigation.
   * @throws RSuiteException
   * @throws IOException
   */
  public RestResult getRestResult(String confAlias) throws RSuiteException, IOException {
    if (!hasRequestedPage()) {
      throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
          "The requested page does not exist.");
    }

    Page page = getRequestedPageConf();
    if (StringUtils.isNotBlank(page.getFormId())) {
      return constructFormRequest(confAlias, page);
    } else if (StringUtils.isNotBlank(page.getActionId())) {
      return constructActionRequest(confAlias, page);
    } else {
      // Configuration error (no form or action).
      throw new RSuiteException(RSuiteException.ERROR_CONFIGURATION_PROBLEM,
          "Page does not declare a form or an action.");
    }

  }

  /**
   * Construct a REST result that'll request a form.
   * 
   * @param confAlias
   * @param page
   * @return a REST result that'll request a form.
   * @throws IOException
   */
  public RestResult constructFormRequest(String confAlias, Page page) throws IOException {
    log.info("Constructing a form request...");

    InvokeWebServiceAction serviceAction = new InvokeWebServiceAction(args.getFirstString(
        PARAM_NAME_API_NAME));
    serviceAction.setFormId(page.getFormId());

    serviceAction.addServiceParameter(PARAM_NAME_CONF_ALIAS, confAlias);
    serviceAction.addServiceParameter(PARAM_NAME_CONTAINER_WIZARD, wizard.serialize());

    if (shouldSetNextPageIdx) {
      log.info("Setting next page index to " + nextPageIdx);
      serviceAction.addServiceParameter(PARAM_NAME_NEXT_PAGE_IDX, String.valueOf(nextPageIdx));
    }
    if (shouldSetNextSubPageIdx) {
      log.info("Setting next sub page index to " + getNextSubPageIdx());
      serviceAction.addFormParameter(PARAM_NAME_NEXT_SUB_PAGE_IDX, String.valueOf(
          getNextSubPageIdx()));
    }

    // Make the web service string params also available to the form.
    for (CallArgument arg : serviceAction.getServiceParameters().values()) {
      if (StringUtils.isNotBlank(arg.getValue())) {
        serviceAction.addFormParameter(arg.getName(), arg.getValue());
      }
    }

    RestResult restResult = new RestResult();
    restResult.addAction(serviceAction);
    return restResult;
  }

  /**
   * Construct a REST result that'll request a CMS UI action.
   * 
   * @param confAlias
   * @param page
   * @return a REST result that'll request a CMS UI action.
   * @throws IOException
   */
  public RestResult constructActionRequest(String confAlias, Page page) throws IOException {
    log.info("Constructing an action request...");

    UserInterfaceAction wizardPage = new UserInterfaceAction(page.getActionId());

    wizardPage.addProperty(PARAM_NAME_CONF_ALIAS, confAlias);
    wizardPage.addProperty(PARAM_NAME_API_NAME, args.getFirstString(PARAM_NAME_API_NAME));
    wizardPage.addProperty(PARAM_NAME_CONTAINER_WIZARD, wizard.serialize());
    wizardPage.addProperty(PARAM_NAME_OPERATION_NAME, wizard.getOperationName());

    if (shouldSetNextPageIdx) {
      log.info("Setting next page index to " + nextPageIdx);
      wizardPage.addProperty(PARAM_NAME_NEXT_PAGE_IDX, nextPageIdx);
    }

    // TODO: use shouldSetNextSubPageIdx?
    if (page.isSubPages()) {
      Integer nextSubPageOffset = getIntValue(args.getFirst(PARAM_NAME_SECTION_TYPE_IDX), 0);
      Integer nextSubPageIdx = wizard.isInAddXmlMoMode() ? wizard.getAddXmlMoContext()
          .getXmlMoConfIdx() : getNextSubPageIdx() + nextSubPageOffset;
      log.info("Setting next sub page index to " + nextSubPageIdx + " (offset was "
          + nextSubPageOffset + ")");
      wizardPage.addProperty(PARAM_NAME_NEXT_SUB_PAGE_IDX, nextSubPageIdx);
    }

    RestResult restResult = new RestResult();
    restResult.addAction(wizardPage);
    return restResult;
  }

}
