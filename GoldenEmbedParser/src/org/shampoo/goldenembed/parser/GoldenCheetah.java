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
     private int prevHRSecs;
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
     public int getPrevHRSecs() {
          return prevHRSecs;
     }
     public void setPrevHRSecs(int prevHRSecs) {
          this.prevHRSecs = prevHRSecs;
     }

     public GoldenCheetah clone(GoldenCheetah _gc)
     {
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
