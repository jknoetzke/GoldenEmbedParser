package org.shampoo.goldenembed.elevation;

import it.units.GoogleCommon.GeocodeException;
import it.units.GoogleCommon.Location;
import it.units.GoogleElevation.ElevationRequestor;
import it.units.GoogleElevation.ElevationResponse;
import it.units.GoogleElevation.Result;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.shampoo.goldenembed.parser.GoldenCheetah;
import org.shampoo.goldenembed.tools.Wait;

public class GoogleElevation {

	private Map<Location, Float> elevations = new HashMap<Location, Float>();

	public Map<Location, Float> getElevations() {
		return elevations;
	}

	public GoogleElevation() {

	}

	public float getElevation(Float lat, Float lon) {

		Location location = new Location(lat, lon);
		if (!elevations.containsKey(location)) {

			ElevationRequestor requestor = new ElevationRequestor();
			ElevationResponse elevationResponse;
			try {
				elevationResponse = requestor.getElevation(location);
				List<Result> resultList = elevationResponse.getResults();
				float elevation = resultList.get(0).getElevation();
				elevations.put(location, elevation);
				return elevation;
			} catch (GeocodeException ex) {
				ex.printStackTrace();
				return 0;
			}
		} else
			return elevations.get(location);

	}

	public List<Result> parseMultipleLocations(List<Location> locations) {
		ElevationRequestor requestor = new ElevationRequestor();
		ElevationResponse elevationResponse;
		try {
			elevationResponse = requestor.getElevations(locations);
			List<Result> resultList = elevationResponse.getResults();
			return resultList;

		} catch (GeocodeException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public List<GoldenCheetah> getGCElevations(List<GoldenCheetah> gcArray) {

		getSerializedElevations();
		Iterator<GoldenCheetah> iter = gcArray.iterator();
		List<Location> locations = new ArrayList<Location>();
		Location location;
		GoldenCheetah gc;
		int count = 0;
		int totalCounter = 0;

		while (iter.hasNext()) {
			gc = iter.next();
			location = new Location(Float.parseFloat(gc.getLatitude()),
					Float.parseFloat(gc.getLongitude()));
			if (!elevations.containsKey(location)) {
				locations.add(location);
				elevations.put(location, null);
				count++;
				if (count == 50) // Maximum that Google allows.
				{
					List<Result> results = parseMultipleLocations(locations);
					parseResults(results);
					if (++totalCounter % 20 == 0)
						Wait.oneSec();
					locations.clear();
					count = 0;
				}
			}
		}
		// Call with what is left..
		if (count != 0) {
			List<Result> results = parseMultipleLocations(locations);
			parseResults(results);
		}
		serializeElevations();

		// Now go and store the elevations for GC
		GoldenCheetah _gc;
		Iterator<GoldenCheetah> iter2 = gcArray.iterator();
		List<GoldenCheetah> gcReturnArray = new ArrayList<GoldenCheetah>();
		while (iter2.hasNext()) {
			_gc = iter2.next();
			location = new Location(Float.parseFloat(_gc.getLatitude()),
					Float.parseFloat(_gc.getLongitude()));
			Float elevation = elevations.get(location);
			gc = _gc.clone(_gc);
			gc.setElevation(elevation);
			gcReturnArray.add(gc);

		}

		return gcReturnArray;

	}

	private void parseResults(List<Result> results) {

		Iterator<Result> iterResults = results.iterator();
		while (iterResults.hasNext()) {
			Result result = iterResults.next();
			elevations.put(result.getLocation(), result.getElevation());
		}
	}

	public void serializeElevations() {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("elevations.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(elevations);
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Map<Location, Float> getSerializedElevations() {

		FileInputStream fis;
		try {
			fis = new FileInputStream("elevations.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			elevations = (HashMap<Location, Float>) ois.readObject();
			ois.close();

			return elevations;

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

}
