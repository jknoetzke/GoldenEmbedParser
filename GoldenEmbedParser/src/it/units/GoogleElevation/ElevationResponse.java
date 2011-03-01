/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.units.GoogleElevation;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author giorgio
 */
@XmlRootElement (name = "ElevationResponse")
public class ElevationResponse {
  private Status status;
  //@XmlElement (name = "result")
  private List<Result> results = new ArrayList<Result>();

  @XmlElement (name = "result")
  public List<Result> getResults() {
    return results;
  }

  public void setResults(List<Result> results) {
    this.results = results;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  public String toString() {
    if((results==null)||(results.size()==0)){
      return super.toString();
    }else{
      String niceResult = "";
      for(Result result : results){
        niceResult += result + "; ";
      }
      return niceResult.trim();
    }
    
  }


  
}
