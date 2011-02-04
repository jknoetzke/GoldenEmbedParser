/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.units.GoogleCommon;

import java.io.Serializable;

/**
 *
 * @author giorgio
 */
public class Location implements Serializable{
  /**
	 * 
	 */
	private static final long serialVersionUID = -7218715061482752214L;
	
	
	public Location()
	{
		
	}
	public Location(float lat, float lon)
	{
		this.lat = lat;
		this.lng = lon;
	}

@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(lat);
		result = prime * result + Float.floatToIntBits(lng);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Location other = (Location) obj;
		if (Float.floatToIntBits(lat) != Float.floatToIntBits(other.lat))
			return false;
		if (Float.floatToIntBits(lng) != Float.floatToIntBits(other.lng))
			return false;
		return true;
	}

private float lat;
  private float lng;

  public float getLat() {
    return lat;
  }

  public void setLat(float lat) {
    this.lat = lat;
  }

  public float getLng() {
    return lng;
  }

  public void setLng(float lng) {
    this.lng = lng;
  }

  @Override
  public String toString() {
    return lat+","+lng;
  }


  
}
