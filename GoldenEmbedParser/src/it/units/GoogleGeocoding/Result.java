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
public class Result implements Serializable {

  private List<AddressComponentType> addressTypes = new ArrayList<AddressComponentType>();
  private String formattedAddress;
  private List<AddressComponent> addressComponents = new ArrayList<AddressComponent>();
  private Geometry geometry;

  public Geometry getGeometry() {
    return geometry;
  }

  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
  }

  @XmlElement(name = "address_component")
  public List<AddressComponent> getAddressComponents() {
    return addressComponents;
  }

  public void setAddressComponents(List<AddressComponent> addressComponents) {
    this.addressComponents = addressComponents;
  }

  @XmlElement(name = "formatted_address")
  public String getFormattedAddress() {
    return formattedAddress;
  }

  public void setFormattedAddress(String formattedAddress) {
    this.formattedAddress = formattedAddress;
  }

  @XmlElement(name = "type")
  public List<AddressComponentType> getAddressTypes() {
    return addressTypes;
  }

  public void setAddressTypes(List<AddressComponentType> addressTypes) {
    this.addressTypes = addressTypes;
  }
}
