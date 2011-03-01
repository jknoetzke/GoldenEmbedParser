/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.GoogleGeocoding;

import it.units.GoogleCommon.GeocodeException;
import it.units.GoogleCommon.Location;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author giorgio
 */
public class GeocodeRequestor {

  private static final String serviceUrl = "http://maps.google.com/maps/api/geocode/xml?";
  private boolean hasPositionSensor = false;

  protected GeocodeResponse unmarshalResponse(Reader reader) throws GeocodeException {
    try {
      JAXBContext context = JAXBContext.newInstance(GeocodeResponse.class);
      Unmarshaller unmarshaller = context.createUnmarshaller();
      GeocodeResponse response = (GeocodeResponse) unmarshaller.unmarshal(reader);
      reader.close();
      return response;
    } catch (JAXBException ex) {
      throw new GeocodeException("JAXBException unmarshalling xml", ex.getCause());
    } catch (IOException ex) {
      throw new GeocodeException("IOException closing http stream", ex.getCause());
    }

  }

  protected GeocodeResponse getGeocodeInternal(String url) throws GeocodeException{
    //fetch the xml from google & unmarshall it
    URL request;
    try {
      request = new URL(url);
    } catch (MalformedURLException ex) {
      throw new GeocodeException("MalformedURLException generating URL", ex.getCause());
    }
    try {
      URLConnection connection = request.openConnection();
      GeocodeResponse response = unmarshalResponse(new InputStreamReader(connection.getInputStream()));
      if(response.getStatus()!=Status.OK){
        throw new GeocodeException("Server side error in the response: "+response.getStatus());
      }
      return response;
    } catch (IOException ex) {
      throw new GeocodeException("IOException connecting to GoogleGeocode Web Service", ex.getCause());
    }
  }

  public GeocodeResponse getGeocode(String formattedAddress) throws GeocodeException{
    return getGeocodeInternal(serviceUrl + "address=" + formattedAddress + "&sensor=" + Boolean.toString(hasPositionSensor));
  }

  public GeocodeResponse getGeocode(List<String> addressLines) throws GeocodeException{
    String formattedAddress = "";
    boolean isFirst = true;
    for (String addressLine : addressLines) {
      if (!isFirst) {
        formattedAddress += ",";
      } else {
        isFirst = false;
      }
      formattedAddress += addressLine.replaceAll(" ", "+");
    }
    return getGeocode(formattedAddress);
  }

  public GeocodeResponse getGeocode(Location location) throws GeocodeException{
    return getGeocodeInternal(serviceUrl + "latlng=" + location + "&sensor=" + Boolean.toString(hasPositionSensor));
  }
}
