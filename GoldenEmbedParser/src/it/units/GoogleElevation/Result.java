/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.units.GoogleElevation;

import it.units.GoogleCommon.Location;
import java.io.Serializable;

/**
 *
 * @author giorgio
 */
public class Result implements Serializable{
  private Location location;
  private float elevation;

  public float getElevation() {
    return elevation;
  }

  public void setElevation(float elevation) {
    this.elevation = elevation;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  @Override
  public String toString() {
    if(location==null){
      return super.toString();
    }else{
      return location.toString()+","+elevation;
    }
  }

  

}
