package org.shampoo.goldenembed.parser;

public class GPS {
    private String date;
    private String time;
    private String latitude = "";
    private String longitude = "";
    private double speed;
    private float elevation;

    public GPS() {

    }

    public float getElevation() {
        return elevation;
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    private byte[] parseOutGPS(byte[] buf, int length, int pos) {
        byte[] position = new byte[length];

        for (int i = 0; i < length; i++) {
            position[i] = buf[pos++];
        }
        return position;

    }

    public GPS GPSHandler(byte[] gpsGGA, int pos) throws NumberFormatException,
            StringIndexOutOfBoundsException {
        GPS gps = new GPS();

        try {

            float degrees = 0;
            float mins = 0;

            byte[] position = parseOutGPS(gpsGGA, 9, pos);
            String strPosition = GoldenEmbedParserMain
                    .convertBytesToString(position);

            pos += 9;

            if (strPosition.startsWith("0")) {
                strPosition = strPosition.replaceFirst("0", "-");
                degrees = Float.parseFloat(strPosition.substring(0, 2));
                mins = Float.parseFloat(strPosition.substring(2,
                        strPosition.length()));
                gps.setLatitude((String.valueOf(-1
                        * (Math.abs(degrees) + (mins / 60)))));
            } else {
                degrees = Float.parseFloat(strPosition.substring(0, 2));
                mins = Float.parseFloat(strPosition.substring(2,
                        strPosition.length()));
                gps.setLatitude(String.valueOf(degrees + (mins / 60)));
            }

            position = parseOutGPS(gpsGGA, 9, pos);
            strPosition = GoldenEmbedParserMain.convertBytesToString(position);
            pos += 9;

            if (strPosition.startsWith("0")) {
                strPosition = strPosition.replaceFirst("0", "-");
                degrees = Float.parseFloat(strPosition.substring(0, 3));
                mins = Float.parseFloat(strPosition.substring(3,
                        strPosition.length()));
                gps.setLongitude((String.valueOf(-1
                        * (Math.abs(degrees) + (mins / 60)))));
            } else {
                degrees = Float.parseFloat(strPosition.substring(0, 2));
                mins = Float.parseFloat(strPosition.substring(2,
                        strPosition.length()));
                gps.setLongitude(String.valueOf(Math.abs(degrees) + (mins / 60)));
            }
            // Speed
            position = parseOutGPS(gpsGGA, 4, pos);
            strPosition = GoldenEmbedParserMain.convertBytesToString(position);
            pos += 4;

            if (strPosition.trim().length() != 0)
                gps.setSpeed(Double.parseDouble(strPosition));
            else
                gps.setSpeed(0.0);
        } catch (NumberFormatException e) {
            throw new NumberFormatException();
        }
        return gps;
    }

}
