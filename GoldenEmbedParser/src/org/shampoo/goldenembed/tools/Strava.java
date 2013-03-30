package org.shampoo.goldenembed.tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.shampoo.goldenembed.parser.GoldenCheetah;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class Strava {

	Client client = Client.create();
	WebResource webResourceSSL = client
			.resource("https://www.strava.com/api/v2/");
	WebResource webResource = client.resource("http://www.strava.com/api/v2/");

	public Strava(List<GoldenCheetah> gcArray, String email, String password,
			String rideDate) {
		String token = login(email, password);
		upload(token, gcArray, rideDate);
	}

	public String getTokenFromJSON(String json) {
		int start = json.indexOf("token");
		start += 8;
		String token = json.substring(start, json.indexOf(',') - 1);
		return token;

	}

	private String login(String email, String password) {
		JSONObject formData = new JSONObject();
		formData.put("email", email);

		formData.put("password", password);
		formData.put("agreed_to_terms", "true");

		ClientResponse response = webResourceSSL.path("authentication")
				.path("login").type("application/json")
				.post(ClientResponse.class, formData.toString());
		String reply = response.getEntity(String.class);

		String token = getTokenFromJSON(reply);
		System.out.println("Strava Token: " + token);
		return token;
	}

	@SuppressWarnings("unchecked")
	private void upload(String token, List<GoldenCheetah> gcArray,
			String rideDate) {

		JSONObject formData = new JSONObject();
		JSONArray data_fields = new JSONArray();
		JSONArray data = new JSONArray();

		formData.put("data", data_fields);
		formData.put("type", "json");
		formData.put("token", token);
		formData.put("activity_name", getNameFromDate(rideDate));

		data_fields.add("time");
		data_fields.add("latitude");
		data_fields.add("longitude");
		data_fields.add("elevation");
		data_fields.add("watts");
		data_fields.add("cadence");
		data_fields.add("heartrate");
		data_fields.add("h_accuracy");
		data_fields.add("v_accuracy");

		formData.put("data_fields", data_fields);

		JSONArray dataArray;
		for (GoldenCheetah gc : gcArray) {
			dataArray = new JSONArray();
			dataArray.add(formatDate(gc, rideDate));
			dataArray.add(Float.parseFloat(gc.getLatitude()));
			dataArray.add(Float.parseFloat(gc.getLongitude()));
			dataArray.add(gc.getElevation());
			dataArray.add((float) gc.getWatts());
			dataArray.add((float) gc.getCad());
			dataArray.add((float) gc.getHr());
			dataArray.add(5);
			dataArray.add(5);
			data.add(dataArray);

		}

		formData.put("data", data);
		ClientResponse response = webResource.path("upload")
				.type("application/json")
				.post(ClientResponse.class, formData.toString());

		System.out.println(response.getEntity(String.class));

	}

	public String formatDate(GoldenCheetah gc, String rideDate) {

		Date currentDate = new Date(gc.getCurrentTime().getTimeInMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss");
		return sdf.format(currentDate);

	}

	public String getNameFromDate(String rideDate) {
		Calendar rideCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

		int year = Integer.parseInt(rideDate.substring(0, 4));
		int month = Integer.parseInt(rideDate.substring(5, 7));
		int day = Integer.parseInt(rideDate.substring(8, 10));

		rideCal.set(year, --month, day);

		SimpleDateFormat sdf = new SimpleDateFormat("EEE yyyy, MMM d");
		String formattedDate = sdf.format(rideCal.getTime());

		return formattedDate;

	}
}