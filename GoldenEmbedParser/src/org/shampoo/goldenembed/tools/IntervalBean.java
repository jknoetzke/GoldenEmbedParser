package org.shampoo.goldenembed.tools;

public class IntervalBean {

    long startInterval;
    long endInterval;
    long watts;
    int hr;
    long cad;
    long duration;
    long speed;
    String latitude;
    String longitude;
    float elevation;

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
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

    public float getElevation() {
        return elevation;
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
    }

    public long getStartInterval() {
        return startInterval;
    }

    public void setStartInterval(long startInterval) {
        this.startInterval = startInterval;
    }

    public long getEndInterval() {
        return endInterval;
    }

    public void setEndInterval(long endInterval) {
        this.endInterval = endInterval;
    }

    public long getWatts() {
        return watts;
    }

    public void setWatts(long watts) {
        this.watts = watts;
    }

    public int getHr() {
        return hr;
    }

    public void setHr(int hr) {
        this.hr = hr;
    }

    public long getCad() {
        return cad;
    }

    public void setCad(long cad) {
        this.cad = cad;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
