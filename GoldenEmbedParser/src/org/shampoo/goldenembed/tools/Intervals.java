package org.shampoo.goldenembed.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.shampoo.goldenembed.parser.GoldenCheetah;

public class Intervals {

    private long watts;
    private long speed;
    private int hr;
    private long cad;
    private long startInterval;
    private long endInterval;
    private long duration;
    private String latitude;
    private String longitude;
    private float elevation;

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

    public long getWatts() {
        return watts;
    }

    public void setWatts(long watts) {
        this.watts = watts;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Intervals() {

    }

    public List<Intervals> createInterval(List<GoldenCheetah> gcArray,
            String params) {

        GoldenCheetah gc = new GoldenCheetah();
        List<Intervals> intervals = new ArrayList<Intervals>();

        int totalParams = occurances(" ", params);
        totalParams++;
        int veryStart = 0;
        int end = 0;
        int veryEnd = 0;
        if (totalParams != 1)
            veryEnd = params.indexOf(" ");
        else
            veryEnd = params.length();
        int intervalTime = 0;

        for (int i = 0; i < totalParams; i++) {

            String currentParams = params.substring(veryStart, veryEnd);
            end = currentParams.indexOf("+", 0);

            long secs = createSecsFromParams(currentParams.substring(0, end));

            intervalTime = Integer.parseInt(currentParams.substring(
                    currentParams.indexOf("+") + 1, currentParams.length())) * 60;

            long totalWatts = 0;
            long totalHr = 0;
            long totalCad = 0;
            long totalSpeed = 0;
            String intervalLat = "";
            String intervalLon = "";
            float intervalElevation = 0;
            int count = 0;
            Iterator<GoldenCheetah> iter = gcArray.iterator();

            while (iter.hasNext()) {
                gc = iter.next();
                if (gc.getSecs() >= secs && gc.getSecs() <= intervalTime + secs) {
                    totalWatts += gc.getWatts();
                    totalHr += gc.getHr();
                    totalCad += gc.getCad();
                    totalSpeed += gc.getSpeed();
                    intervalLat = gc.getLatitude();
                    intervalLon = gc.getLongitude();
                    intervalElevation = gc.getElevation();
                    if (gc.getSecs() == secs)
                        startInterval = gc.getSecs();
                    if (gc.getSecs() == intervalTime + secs)
                        endInterval = gc.getSecs();
                    count++;
                }
                setDuration(endInterval - startInterval);
            }

            System.out.println("");
            System.out.print("Watts = " + (totalWatts / count));
            System.out.print(" Cadence = " + (totalCad / count));
            System.out.print(" Hr = " + (totalHr / count));
            System.out.print(" Speed = " + (totalSpeed / count));
            System.out.print(" Duration = " + secondsToString(getDuration()));

            setWatts(totalWatts / count);
            setCad(totalCad / count);
            setHr((int) totalHr / count);
            setSpeed(totalSpeed / count);
            setLatitude(intervalLat);
            setLongitude(intervalLon);
            setElevation(intervalElevation);

            intervals.add(this);

            veryStart = veryEnd + 1;
            veryEnd = params.indexOf(" ", veryStart);
            if (veryEnd == -1)
                veryEnd = params.length();
        }
        System.out.println("");

        return intervals;
    }

    private int occurances(String toSearch, String params) {
        int result = 0;
        int start = params.indexOf(toSearch);
        while (start != -1) {
            result++;
            start = params.indexOf(toSearch, start + 1);
        }
        return result;
    }

    private long createSecsFromParams(String params) {
        long secs = 0;

        int hr = 0;
        int min = 0;
        int sec = 0;
        int result = occurances(":", params);

        if (result == 0)
            return Integer.parseInt(params);
        else if (result == 1) {
            min = Integer.parseInt(params.substring(0, params.indexOf(":")));
            sec = Integer.parseInt(params.substring((params.indexOf(":") + 1),
                    params.length()));

            return (min * 60) + sec;
        } else if (result == 2) {

            hr = Integer.parseInt(params.substring(0, params.indexOf(":")));
            min = Integer.parseInt(params.substring((params.indexOf(":") + 1),
                    params.indexOf(":") - 1));
            sec = Integer.parseInt(params.substring((params.indexOf(":") + 1),
                    params.length()));

            return (hr * 60 * 60) + (min * 60) + sec;
        }

        return secs;
    }

    public static String secondsToString(long time) {
        int seconds = (int) (time % 60);
        int minutes = (int) ((time / 60) % 60);
        int hours = (int) ((time / 3600) % 24);
        String secondsStr = (seconds < 10 ? "0" : "") + seconds;
        String minutesStr = (minutes < 10 ? "0" : "") + minutes;
        String hoursStr = (hours < 10 ? "0" : "") + hours;
        return new String(hoursStr + ":" + minutesStr + ":" + secondsStr);
    }
}
