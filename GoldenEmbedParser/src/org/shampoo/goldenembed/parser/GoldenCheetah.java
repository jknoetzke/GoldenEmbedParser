/*
 * Copyright (c) 2009 Justin F. Knotzke (jknotzke@shampoo.ca)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.shampoo.goldenembed.parser;


public class GoldenCheetah {

    private long cad;
    private long watts;
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
    private String date;
    private float elevation;
    private String description;
    private String name;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getElevation() {
        return elevation;
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

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

    public long getWatts() {
        return watts;
    }

    public void setWatts(long watts) {
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
        if (latitude == null)
            return;
        if (latitude.length() >= 8)
            this.latitude = latitude.substring(0, 7);
        else
            this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        if (longitude == null)
            return;
        if (longitude.length() >= 9)
            this.longitude = longitude.substring(0, 8);
        else
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
        gc.setLatitude(_gc.getLatitude());
        gc.setLongitude(_gc.getLongitude());
        gc.setElevation(_gc.getElevation());

        return gc;
    }
}
