package org.shampoo.goldenembed.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

	public Strava(List<GoldenCheetah> gcArray) {
		login();
		upload(gcArray);
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
	
	private void login()
	{
		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("email", "jknotzke@shampoo.ca");
		formData.add("password", "qoOB2JDz");
		formData.add("agreed_to_terms", "true");

		ClientResponse response = webResource.path("authentication")
				.path("login").type("application/x-www-form-urlencoded")
				.post(ClientResponse.class, formData);
		String reply = response.getEntity(String.class);
		String token = getTokenFromJSON(reply);
		
		System.out.println("Token: " +token);

	}
	
	private void upload(List<GoldenCheetah> gcArray)
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
		
		ArrayList<String> data = new ArrayList<String>();
		for (GoldenCheetah gc : gcArray) {
			data.add(String.valueOf(gc.getSecs()));
			data.add(gc.getLatitude());
			data.add(gc.getLongitude());
			data.add(String.valueOf(gc.getElevation()));
			data.add(String.valueOf(gc.getWatts()));
			data.add(String.valueOf(gc.getCad()));
			data.add(String.valueOf(gc.getHr()));
		}
		
		formData.add("data", data);
		ClientResponse response = webResource.path("upload")
				.type("application/x-www-form-urlencoded")
				.post(ClientResponse.class, formData);
		System.out.println(response.getEntity(String.class));
		
	}
}