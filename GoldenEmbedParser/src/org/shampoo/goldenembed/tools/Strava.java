package org.shampoo.goldenembed.tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import javax.ws.rs.core.MultivaluedMap;

import org.shampoo.goldenembed.parser.GoldenCheetah;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class Strava {

	Client client = Client.create();
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
		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("email", email);
		formData.add("password", password);
		formData.add("agreed_to_terms", "true");

		ClientResponse response = webResource.path("authentication")
				.path("login").type("application/x-www-form-urlencoded")
				.post(ClientResponse.class, formData);
		String reply = response.getEntity(String.class);
		System.out.println(reply);
		return getTokenFromJSON(reply);
	}

	private void upload(String token, List<GoldenCheetah> gcArray,
			String rideDate) {

		HashMap<String, Object> metaMap = new HashMap<String, Object>();

		ArrayList<String> list = new ArrayList<String>();
		list.add("time");
		list.add("latitude");
		list.add("longitude");
		list.add("elevation");
		list.add("watts");
		list.add("cadence");
		list.add("heartrate");

		metaMap.put("token", token);
		metaMap.put("type", "json");
		metaMap.put("data_fields", list);

		ArrayList<String> data;
		ArrayList<List<String>> tmpMap = new ArrayList<List<String>>();

		for (GoldenCheetah gc : gcArray) {
			data = new ArrayList<String>();
			data.add(formatDate(gc, rideDate));
			data.add(gc.getLatitude());
			data.add(gc.getLongitude());
			data.add(String.valueOf(gc.getElevation()));
			data.add(String.valueOf(gc.getWatts()));
			data.add(String.valueOf(gc.getCad()));
			data.add(String.valueOf(gc.getHr()));
			tmpMap.add(data);
		}

		// Hack to format the JSON request in the manner Strava requires.
		StringBuffer strBuf = new StringBuffer(metaMap.toString());
		strBuf.insert(strBuf.length() - 1, " data=" + tmpMap.toString());
		System.out.println(strBuf);

		ClientResponse response = webResource.path("upload")
				.type("application/x-www-form-urlencoded")
				.post(ClientResponse.class, strBuf.toString());
		System.out.println(response.getEntity(String.class));

	}

	public String formatDate(GoldenCheetah gc, String rideDate) {
		Calendar rideCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

		int year = Integer.parseInt(rideDate.substring(0, 4));
		int month = Integer.parseInt(rideDate.substring(5, 7));
		int day = Integer.parseInt(rideDate.substring(8, 10));

		int hours = (int) gc.getSecs() / 3600, remainder = (int) gc.getSecs() % 3600, minutes = remainder / 60, seconds = remainder % 60;

		rideCal.set(year, month, day, hours, minutes, seconds);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		String formattedDate = sdf.format(rideCal.getTime());

		return formattedDate;
	}
}