package org.shampoo.goldenembed.tools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
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
	private String token;

	public Strava(List<GoldenCheetah> gcArray, String email, String password, String rideDate) {
		login(email, password);
		upload(gcArray, rideDate);
	}

	//public static void main(String[] args) {
	//	new Strava();
//	}
	
	public String getTokenFromJSON(String json)
	{
		int start = json.indexOf("token");
		start += 8;
		token = json.substring(start, json.indexOf(',') -1);
		return token;
		
	}
	
	private void login(String email, String password)
	{
		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("email", email);
		formData.add("password", password);
		formData.add("agreed_to_terms", "true");

		System.out.println(formData);
		ClientResponse response = webResource.path("authentication")
				.path("login").type("application/x-www-form-urlencoded")
				.post(ClientResponse.class, formData);
		String reply = response.getEntity(String.class);
		String token = getTokenFromJSON(reply);
		
		System.out.println("Token: " +token);

	}
	
	private void upload(List<GoldenCheetah> gcArray, String rideDate)
	{
		
		ArrayList<String> list = new ArrayList<String>();
		list.add("time");
		list.add("latitude");
		list.add("longitude");
		list.add("elevation");
		list.add("watts");
		list.add("cadence");
		list.add("heartrate");
		
		MultivaluedMapImpl formData = new MultivaluedMapImpl();
		formData.add("token", token);
		formData.add("type", "json");
		formData.add("data_fields", list);
		
		ArrayList<String> data;
		for (GoldenCheetah gc : gcArray) {
			data = new ArrayList<String>();
			data.add(formatDate(gc, rideDate));
			data.add(gc.getLatitude());
			data.add(gc.getLongitude());
			data.add(String.valueOf(gc.getElevation()));
			data.add(String.valueOf(gc.getWatts()));
			data.add(String.valueOf(gc.getCad()));
			data.add(String.valueOf(gc.getHr()));
			formData.add("data", data);
			break;
		}
		
		System.out.println(formData);
		ClientResponse response = webResource.path("upload")
				.type("application/x-www-form-urlencoded")
				.post(ClientResponse.class, formData);
		System.out.println(response.getEntity(String.class));
		
	}
	
	public String formatDate(GoldenCheetah gc, String rideDate)
	{
		Calendar rideCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		
		int year = Integer.parseInt(rideDate.substring(0, 4));
		int month = Integer.parseInt(rideDate.substring(5, 7));
		int day = Integer.parseInt(rideDate.substring(8, 10));
		
		int hours = (int)gc.getSecs() / 3600,
		remainder = (int)gc.getSecs() % 3600,
		minutes = remainder / 60,
		seconds = remainder % 60;

        rideCal.set(year, month, day, hours, minutes, seconds);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        String formattedDate = sdf.format(rideCal.getTime());

        return formattedDate;
	}
}