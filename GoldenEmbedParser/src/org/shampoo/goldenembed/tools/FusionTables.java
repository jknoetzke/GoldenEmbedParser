package org.shampoo.goldenembed.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.shampoo.goldenembed.parser.GPS;
import org.shampoo.goldenembed.parser.GoldenCheetah;

import com.google.api.client.util.Strings;
import com.google.gdata.client.ClientLoginAccountType;
import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequest.RequestType;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.ServiceException;

/**
 * Java example using the Google Fusion Tables API to query, insert, update, and
 * delete. Uses the Google GDATA core library.
 * 
 * @author googletables-feedback@google.com (Google Fusion Tables Team)
 */

public class FusionTables {

    String tableId;

    /**
     * Google Fusion Tables API URL stem. All requests to the Google Fusion
     * Tables server begin with this URL.
     * 
     * The next line is Google Fusion Tables API-specific code:
     */
    private static final String SERVICE_URL = "https://www.google.com/fusiontables/api/query";

    /**
     * Handle to the authenticated Google Fusion Tables service.
     * 
     * This code uses the GoogleService class from the Google GData APIs Client
     * Library.
     */
    private GoogleService service;

    /**
     * Two versions of ApiExample() are provided: one that accepts a Google user
     * account ID and password for authentication, and one that accepts an
     * existing auth token.
     */

