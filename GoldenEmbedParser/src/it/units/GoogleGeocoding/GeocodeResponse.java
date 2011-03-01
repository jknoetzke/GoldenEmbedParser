/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.GoogleGeocoding;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author giorgio
 */
@XmlRootElement(name = "GeocodeResponse")
public class GeocodeResponse {

  private Status status;
  private Result result;

  public Result getResult() {
    return result;
  }

  public void setResult(Result result) {
    this.result = result;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  public String toString() {
    if (result == null) {
      return super.toString();
    } else {
      return result.getFormattedAddress();
    }
  }
}
