package org.shampoo.goldenembed.parser;

public class SpeedCad
{

    private int crankrev = 0;
    private int cranktime = 0;
    private int wheelrev = 0;
    private int wheeltime = 0;
    private boolean first12Cad = true;
    private boolean first12Speed = true;
    public final double wheelCirc = 2100.0; // wheel circumference in mm
    private double totalSpeed;


    public double getTotalSpeed() {
		return totalSpeed;
	}

	public void setTotalSpeed(double totalSpeed) {
		this.totalSpeed = totalSpeed;
	}

	public int getCrankrev() {
        return crankrev;
    }

    public void setCrankrev(int crankrev) {
        this.crankrev = crankrev;
    }

    public boolean isFirst12Cad() {
        return first12Cad;
    }

    public void setFirst12Cad(boolean first12Cad) {
        this.first12Cad = first12Cad;
    }

    public boolean isFirst12Speed() {
        return first12Speed;
    }

    public void setFirst12Speed(boolean first12Speed) {
        this.first12Speed = first12Speed;
    }

    public int getCranktime() {
        return cranktime;
    }
    public void setCranktime(int cranktime) {
        this.cranktime = cranktime;
    }
    public int getWheelrev() {
        return wheelrev;
    }
    public void setWheelrev(int wheelrev) {
        this.wheelrev = wheelrev;
    }
    public int getWheeltime() {
        return wheeltime;
    }
    public void setWheeltime(int wheeltime) {
        this.wheeltime = wheeltime;
    }

}