    /**
     * Authenticates the given account for {@code fusiontables} service using a
     * given email ID and password.
     * 
     * @param email
     *            Google account email. (For more information, see
     *            http://www.google.com/support/accounts.)
     * @param password
     *            Password for the given Google account.
     * 
     *            This code instantiates the GoogleService class from the Google
     *            GData APIs Client Library, passing in Google Fusion Tables
     *            API-specific parameters. It then goes back to the Google GData
     *            APIs Client Library for the setUserCredentials() method.
     */
    public FusionTables(String email, String password) {
        try {
            service = new GoogleService("fusiontables",
                    "fusiontables.ApiExample");
            service.setUserCredentials(email, password,
                    ClientLoginAccountType.GOOGLE);

        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Authenticates for {@code fusiontables} service using the auth token. The
     * auth token can be retrieved for an authenticated user by invoking
     * service.getAuthToken() on the email and password. The auth token can be
     * reused rather than specifying the user name and password repeatedly.
     * 
     * @param authToken
     *            The auth token. (For more information, see
     *            http://code.google.com/apis/gdata/auth.html#ClientLogin.)
     * 
     * @throws AuthenticationException
     * 
     *             This code instantiates the GoogleService class from the
     *             Google Data APIs Client Library, passing in Google Fusion
     *             Tables API-specific parameters. It then goes back to the
     *             Google Data APIs Client Library for the setUserToken()
     *             method.
     */
    public FusionTables(String authToken) throws AuthenticationException {
        service = new GoogleService("fusiontables", "fusiontables.ApiExample");
        service.setUserToken(authToken);
    }

    /**
     * Fetches the results for a select query. Prints them to standard output,
     * surrounding every field with (@code |}.
     * 
     * This code uses the GDataRequest class and getRequestFactory() method from
     * the Google Data APIs Client Library. The Google Fusion Tables
     * API-specific part is in the construction of the service URL. A Google
     * Fusion Tables API SELECT statement will be passed in to this method in
     * the selectQuery parameter.
     */
    private void runSelect(String selectQuery) throws IOException,
            ServiceException {
        URL url = new URL(SERVICE_URL + "?sql="
                + URLEncoder.encode(selectQuery, "UTF-8"));
        GDataRequest request = service.getRequestFactory().getRequest(
                RequestType.QUERY, url, ContentType.TEXT_PLAIN);

        request.execute();
    }

    /**
     * Executes insert, update, and delete statements. Prints out results, if
     * any.
     * 
     * This code uses the GDataRequest class and getRequestFactory() method from
     * the Google Data APIs Client Library to construct a POST request. The
     * Google Fusion Tables API-specific part is in the use of the service URL.
     * A Google Fusion Tables API INSERT, UPDATE, or DELETE statement will be
     * passed into this method in the updateQuery parameter.
     */

    private void runUpdate(String updateQuery) throws IOException,
            ServiceException {
        URL url = new URL(SERVICE_URL);
        GDataRequest request = service.getRequestFactory().getRequest(
                RequestType.INSERT, url,
                new ContentType("application/x-www-form-urlencoded"));
        OutputStreamWriter writer = new OutputStreamWriter(
                request.getRequestStream());
        writer.append("sql=" + URLEncoder.encode(updateQuery, "UTF-8"));
        writer.flush();

        request.execute();
        InputStream response = request.getResponseStream();
        byte[] result = new byte[1024];
        response.read(result);
        String s = Strings.fromBytesUtf8(result);
        String[] lines = s.split(Strings.LINE_SEPARATOR);
        if (lines[0].equals("tableid")) {
            tableId = lines[1];
        }

    }

    private void createNewTable(String name) throws IOException,
            ServiceException {
        String query = "CREATE TABLE '"
                + sqlEscape(name)
                + "' (name:STRING,description:STRING,time:DATETIME,watts:NUMBER,hr:NUMBER,speed:NUMBER,cadence:NUMBER,elevation:NUMBER, geometry:LOCATION,interval:STRING)";
        runUpdate(query);
    }

    private static String sqlEscape(String value) {
        return value.replaceAll("'", "''");
    }

    public void uploadToFusionTables(String description,
            List<GoldenCheetah> gcArray, String name,
            List<IntervalBean> gcIntervals) {
        int counter = 0;
        IntervalBean gcInterval = null;
        StringBuffer strArray = new StringBuffer();
        int interval = 0;
        int smoothCounter = 1;
        long totalWatts = 0;
        long totalCad = 0;
        int totalHr = 0;
        long totalSpeed = 0;
        List<GPS> gpsArray = new ArrayList<GPS>();
        GPS gps;

        // Get the total time to smooth
        long totalSecs = gcArray.get(gcArray.size() - 1).getSecs();
        long smoothFactor = totalSecs / 200;

        if (!gcIntervals.isEmpty())
            gcInterval = gcIntervals.get(0);

        try {
            createNewTable(name);
            for (GoldenCheetah gc : gcArray) {
                gps = new GPS();
                if (gc.getSecs() % smoothFactor == 0) {
                    GoldenCheetah gcOut = new GoldenCheetah();
                    gcOut.setWatts(totalWatts / smoothCounter);
                    gcOut.setCad(totalCad / smoothCounter);
                    gcOut.setSecs(gc.getSecs());
                    gcOut.setHr(totalHr / smoothCounter);
                    gcOut.setSpeed(totalSpeed / smoothCounter);
                    gcOut.setElevation(gc.getElevation());

                    strArray.append(createNewLineString(description, name,
                            gcOut, gpsArray));
                    counter++;
                    if (gpsArray.size() != 0)
                        gps = gpsArray.get(gpsArray.size() - 1); // For
                                                                 // continuation
                    gpsArray.clear();
                    gpsArray.add(gps);

                    smoothCounter = 1;
                    totalWatts = 0;
                    totalCad = 0;
                    totalHr = 0;
                    totalSpeed = 0;

                    if (counter >= 100) {
                        runUpdate(strArray.toString());
                        strArray = new StringBuffer();
                        counter = 0;
                    }

                } else {
                    smoothCounter++;
                    totalWatts += gc.getWatts();
                    totalCad += gc.getCad();
                    totalHr += gc.getHr();
                    totalSpeed += gc.getSpeed();
                    gps.setElevation(gc.getElevation());
                    gps.setLatitude(gc.getLatitude());
                    gps.setLongitude(gc.getLongitude());
                    gpsArray.add(gps);
                }

                // Check if we have intervals
                if (gcInterval != null) {
                    if (gc.getSecs() == gcInterval.getStartInterval()
                            || gc.getSecs() == gcInterval.getEndInterval()) {
                        String intervalInfo;
                        if (gc.getSecs() == gcInterval.getStartInterval()) {
                            intervalInfo = "Start: "
                                    + Intervals.secondsToString(gcInterval
                                            .getStartInterval());
                        } else {
                            intervalInfo = "End: "
                                    + Intervals.secondsToString(gcInterval
                                            .getEndInterval())
                                    + " Time: "
                                    + Intervals.secondsToString(gcInterval
                                            .getDuration()) + " Watts: "
                                    + gcInterval.getWatts() + " HR: "
                                    + gcInterval.getHr() + " Speed: "
                                    + gcInterval.getSpeed();
                        }
                        createNewPoint(name, getDescriptionString(gcInterval),
                                gcInterval, intervalInfo, gc.getSecs()); // Start

                        System.out.print(interval);

                        if (gc.getSecs() == gcInterval.getEndInterval()) {
                            interval++;
                            if (gcIntervals.size() > interval) {
                                gcInterval = gcIntervals.get(interval);
                            } else
                                gcInterval = null;
                        }
                    }
                }
            }
            if (counter != 0) // Flush out what is left
                runUpdate(strArray.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new row representing a interval.
     * 
     * @param name
     *            the interval name
     * @param description
     *            the interval description
     * @param the
     *            interval location
     * @return true in case of success.
     * @throws ServiceException
     * @throws IOException
     */
    private void createNewPoint(String name, String description,
            IntervalBean gc, String interval, long pointInTime)
            throws IOException, ServiceException {
        GPS gps = new GPS();
        gps.setLatitude(gc.getLatitude());
        gps.setLongitude(gc.getLongitude());
        gps.setElevation(gc.getElevation());
        String query = "INSERT INTO "
                + tableId
                + " (name,description,time, watts,hr,speed,cadence,elevation,geometry,interval) VALUES "
                + values(name, getDescriptionString(gc),
                        String.valueOf(pointInTime),
                        String.valueOf(gc.getWatts()),
                        String.valueOf(gc.getHr()),
                        String.valueOf(gc.getSpeed()),
                        String.valueOf(gc.getCad()),
                        String.valueOf(gc.getElevation()), getKmlPoint(gps),
                        interval);
        runUpdate(query);
    }

    /**
     * Creates a new row representing a line segment.
     * 
     * @param track
     *            the track/segment to draw
     * @return true in case of success.
     * @throws ServiceException
     * @throws IOException
     */
    private String createNewLineString(String name, String description,
            GoldenCheetah gc, List<GPS> gpsArray) throws IOException,
            ServiceException {
        GPS gps = new GPS();
        gps.setLatitude(gc.getLatitude());
        gps.setLongitude(gc.getLongitude());
        gps.setElevation(gc.getElevation());

        String query = "INSERT INTO "
                + tableId
                + " (name,description,time, watts,hr,speed,cadence,elevation,geometry) VALUES "
                + values(name, description, String.valueOf(gc.getSecs()),
                        String.valueOf(gc.getWatts()),
                        String.valueOf(gc.getHr()),
                        String.valueOf(gc.getSpeed()),
                        String.valueOf(gc.getCad()),
                        String.valueOf(gc.getElevation()),
                        getKmlLineString(gpsArray)) + "; ";
        return query;
    }

    /**
     * Returns a KML Point tag for the given location.
     * 
     * @param location
     *            The location.
     * @return the kml.
     */
    private String getKmlLineString(List<GPS> gpsArray) {
        StringBuilder builder = new StringBuilder("<LineString><coordinates>");
        for (GPS location : gpsArray) {
            appendCoordinate(location, builder);
            builder.append(' ');
        }
        builder.append("</coordinates></LineString>");
        return builder.toString();
    }

    /**
     * Gets a KML Point tag for the given location.
     * 
     * @param location
     *            The location.
     * @return the kml.
     */
    private String getKmlPoint(GPS location) {
        StringBuilder builder = new StringBuilder("<Point><coordinates>");
        appendCoordinate(location, builder);
        builder.append("</coordinates></Point>");
        return builder.toString();
    }

    /**
     * Appends the given location to the string in the format:
     * longitude,latitude[,altitude]
     * 
     * @param location
     *            the location to be added
     * @param builder
     *            the string builder to use
     */
    private void appendCoordinate(GPS location, StringBuilder builder) {
        builder.append(location.getLongitude()).append(",")
                .append(location.getLatitude());
        builder.append(",");
        builder.append(location.getElevation());
    }

    /**
     * Formats given values SQL style. Escapes single quotes with a backslash.
     * 
     * @param values
     *            the values to format
     * @return the values formatted as: ('value1','value2',...,'value_n').
     */
    private static String values(String... values) {
        StringBuilder builder = new StringBuilder("(");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('\'');
            builder.append(sqlEscape(values[i]));
            builder.append('\'');
        }
        builder.append(')');
        return builder.toString();
    }

    private String getDescriptionString(IntervalBean gc) {
        StringBuffer description = new StringBuffer(
                "<p></p><p>Created by <a href='http://chomsky.shampoo.ca/goldenembed'>Golden Embed</a>");
        description.append("<p>Interval Duration: "
                + Intervals.secondsToString(gc.getDuration()));
        description.append("<br>All numbers below are averages<br>");
        return description.toString();
    }
}