/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.units.GoogleCommon;

/**
 *
 * @author giorgio
 */
public class GeocodeException extends Exception{

  public GeocodeException(String message, Throwable cause) {
    super(message, cause);
  }

  public GeocodeException(String message) {
    super(message);
  }
    
}
