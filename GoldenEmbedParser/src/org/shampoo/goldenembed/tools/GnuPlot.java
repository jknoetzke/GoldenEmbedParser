package org.shampoo.goldenembed.tools;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.shampoo.goldenembed.parser.GoldenCheetah;

public class GnuPlot {

	private final static String SPACE = " ";

	public GnuPlot() {
	}

	public void writeOutGnuPlot(List<GoldenCheetah> gcArray, String outFile) {
		Iterator<GoldenCheetah> iter = gcArray.iterator();
		GoldenCheetah gc;
		PrintWriter fout = initPlotFile(outFile);

		int totalWatts = 0;
		int count = 0;
		int totalHr = 0;
		int totalCad = 0;
		int totalSpeed = 0;
		int totalDistance = 0;
		// Open up the file..

		while (iter.hasNext()) {
			gc = iter.next();

			if (gc.getSecs() != 0) {

				fout.write(convertToTime(gc.getSecs()) + SPACE
						+ (totalWatts / count) + SPACE + (totalHr / count)
						+ SPACE + (totalCad / count) + SPACE
						+ (totalSpeed / count) + SPACE
						+ (totalDistance / count) + "\n");

				totalWatts = 0;
				count = 0;
				totalHr = 0;
				totalCad = 0;
				totalSpeed = 0;
				totalDistance = 0;

			}

			totalWatts += gc.getWatts();
			totalHr += gc.getHr();
			totalCad += gc.getCad();
			totalSpeed += gc.getSpeed();
			totalDistance += gc.getDistance();
			count++;

		}
		fout.close();
	}

	private PrintWriter initPlotFile(String outFile) {
		PrintWriter fout;
		try {
			fout = new PrintWriter(new FileOutputStream(outFile));
			return fout;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String convertToTime(long secsIn) {
		long hours = secsIn / 3600, remainder = secsIn % 3600, minutes = remainder / 60, seconds = remainder % 60;

		return ((hours < 10 ? "0" : "") + hours + ":"
				+ (minutes < 10 ? "0" : "") + minutes + ":"
				+ (seconds < 10 ? "0" : "") + seconds);

	}
}
