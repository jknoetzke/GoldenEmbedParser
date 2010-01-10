package org.shampoo.goldenembed.parser;

public class GoldenCheetah 
{
	//<sample cad="0" watts="0" secs="0" hr="92" len="1" />

	private int cad;
	private int watts;
	private int secs;
	private int hr;
	private int len;
	private int channel;
	
	public int getChannel() {
		return channel;
	}
	public void setChannel(int channel) {
		this.channel = channel;
	}
	public int getCad() {
		return cad;
	}
	public void setCad(int cad) {
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
