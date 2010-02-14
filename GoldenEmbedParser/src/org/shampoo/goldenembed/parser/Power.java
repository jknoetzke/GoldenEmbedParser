package org.shampoo.goldenembed.parser;

public class Power
{
     private double watts;
     private double nm;
     private double rpm;
     private double p;
     private double t;
     private double r;
     private double v; //SRM
     private double totalWattCounter;
     private double totalCadCounter;

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
     boolean first12 = true;
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
     public boolean isFirst12() {
          return first12;
     }
     public void setFirst12(boolean first12) {
          this.first12 = first12;
     }



}
