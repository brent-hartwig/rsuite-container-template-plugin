package com.rsicms.rsuite.containerWizard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;

import biz.source_code.base64Coder.Base64Coder;

/**
 * Defines everything known about an instance of the container wizard. Implements Serializable in
 * order to be passed between web service calls and form advisors (which is a way to retain state
 * without implementing a cache expiration policy).
 */
public class ContainerWizard
    implements Serializable {

  private static final long serialVersionUID = 1L;

  private String operationName;

  private String containerName;

  private Map<String, List<String>> containerMetadata = new HashMap<String, List<String>>();

  private SortedMap<String, List<FutureManagedObject>> futureMoList =
      new TreeMap<String, List<FutureManagedObject>>();

  private AddXmlMoContext addXmlMoContext;

  public ContainerWizard() {

  }

  public String getOperationName() {
    return operationName;
  }

  public void setOperationName(String opName) {
    this.operationName = opName;
  }

  public String getContainerName() {
    return containerName;
  }

  public void setContainerName(String containerName) {
    this.containerName = containerName;
  }

  public Map<String, List<String>> getContainerMetadata() {
    return containerMetadata;
  }

  public List<MetaDataItem> getContainerMetadataAsList() {
    List<MetaDataItem> mdList = new ArrayList<MetaDataItem>();
    for (Map.Entry<String, List<String>> entry : containerMetadata.entrySet()) {
      for (String lmdValue : entry.getValue()) {
        mdList.add(new MetaDataItem(entry.getKey(), lmdValue));
      }
    }
    return mdList;
  }

  public void setContainerMetadata(Map<String, List<String>> containerMetadata) {
    this.containerMetadata = containerMetadata;
  }

  public boolean hasMetadataList(String name) {
    List<String> mdList = getMetadataList(name, MetadataBehavior.MAY_RETURN_NULL);
    return mdList != null && mdList.size() > 0;
  }

  public List<String> getMetadataList(String name, MetadataBehavior behavior) {
    if (behavior == MetadataBehavior.CLEAR_EXISTING_VALUES
        || (behavior == MetadataBehavior.CREATE_IF_NULL && !containerMetadata.containsKey(name))) {
      containerMetadata.put(name, new ArrayList<String>());
    }

    return containerMetadata.get(name);
  }

  public List<String> getMetadataList(String name) {
    return getMetadataList(name, MetadataBehavior.CREATE_IF_NULL);
  }

  /**
   * Add a piece of container metadata, even if this class already has one with the same name. This
   * is intended to support repeating metadata.
   * 
   * @param name
   * @param value
   */
  public void addContainerMetadata(String name, String value) {
    getMetadataList(name, MetadataBehavior.CREATE_IF_NULL).add(value);
  }

  /**
   * Set a piece of container metadata, replacing any previous values with the same name.
   * 
   * @param name
   * @param value
   */
  public void setContainerMetadata(String name, String value) {
    getMetadataList(name, MetadataBehavior.CLEAR_EXISTING_VALUES).add(value);
  }

  public String serialize() throws IOException {
    return serialize(this);
  }

  public void addFutureManagedObject(String listKey, FutureManagedObject fmo) {
    if (listKey != null && fmo != null) {
      getFutureManagedObjectListByKey(listKey).add(fmo);
    }
  }

  public List<FutureManagedObject> getFutureManagedObjectListByKey(String listKey) {
    if (listKey != null) {
      if (!futureMoList.containsKey(listKey)) {
        futureMoList.put(listKey, new ArrayList<FutureManagedObject>());
      }
      return futureMoList.get(listKey);
    }
    return null;
  }

  /**
   * @return True if there is at least one future managed object.
   */
  public boolean hasFutureManagedObjects() {
    for (Map.Entry<String, List<FutureManagedObject>> entry : futureMoList.entrySet()) {
      if (entry.getValue().size() > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the first list of future MOs. This is intended to be used when there is only one. In fact,
   * if there is more than one and this is called, an exception is thrown.
   * 
   * @return The first list of future MOs or null when there are none.
   * @throws RSuiteException Thrown when there is more than one list of future MOs.
   */
  public List<FutureManagedObject> getFirstAndOnlyFutureManagedObjectList() throws RSuiteException {
    if (futureMoList.size() == 1) {
      return futureMoList.get(futureMoList.firstKey());
    } else if (futureMoList.size() > 1) {
      throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR,
          "More than one lists exists when only one is expected.");
    }
    return null;
  }

  public SortedMap<String, List<FutureManagedObject>> getFutureManagedObjectLists() {
    return futureMoList;
  }

  public boolean isInAddXmlMoMode() {
    return addXmlMoContext != null;
  }

  public AddXmlMoContext getAddXmlMoContext() {
    return addXmlMoContext;
  }

  public void setAddXmlMoContext(AddXmlMoContext addXmlMoContext) {
    this.addXmlMoContext = addXmlMoContext;
  }

  // Credit to http://stackoverflow.com/revisions/134918/9 but dropped base64 encoding
  public static String serialize(Serializable o) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(o);
    oos.close();
    return new String(Base64Coder.encode(baos.toByteArray()));
  }

  // Credit to http://stackoverflow.com/revisions/134918/9 but dropped base64 decoding
  public static ContainerWizard deserialize(String s) throws IOException, ClassNotFoundException {
    byte[] data = Base64Coder.decode(s);
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
    Object o = ois.readObject();
    ois.close();
    return (ContainerWizard) o;
  }

  public enum MetadataBehavior {
    CREATE_IF_NULL(),
    MAY_RETURN_NULL(),
    CLEAR_EXISTING_VALUES();
  }

  public String getInfo() {
    return getInfo(this);
  }

  public static String getInfo(ContainerWizard wizard) {
    StringBuilder sb = new StringBuilder("ContainerWizard:");

    sb.append("\n  Container name: ").append(wizard.getContainerName());

    sb.append("\n  Container LMD: ");
    Map<String, List<String>> lmdMap = wizard.getContainerMetadata();
    if (lmdMap.size() > 0) {
      for (Map.Entry<String, List<String>> entry : lmdMap.entrySet()) {
        for (String lmdValue : entry.getValue()) {
          sb.append("\n    ").append(entry.getKey()).append(": '").append(lmdValue).append("'");
        }
      }
    } else {
      sb.append("NONE");
    }

    sb.append("\n  Future MO Lists: ");
    SortedMap<String, List<FutureManagedObject>> fmoLists = wizard.getFutureManagedObjectLists();
    if (fmoLists.size() > 0) {
      for (Map.Entry<String, List<FutureManagedObject>> entry : fmoLists.entrySet()) {
        sb.append("\n    ").append(entry.getKey());
        for (FutureManagedObject fmo : entry.getValue()) {
          sb.append("\n      XML template MO ID: ").append(fmo.getTemplateMoId());
          sb.append("\n      Title: ").append(fmo.getTitle());
          sb.append("\n      ---------------");
        }
      }
    } else {
      sb.append("NONE");
    }

    return sb.toString();

  }
}
