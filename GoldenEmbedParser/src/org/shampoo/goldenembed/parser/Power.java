package org.shampoo.goldenembed.parser;

public class Power {
    private double watts;
    private double nm;
    private double rpm;
    private double p;
    private double t;
    private double r;
    private double v; // SRM
    private double totalWattCounter;
    private double totalCadCounter;
    public boolean first12 = false;
    public boolean first0x20 = false;

    public boolean isFirst0x20() {
        return first0x20;
    }

    public void setFirst0x20(boolean first0x20) {
        this.first0x20 = first0x20;
    }

    public boolean isFirst12() {
        return first12;
    }

    public void setFirst12(boolean first12) {
        this.first12 = first12;
    }

    public double getTotalCadCounter() {
        return totalCadCounter;
    }

    public void setTotalCadCounter(double totalCadCounter) {
        this.totalCadCounter = totalCadCounter;
    }

    public double getV() {
        return v;
    }

    public double getTotalWattCounter() {
        return totalWattCounter;
    }

    public void setTotalWattCounter(double totalWattCounter) {
        this.totalWattCounter = totalWattCounter;
    }

    public void setV(double v) {
        this.v = v;
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public double getWatts() {
        return watts;
    }

    public void setWatts(double watts) {
        this.watts = watts;
    }

    public double getNm() {
        return nm;
    }

    public void setNm(double nm) {
        this.nm = nm;
    }

    public double getRpm() {
        return rpm;
    }

    public void setRpm(double rpm) {
        this.rpm = rpm;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    boolean first0x11 = true; // For 0x11 messages

    public boolean isFirst0x11() {
        return first0x11;
    }

    public void setFirst0x11(boolean first0x11) {
        this.first0x11 = first0x11;
    }

    boolean first0x12 = true; // For 0x12 messages

    public boolean isFirst0x12() {
        return first0x12;
    }

    public void setFirst0x12(boolean first0x12) {
        this.first0x12 = first0x12;
    }

    private double cnt;

    public double getCnt() {
        return cnt;
    }

    public void setCnt(double cnt) {
        this.cnt = cnt;
    }

}
