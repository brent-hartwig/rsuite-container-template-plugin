package com.rsicms.rsuite.containerWizard;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerConf;
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.ObjectFactory;
import com.rsicms.rsuite.containerWizard.jaxb.XmlMoConf;

public class ContainerWizardConfUtils {

  public ManagedObject getContainerWizardConfMo(User user, ManagedObjectService moService,
      String alias) throws RSuiteException {

    if (StringUtils.isBlank(alias)) {
      throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
          "The alias for the wizard configuration MO was not provided.");
    }

    List<ManagedObject> candidateMos = moService.getObjectsByAlias(user, alias);
    if (candidateMos == null || candidateMos.size() == 0) {
      throw new RSuiteException(RSuiteException.ERROR_OBJECT_NOT_FOUND,
          "Unable to find a wizard configuration MO with the '" + alias + "' alias.");
    } else if (candidateMos.size() == 1) {
      return candidateMos.get(0);
    }

    throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR, "Found " + candidateMos.size()
        + " wizard configuration MOs with the '" + alias + "' alias when exactly one is required.");
  }

  public ContainerWizardConf getContainerWizardConf(User user, ManagedObjectService moService,
      String alias) throws JAXBException, RSuiteException {
    return getContainerWizardConf(getContainerWizardConfMo(user, moService, alias));
  }

  public ContainerWizardConf getContainerWizardConf(ManagedObject mo)
      throws JAXBException, RSuiteException {
    JAXBContext jaxbContext = JAXBContext.newInstance(ContainerWizardConf.class.getPackage()
        .getName(), ObjectFactory.class.getClassLoader());
    return (ContainerWizardConf) jaxbContext.createUnmarshaller().unmarshal(mo.getElement());
  }

  /**
   * Get the number of container configurations in the provided wizard configuration.
   * 
   * @param conf
   * @return The number of container configurations in the provided wizard configuration.
   */
  public int getContainerConfCount(ContainerWizardConf conf) {
    return getContainerConfList(conf).size();
  }

  public List<ContainerConf> getContainerConfList(ContainerWizardConf conf) {
    List<ContainerConf> containerConfList = new ArrayList<ContainerConf>();
    for (Object o : conf.getPrimaryContainer().getContainerConfOrXmlMoConf()) {
      if (o instanceof ContainerConf) {
        containerConfList.add((ContainerConf) o);
      }
    }
    return containerConfList;
  }

  /**
   * Get the number of XML MO configurations in the provided wizard configuration.
   * 
   * @param conf
   * @return The number of XML MO configurations in the provided wizard configuration.
   */
  public int getXmlMoConfCount(ContainerWizardConf conf) {
    return getXmlMoConfList(conf).size();
  }

  public List<XmlMoConf> getXmlMoConfList(ContainerWizardConf conf) {
    List<XmlMoConf> xmlMoConfList = new ArrayList<XmlMoConf>();
    for (Object o : conf.getPrimaryContainer().getContainerConfOrXmlMoConf()) {
      if (o instanceof XmlMoConf) {
        xmlMoConfList.add((XmlMoConf) o);
      }
    }
    return xmlMoConfList;
  }

  /**
   * Get a sub list of XML MO configuration instances, starting at a specified index and
   * conditionally stopping after the first required one.
   * 
   * @param conf
   * @param startIdx Identify where in the list to start at. Submit zero to start at the beginning
   *        of the list.
   * @param throughFirstRequired Submit true if none after the first qualifying XML MO configuration
   *        instance marked as required should be included.
   * @return All or some of the XML MO configuration instances within the provided configuration.
   */
  public List<XmlMoConf> getXmlMoConfSubList(ContainerWizardConf conf, int startIdx,
      boolean throughFirstRequired) {
    int i = 0;
    List<XmlMoConf> XmlMoConfList = new ArrayList<XmlMoConf>();
    XmlMoConf xmlMoConf;
    for (Object o : conf.getPrimaryContainer().getContainerConfOrXmlMoConf()) {
      if (o instanceof XmlMoConf) {
        if (i >= startIdx) {
          xmlMoConf = (XmlMoConf) o;
          XmlMoConfList.add(xmlMoConf);
          if (throughFirstRequired && xmlMoConf.isRequired()) {
            break;
          }
        }
        i++;
      }
    }
    return XmlMoConfList;
  }

  /**
   * Given a couple local element names and a container wizard's configuration, determine the first
   * allowed XML MO configuration index.
   * 
   * @param conf
   * @param localNameBefore The element name that comes before the insertion point. May be null.
   * @param localNameAfter The element name that comes after the insertion point. May be null.
   * @return The XML MO configuration index allowed between the two local names. If -1, nothing is
   *         allowed.
   */
  public int getFirstAllowedXmlMoConfIndex(ContainerWizardConf conf, String localNameBefore,
      String localNameAfter) {

    List<XmlMoConf> xmlMoConfList = getXmlMoConfList(conf);
    XmlMoConf xmlMoConf;

    for (int i = 0; i < xmlMoConfList.size(); i++) {
      xmlMoConf = xmlMoConfList.get(i);

      if (xmlMoConf.getLocalName().equals(localNameBefore)) {
        if (xmlMoConf.isMultiple()) {
          return i;
        } else if (i + 1 < xmlMoConfList.size()) {
          xmlMoConf = xmlMoConfList.get(i + 1);
          if (xmlMoConf.isMultiple() || !xmlMoConf.getLocalName().equals(localNameAfter)) {
            return i + 1;
          }
        }
        return -1;
      } else if (xmlMoConf.getLocalName().equals(localNameAfter)) {
        int candidate = xmlMoConf.isMultiple() ? i : -1;
        for (int t = i - 1; t >= 0; t--) {
          xmlMoConf = xmlMoConfList.get(t);
          if (xmlMoConf.isRequired()) {
            return t;
          }
          candidate = t;
        }
        return candidate;
      }
    }

    return -1;

  }

}
