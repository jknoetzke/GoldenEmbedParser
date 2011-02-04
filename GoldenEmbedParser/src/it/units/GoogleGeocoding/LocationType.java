/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.GoogleGeocoding;

/**
 *
 * @author giorgio
 */
public enum LocationType {

  ROOFTOP, //indicates that the returned result is a precise geocode for which we have location information accurate down to street address precision.
  RANGE_INTERPOLATED, //indicates that the returned result reflects an approximation (usually on a road) interpolated between two precise points (such as intersections). Interpolated results are generally returned when rooftop geocodes are unavailable for a street address.
  GEOMETRIC_CENTER, //indicates that the returned result is the geometric center of a result such as a polyline (for example, a street) or polygon (region).
  APPROXIMATE //indicates that the returned result is approximate.
}
