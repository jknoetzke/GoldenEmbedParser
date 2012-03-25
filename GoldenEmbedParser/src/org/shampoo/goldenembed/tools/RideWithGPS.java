package org.shampoo.goldenembed.tools;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.shampoo.goldenembed.parser.GoldenCheetah;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class RideWithGPS {

	Client client = Client.create();
	WebResource webResource = client.resource("http://ridewithgps.com");

	public RideWithGPS(List<GoldenCheetah> gcArray, String email,
			String password, String rideDate) {
		upload("", gcArray, rideDate, email, password);
	}

	@SuppressWarnings("unchecked")
	private void upload(String token, List<GoldenCheetah> gcArray,
			String rideDate, String email, String password) {

		JSONObject formData = new JSONObject();
		JSONObject data;
		JSONArray dataArray = new JSONArray();
		for (GoldenCheetah gc : gcArray) {
			data = new JSONObject();
			data.put("x", Float.parseFloat(gc.getLongitude()));
			data.put("y", Float.parseFloat(gc.getLatitude()));
			data.put("t", formatDate(gc, rideDate));
			data.put("e", gc.getElevation());
			data.put("p", (float) gc.getWatts());
			data.put("c", (float) gc.getCad());
			data.put("h", (float) gc.getHr());
			data.put("s", (float) (gc.getSpeed() * 1000) % 3600);
			data.put("d", (float) (gc.getDistance() * 1000));
			dataArray.add(data);
		}
		formData.put("track_points", dataArray.toJSONString());
		formData.put("apikey", "p24n3a9e");
		formData.put("email", email);
		formData.put("password", password);

		ClientResponse response = webResource.path("trips.json")
				.type("application/json")
				.post(ClientResponse.class, formData.toString());

		System.out.println(response.getEntity(String.class));

	}

	public long formatDate(GoldenCheetah gc, String rideDate) {
		Calendar rideCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

		int year = Integer.parseInt(rideDate.substring(0, 4));
		int month = Integer.parseInt(rideDate.substring(5, 7));
		int day = Integer.parseInt(rideDate.substring(8, 10));

		int hours = (int) gc.getSecs() / 3600, remainder = (int) gc.getSecs() % 3600, minutes = remainder / 60, seconds = remainder % 60;

		rideCal.set(year, --month, ++day, hours, minutes, seconds);
		return rideCal.getTimeInMillis() / 1000;

	}
}
