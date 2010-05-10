package org.shampoo.goldenembed.parser;

public class GoldenCheetah
{
     //<sample cad="0" watts="0" secs="0" hr="92" len="1" />

     private long cad;
     private int watts;
     private int secs;
     private int prevsecs;
     private int prevWattsecs;
     private int prevSpeedSecs;
     private int prevCadSecs;
     private double distance;
     private double speed;
     public boolean newWatts = true;

     public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public int getPrevCadSecs() {
        return prevCadSecs;
    }
    public void setPrevCadSecs(int prevCadSecs) {
        this.prevCadSecs = prevCadSecs;
    }
    public int getPrevSpeedSecs() {
        return prevSpeedSecs;
    }
    public void setPrevSpeedSecs(int prevSpeedSecs) {
        this.prevSpeedSecs = prevSpeedSecs;
    }
     public double getSpeed() {
        return speed;
    }
    public void setSpeed(double speed) {
        this.speed = speed;
    }
    public int getPrevWattsecs() {
		return prevWattsecs;
	}
	public void setPrevWattsecs(int prevWattsecs) {
		this.prevWattsecs = prevWattsecs;
	}
	public int getPrevsecs() {
          return prevsecs;
     }
     public void setPrevsecs(int prevsecs) {
          this.prevsecs = prevsecs;
     }
     private int hr;
     private int len;

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
     public int getSecs() {
          return secs;
     }
     public void setSecs(int secs) {
          this.secs = secs;
     }
     public int getHr() {
          return hr;
     }
     public void setHr(int hr) {
          this.hr = hr;
     }
     public int getLen() {
          return len;
     }
     public void setLen(int len) {
          this.len = len;
     }


}
