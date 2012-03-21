package org.shampoo.goldenembed.tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.shampoo.goldenembed.json.JSONArray;
import org.shampoo.goldenembed.json.JSONException;
import org.shampoo.goldenembed.json.JSONObject;
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
		try {
			formData.put("email", email);

			formData.put("password", password);
			formData.put("agreed_to_terms", "true");

			ClientResponse response = webResourceSSL.path("authentication")
					.path("login").type("application/json")
					.post(ClientResponse.class, formData.toString());
			String reply = response.getEntity(String.class);

			String token = getTokenFromJSON(reply);
			System.out.println("Strava Token is: " + token);
			return token;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private void upload(String token, List<GoldenCheetah> gcArray,
			String rideDate) {

		try {
			JSONObject formData = new JSONObject();
			JSONArray data_fields = new JSONArray();
			JSONArray data = new JSONArray();

			formData.put("data", data_fields);
			formData.put("type", "json");
			formData.put("token", token);
			formData.put("activity_name", getNameFromDate(rideDate));

			data_fields.put("time");
			data_fields.put("latitude");
			data_fields.put("longitude");
			data_fields.put("elevation");
			data_fields.put("watts");
			data_fields.put("cadence");
			data_fields.put("heartrate");
			data_fields.put("h_accuracy");
			data_fields.put("v_accuracy");

			formData.put("data_fields", data_fields);

			JSONArray dataArray;
			for (GoldenCheetah gc : gcArray) {
				dataArray = new JSONArray();
				dataArray.put(formatDate(gc, rideDate));
				dataArray.put(Float.parseFloat(gc.getLatitude()));
				dataArray.put(Float.parseFloat(gc.getLongitude()));
				dataArray.put(gc.getElevation());
				dataArray.put((float) gc.getWatts());
				dataArray.put((float) gc.getCad());
				dataArray.put((float) gc.getHr());
				dataArray.put(5);
				dataArray.put(5);
				data.put(dataArray);

			}

			formData.put("data", data);
			ClientResponse response = webResource.path("upload")
					.type("application/json")
					.post(ClientResponse.class, formData.toString());

			System.out.println(response.getEntity(String.class));

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String formatDate(GoldenCheetah gc, String rideDate) {
		Calendar rideCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

		int year = Integer.parseInt(rideDate.substring(0, 4));
		int month = Integer.parseInt(rideDate.substring(5, 7));
		int day = Integer.parseInt(rideDate.substring(8, 10));

		int hours = (int) gc.getSecs() / 3600, remainder = (int) gc.getSecs() % 3600, minutes = remainder / 60, seconds = remainder % 60;

		rideCal.set(year, month, day, hours, minutes, seconds);

		SimpleDateFormat sdf = new SimpleDateFormat(
				"yyyy-MM-dd'T'hh:mm:ss.SSSZ");
		String formattedDate = sdf.format(rideCal.getTime());

		return formattedDate;
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