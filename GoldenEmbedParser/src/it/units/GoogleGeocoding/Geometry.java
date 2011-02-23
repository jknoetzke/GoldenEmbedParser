/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.units.GoogleGeocoding;

import it.units.GoogleCommon.Location;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author giorgio
 */
public class Geometry implements Serializable{
  private Location location;
  private LocationType locationType;
  private Viewport viewport;

  public Viewport getViewport() {
    return viewport;
  }

  public void setViewport(Viewport viewport) {
    this.viewport = viewport;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  @XmlElement (name = "location_type")
  public LocationType getLocationType() {
    return locationType;
  }

  public void setLocationType(LocationType locationType) {
    this.locationType = locationType;
  }




}
