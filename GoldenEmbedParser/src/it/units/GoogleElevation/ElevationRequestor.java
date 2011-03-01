/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.units.GoogleElevation;

import it.units.GoogleCommon.GeocodeException;
import it.units.GoogleCommon.Location;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * 
 * @author giorgio
 */
public class ElevationRequestor {
    private static final String serviceUrl = "http://maps.google.com/maps/api/elevation/xml?locations=";
    private boolean hasPositionSensor = true;

    protected String buildUrl(List<Location> locations) {
        // verify input parameter
        if ((locations == null) || (locations.size() == 0)) {
            return null;
        }
        // initialize
        String urlWithParameters = serviceUrl;
        boolean isFirst = true;
        // cycle over positions
        for (Location location : locations) {
            if (!isFirst) {
                urlWithParameters += "|";
            } else {
                isFirst = false;
            }
            urlWithParameters += location.getLat() + "," + location.getLng();
        }
        // add trailing
        urlWithParameters += "&sensor=" + Boolean.toString(hasPositionSensor);
        return urlWithParameters;
    }

    protected ElevationResponse unmarshalResponse(Reader reader)
            throws GeocodeException {
        try {
            JAXBContext context = JAXBContext
                    .newInstance(ElevationResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            ElevationResponse response = (ElevationResponse) unmarshaller
                    .unmarshal(reader);
            reader.close();
            return response;
        } catch (JAXBException ex) {
            throw new GeocodeException("JAXBException unmarshalling xml",
                    ex.getCause());
        } catch (IOException ex) {
            throw new GeocodeException("IOException closing http stream",
                    ex.getCause());
        }

    }

    public ElevationResponse getElevations(List<Location> locations)
            throws GeocodeException {
        // url for GET
        String url = buildUrl(locations);
        // fetch the xml from google & unmarshall it
        URL request;
        try {
            request = new URL(url);
        } catch (MalformedURLException ex) {
            throw new GeocodeException("MalformedURLException generating URL",
                    ex.getCause());
        }
        try {
            URLConnection connection = request.openConnection();
            ElevationResponse response = unmarshalResponse(new InputStreamReader(
                    connection.getInputStream()));
            if (response.getStatus() != Status.OK) {
                throw new GeocodeException(
                        "Server side error in the response: "
                                + response.getStatus());
            }
            return response;
        } catch (IOException ex) {
            throw new GeocodeException(
                    "IOException connecting to GoogleElevation Web Service",
                    ex.getCause());
        }
    }

    public ElevationResponse getElevation(Location location)
            throws GeocodeException {
        List<Location> locations = new ArrayList<Location>();
        locations.add(location);
        return getElevations(locations);
    }

    public boolean isHasPositionSensor() {
        return hasPositionSensor;
    }

    public void setHasPositionSensor(boolean hasPositionSensor) {
        this.hasPositionSensor = hasPositionSensor;
    }
}
