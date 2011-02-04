/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.units.GoogleGeocoding;

/**
 *
 * @author giorgio
 */
public enum Status {
  OK,
  ZERO_RESULTS, // indicates that the geocode was successful but returned no results.
                // This may occur if the geocode was passed a non-existent address or a latlng in a remote location
  OVER_QUERY_LIMIT, // indicates that you are over your quota
  REQUEST_DENIED, //indicates that your request was denied, generally because of lack of a sensor parameter
  INVALID_REQUEST //generally indicates that the query (address or latlng) is missing
}
