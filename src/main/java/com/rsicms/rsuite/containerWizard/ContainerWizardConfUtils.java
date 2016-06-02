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
import com.rsicms.rsuite.containerWizard.jaxb.ContainerWizardConf;
import com.rsicms.rsuite.containerWizard.jaxb.ObjectFactory;
import com.rsicms.rsuite.containerWizard.jaxb.XmlMoConf;

public class ContainerWizardConfUtils {

  public static ManagedObject getContainerWizardConfMo(User user, ManagedObjectService moService,
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

  public static ContainerWizardConf getContainerWizardConf(User user,
      ManagedObjectService moService, String alias) throws JAXBException, RSuiteException {
    ManagedObject mo = getContainerWizardConfMo(user, moService, alias);
    JAXBContext jaxbContext = JAXBContext.newInstance(
        ContainerWizardConf.class.getPackage().getName(), ObjectFactory.class.getClassLoader());
    return (ContainerWizardConf) jaxbContext.createUnmarshaller().unmarshal(mo.getElement());
  }

  public static ContainerWizardConf getContainerWizardConf(ManagedObject mo)
      throws JAXBException, RSuiteException {
    JAXBContext jaxbContext = JAXBContext.newInstance(
        ContainerWizardConf.class.getPackage().getName(), ObjectFactory.class.getClassLoader());
    return (ContainerWizardConf) jaxbContext.createUnmarshaller().unmarshal(mo.getElement());
  }

  public static List<XmlMoConf> getXmlMoConfList(ContainerWizardConf conf) {
    List<XmlMoConf> XmlMoConfList = new ArrayList<XmlMoConf>();
    for (Object o : conf.getPrimaryContainer().getContainerConfOrXmlMoConf()) {
      if (o instanceof XmlMoConf) {
        XmlMoConfList.add((XmlMoConf) o);
      }
    }
    return XmlMoConfList;
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
  public static List<XmlMoConf> getXmlMoConfSubList(ContainerWizardConf conf, int startIdx,
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

}
