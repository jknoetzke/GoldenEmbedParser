package org.shampoo.goldenembed.tools;

import java.util.Iterator;
import java.util.List;

import org.shampoo.goldenembed.parser.GoldenCheetah;

public class Intervals {

    public Intervals() {

    }

    public void createInterval(List<GoldenCheetah> gcArray, String params) {

	GoldenCheetah gc;

	int totalParams = occurances(" ", params);
	totalParams++;
	int veryStart = 0;
	int end = 0;
	int veryEnd = 0;
	if (totalParams != 1)
	    veryEnd = params.indexOf(" ");
	else
	    veryEnd = params.length();
	int intervalTime = 0;

	for (int i = 0; i < totalParams; i++) {

	    String currentParams = params.substring(veryStart, veryEnd);
	    end = currentParams.indexOf("+", 0);

	    long secs = createSecsFromParams(currentParams.substring(0, end));

	    intervalTime = Integer.parseInt(currentParams.substring(
		    currentParams.indexOf("+") + 1, currentParams.length())) * 60;

	    long totalWatts = 0;
	    long totalHr = 0;
	    long totalCad = 0;
	    long totalSpeed = 0;
	    int count = 0;
	    Iterator<GoldenCheetah> iter = gcArray.iterator();

	    while (iter.hasNext()) {
		gc = iter.next();
		if (gc.getSecs() >= secs && gc.getSecs() <= intervalTime + secs) {
		    totalWatts += gc.getWatts();
		    totalHr += gc.getHr();
		    totalCad += gc.getCad();
		    totalSpeed += gc.getSpeed();
		    count++;
		}
	    }

	    System.out.println("");
	    System.out.print("Watts = " + (totalWatts / count));
	    System.out.print(" Cadence = " + (totalHr / count));
	    System.out.println(" Hr = " + (totalCad / count));

	    veryStart = veryEnd + 1;
	    veryEnd = params.indexOf(" ", veryStart);
	    if (veryEnd == -1)
		veryEnd = params.length();
	}

    }

    private int occurances(String toSearch, String params) {
	int result = 0;
	int start = params.indexOf(toSearch);
	while (start != -1) {
	    result++;
	    start = params.indexOf(toSearch, start + 1);
	}
	return result;
    }

    private long createSecsFromParams(String params) {
	long secs = 0;

	int hr = 0;
	int min = 0;
	int sec = 0;
	int result = occurances(":", params);

	if (result == 0)
	    return Integer.parseInt(params);
	else if (result == 1) {
	    min = Integer.parseInt(params.substring(0, params.indexOf(":")));
	    sec = Integer.parseInt(params.substring((params.indexOf(":") + 1),
		    params.length()));

	    return (min * 60) + sec;
	} else if (result == 2) {

	    hr = Integer.parseInt(params.substring(0, params.indexOf(":")));
	    min = Integer.parseInt(params.substring((params.indexOf(":") + 1),
		    params.indexOf(":") - 1));
	    sec = Integer.parseInt(params.substring((params.indexOf(":") + 1),
		    params.length()));

	    return (hr * 60 * 60) + (min * 60) + sec;
	}

	return secs;
    }
}
