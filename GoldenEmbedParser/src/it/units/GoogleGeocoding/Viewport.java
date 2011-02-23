/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.units.GoogleGeocoding;

import it.units.GoogleCommon.Location;
import java.io.Serializable;

/**
 *
 * @author giorgio
 */
public class Viewport implements Serializable{
  private Location southwest;
  private Location northeast;

  public Location getNortheast() {
    return northeast;
  }

  public void setNortheast(Location northeast) {
    this.northeast = northeast;
  }

  public Location getSouthwest() {
    return southwest;
  }

  public void setSouthwest(Location southwest) {
    this.southwest = southwest;
  }
}
