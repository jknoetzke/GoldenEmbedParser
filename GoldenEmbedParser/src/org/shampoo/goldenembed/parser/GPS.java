package org.shampoo.goldenembed.parser;

public class GPS {
	private String date;
	private String time;
	private String latitude = "";
	private String longitude = "";
	private double speed;
	private float elevation;

	public float getElevation() {
		return elevation;
	}

	public void setElevation(float elevation) {
		this.elevation = elevation;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
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

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

}
