/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.GoogleGeocoding;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author giorgio
 */
public class AddressComponent implements Serializable {

  private String longName;
  private String shortName;
  private List<AddressComponentType> addressComponentTypes = new ArrayList<AddressComponentType>();

  @XmlElement(name = "type")
  public List<AddressComponentType> getAddressComponentTypes() {
    return addressComponentTypes;
  }

  public void setAddressComponentTypes(List<AddressComponentType> addressComponentTypes) {
    this.addressComponentTypes = addressComponentTypes;
  }

  @XmlElement(name = "long_name")
  public String getLongName() {
    return longName;
  }

  public void setLongName(String longName) {
    this.longName = longName;
  }

  @XmlElement(name = "short_name")
  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }
}
