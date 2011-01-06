package org.shampoo.goldenembed.parser;

public class GoldenCheetah {
    // <sample cad="0" watts="0" secs="0" hr="92" len="1" />

    private long cad;
    private int watts;
    private long secs;
    private long prevsecs;
    private long prevWattsecs;
    private long prevSpeedSecs;
    private long prevCadSecs;
    private long prevHRSecs;
    private double distance;
    private double speed;
    public boolean newWatts = true;
    private String latitude;
    private String longitude;
    private int hr;
    private int len;

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
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

    public int getWatts() {
        return watts;
    }

    public void setWatts(int watts) {
        this.watts = watts;
    }

    public long getSecs() {
        return secs;
    }

    public void setSecs(long secs) {
        this.secs = secs;
    }

    public long getPrevsecs() {
        return prevsecs;
    }

    public void setPrevsecs(long prevsecs) {
        this.prevsecs = prevsecs;
    }

    public long getPrevWattsecs() {
        return prevWattsecs;
    }

    public void setPrevWattsecs(long prevWattsecs) {
        this.prevWattsecs = prevWattsecs;
    }

    public long getPrevSpeedSecs() {
        return prevSpeedSecs;
    }

    public void setPrevSpeedSecs(long prevSpeedSecs) {
        this.prevSpeedSecs = prevSpeedSecs;
    }

    public long getPrevCadSecs() {
        return prevCadSecs;
    }

    public void setPrevCadSecs(long prevCadSecs) {
        this.prevCadSecs = prevCadSecs;
    }

    public long getPrevHRSecs() {
        return prevHRSecs;
    }

    public void setPrevHRSecs(long prevHRSecs) {
        this.prevHRSecs = prevHRSecs;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
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

    public GoldenCheetah clone(GoldenCheetah _gc) {
        GoldenCheetah gc = new GoldenCheetah();
        gc.setCad(_gc.getCad());
        gc.setDistance(_gc.getDistance());
        gc.setHr(_gc.getHr());
        gc.setLen(_gc.getLen());
        gc.setPrevCadSecs(_gc.getPrevCadSecs());
        gc.setPrevHRSecs(_gc.getPrevHRSecs());
        gc.setPrevsecs(_gc.getPrevsecs());
        gc.setPrevSpeedSecs(_gc.getPrevSpeedSecs());
        gc.setPrevWattsecs(_gc.getPrevWattsecs());
        gc.setSecs(_gc.getSecs());
        gc.setSpeed(_gc.getSpeed());
        gc.setWatts(_gc.getWatts());

        return gc;
    }
}
