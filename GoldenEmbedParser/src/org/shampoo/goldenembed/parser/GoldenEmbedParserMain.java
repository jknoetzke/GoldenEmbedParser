/*
 * Copyright (c) 2009 Justin F. Knotzke (jknotzke@shampoo.ca)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.shampoo.goldenembed.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.shampoo.goldenembed.tools.GnuPlot;
import org.shampoo.goldenembed.tools.Intervals;

public class GoldenEmbedParserMain {

	static final byte MESG_RESPONSE_EVENT_ID = 0x40;
	static final byte MESG_CAPABILITIES_ID = 0x54;
	static final byte MESG_BROADCAST_DATA_ID = 0x4E;
	static final byte MESG_TX_SYNC = (byte) 0xA4;
	static final byte MESG_CHANNEL_SEARCH_TIMEOUT_ID = 0x44;
	static final byte MESG_ASSIGN_CHANNEL_ID = 0x42;
	static final byte MESG_CHANNEL_RADIO_FREQ_ID = 0x45;
	static final byte MESG_CHANNEL_MESG_PERIOD_ID = 0x43;
	static final byte MESG_OPEN_CHANNEL_ID = (byte) 0x4B;
	static final byte MESG_CHANNEL_ID_ID = (byte) 0x51;
	static final byte MESG_NETWORK_KEY_ID = 0x46;
	static final byte MESG_CHANNEL_EVENT_ERROR = 0x01;
	static final double PI = 3.14159265;

	float totalTrans = 0;
	float totalErrors = 0;
	boolean errorFlag = false;
	long totalSpikes = 0;
	int lastWattSecs = 0; // To keep track of the last time watts were saved.

	private static final String spacer1 = "    ";
	private static final String spacer2 = "        ";

	List<GoldenCheetah> gcArray = new ArrayList<GoldenCheetah>();
	String outFilePath = null;

	Power power;
	SpeedCad speedCad;

	boolean debug = false;
	boolean megaDebug = false;

	PrintWriter fout;
	Options options = new Options();

	String outGCFilePath;
	String outGnuPlotPath;
	String intervalParam;

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		new GoldenEmbedParserMain(args);
	}

	public static int unsignedByteToInt(byte b) {
		return b & 0xFF;
	}

	public GoldenEmbedParserMain() {
		power = new Power();
	}

	public void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar GoldenEmbedParser.jar", options);
	}

	private void initGCFile() {
		fout.write("<!DOCTYPE GoldenCheetah>\n");
		fout.write("<ride>\n");
		fout.write(spacer1 + "<attributes>\n");
		fout.write(spacer2
				+ "<attribute key=\"Start time\" value=\"2010/01/01 00:00:00 UTC\" />\n");
		fout.write(spacer2
				+ "<attribute key=\"Device type\" value=\"Golden Embed\" />\n");
		fout.write(spacer1 + "</attributes>\n");
		fout.write("<samples>\n");
	}

	private File createGCFileName() {

		Calendar cal = Calendar.getInstance();
		int date = cal.get(Calendar.MONTH);
		date++;

		String strMonth = formatDate(date);
		date = cal.get(Calendar.DAY_OF_MONTH);
		String strDay = formatDate(date);

		date = cal.get(Calendar.HOUR_OF_DAY);
		String strHour = formatDate(date);

		date = cal.get(Calendar.MINUTE);
		String strMin = formatDate(date);

		date = cal.get(Calendar.SECOND);
		String strSec = formatDate(date);

		File outFile = new File(outGCFilePath + "/" + cal.get(Calendar.YEAR)
				+ "_" + strMonth + "_" + strDay + "_" + strHour + "_" + strMin
				+ "_" + strSec + ".gc");

		return outFile;

	}

	private void createGCOutFile() {

		File outFile = createGCFileName();
		try {
			fout = new PrintWriter(new FileOutputStream(outFile));
			initGCFile();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(1);
		}

	}

	private String formatDate(int toFormat) {
		String forMatted;
		forMatted = new Integer(toFormat).toString();
		if (forMatted.length() < 2)
			forMatted = "0" + forMatted;

		return forMatted;

	}

	public GoldenEmbedParserMain(String[] args) {

		Option inputFile = OptionBuilder.withArgName("inputfile").hasArg()
				.withDescription("Input file GoldenEmbedGPS")
				.create("inputfile");

		Option outputGCFile = OptionBuilder
				.withArgName("outgc")
				.hasArg()
				.withDescription(
						"Directory of where to write the Golden Cheetah File")
				.create("outgc");

		Option outputGnuPlotFile = OptionBuilder
				.withArgName("outgnuplot")
				.hasArg()
				.withDescription("Directory of where to write the gnuplot file")
				.create("outgnuplot");

		Option debugOption = OptionBuilder.withArgName("debug").hasArg()
				.withDescription("Level of debug").create("debug");

		Option intervalOption = OptionBuilder.withArgName("interval").hasArg()
				.withDescription("Interval Format: MM:SS+MM ex: 62:00+20")
				.create("interval");

		options.addOption(inputFile);
		options.addOption(outputGCFile);
		options.addOption(outputGnuPlotFile);
		options.addOption(debugOption);
		options.addOption(intervalOption);

		// create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			// Load up the file
			File file = null;
			power = new Power();
			speedCad = new SpeedCad();

			// has the buildfile argument been passed?
			if (line.hasOption("debug")) {
				// initialise the member variable
				String strDebug = line.getOptionValue("debug");
				if (strDebug.equalsIgnoreCase("debug"))
					debug = true;
				else if (strDebug.equalsIgnoreCase("megaDebug"))
					megaDebug = true;
			}

			if (!line.hasOption("inputfile")) {
				printUsage();
				System.exit(1);
			} else
				file = new File(line.getOptionValue("inputfile"));

			if (line.hasOption("outgc"))
				outGCFilePath = line.getOptionValue("outgc");

			if (line.hasOption("outgnuplot"))
				outGnuPlotPath = line.getOptionValue("outgnuplot");

			if (line.hasOption("interval"))
				intervalParam = line.getOptionValue("interval");

			// Load up the file
			power = new Power();
			speedCad = new SpeedCad();
			GoldenCheetah gc = new GoldenCheetah();

			System.out.println("Input File: " + file.getAbsolutePath());

			try {
				ANTrxHandler(getBytesFromFile(file), gc);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ParseException exp) {
			// oops, something went wrong
			System.out.println("Parsing failed.  Reason: " + exp.getMessage());
			printUsage();
		} catch (SecurityException e) {
			System.out.println(e);
		}

	}

	private int ANTrxMsg(byte[] rxIN, int i, int size, GoldenCheetah gc) {
		if (megaDebug)
			System.out.println("Converting 0x"
					+ UnicodeFormatter.byteToHex(rxIN[i]));
		switch (rxIN[i]) {
		case MESG_RESPONSE_EVENT_ID:
			if (debug)
				System.out.println("ID: MESG_RESPONSE_EVENT_ID\n");
			i = ANTresponseHandler(rxIN, i, size, gc);
			break;
		case MESG_CAPABILITIES_ID:
			if (debug)
				System.out.println("ID: MESG_CAPABILITIES_ID\n");
			i = ANTCfgCapabilties(i, size); // rxBuf[3] .. skip sync, size, msg
			break;
		case MESG_BROADCAST_DATA_ID:
			// if (debug)
			// System.out.println("ID: MESG_BROADCAST_DATA_ID\n");
			Byte aByte = new Byte(rxIN[++i]);
			int chan = aByte.intValue();
			if (chan == 0)
				i = ANTparseHRM(rxIN, i + 2, gc);
			else if (chan == 1)
				i = ANTParsePower(rxIN, ++i, size, gc);
			else if (chan == 2)
				i = ANTParseSpeedCad(rxIN, ++i, size, gc);

			if (gc.getPrevsecs() != gc.getSecs()) {
				gc.setWatts((int) Round(
						power.getWatts() / power.getTotalWattCounter(), 0));
				gc.setCad((int) Round(
						power.getRpm() / power.getTotalCadCounter(), 0));

				GoldenCheetah _gc = gc.clone(gc);
				gcArray.add(_gc);
				gc.setPrevsecs(gc.getSecs());
				gc.newWatts = false;
			}
			break;

		case MESG_CHANNEL_ID_ID:
			if (debug)
				System.out.println("ID: MESG_CHANNEL_ID_ID\n");
			i = ANTChannelID(rxIN, ++i, gc);
			break;
		default:
			if (debug)
				System.out.println("ID: Unknown 0x"
						+ UnicodeFormatter.byteToHex(rxIN[i]));
		}
		return i;
	}

	public int ANTChannelID(byte[] msgIN, int pos, GoldenCheetah gc) {
		byte[] devNo = new byte[2];

		pos += 2;
		devNo[0] = msgIN[pos];
		if (megaDebug)
			System.out.println("Device Type is: 0x"
					+ UnicodeFormatter.byteToHex(msgIN[pos]));

		pos--;
		devNo[1] = msgIN[pos];
		if (megaDebug)
			System.out.println("Device Type is: 0x"
					+ UnicodeFormatter.byteToHex(msgIN[pos]));

		int deviceNum = byteArrayToInt(devNo, 0, 2);
		if (debug)
			System.out.println("Device Number is: " + deviceNum);

		pos += 2;
		if (debug)
			System.out.println("Device Type is: 0x"
					+ UnicodeFormatter.byteToHex(msgIN[pos]));
		pos++;
		if (debug)
			System.out.println("Man ID is: 0x"
					+ UnicodeFormatter.byteToHex(msgIN[pos]) + "\n");

		pos += 2;
		pos = setTimeStamp(msgIN, pos, gc, false);

		return --pos;
	}

	public int setTimeStamp(byte[] msgData, int i, GoldenCheetah gc,
			boolean write) {
		Byte hr;
		Byte min;
		Byte sec;

		// Print out the time stamp
		hr = new Byte(msgData[i++]);
		min = new Byte(msgData[i++]);
		sec = new Byte(msgData[i++]);

		if (write)
			gc.setSecs((hr * 60 * 60) + (min * 60) + sec);

		if (debug)
			System.out.println("Time stamp: " + hr.intValue() + ":"
					+ min.intValue() + ":" + sec.intValue());

		return i;
	}

	public byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		// You cannot create an array using a long type.
		// It needs to be an int type.
		// Before converting to an int type, check
		// to ensure that file is not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		if (megaDebug)
			System.out.println("Total Bytes: " + bytes.length);
		return bytes;
	}

	private int ANTParseSpeedCad(byte[] msgData, int i, int size,
			GoldenCheetah gc) {

		int crankrev = 0;
		int cranktime = 0;
		int wheelrev = 0;
		int wheeltime = 0;

		byte[] byteArray = new byte[2];
		byteArray[1] = msgData[i++];
		byteArray[0] = msgData[i++];
		cranktime = byteArrayToInt(byteArray, 0, 2);

		byteArray[1] = msgData[i++];
		byteArray[0] = msgData[i++];
		crankrev = byteArrayToInt(byteArray, 0, 2);

		byteArray[1] = msgData[i++];
		byteArray[0] = msgData[i++];
		wheeltime = byteArrayToInt(byteArray, 0, 2);

		byteArray[1] = msgData[i++];
		byteArray[0] = msgData[i++];
		wheelrev = byteArrayToInt(byteArray, 0, 2);

		boolean timeStampRead = false;

		if (speedCad.getCrankrev() != crankrev) {
			if (!speedCad.isFirst12Cad()) {
				double crankTimeDelta = (65536 + cranktime - speedCad
						.getCranktime()) % 65536;
				double crankRevDelta = (65536 + crankrev - speedCad
						.getCrankrev()) % 65536;
				double cad = 1024.0 * 60.0 * crankRevDelta / crankTimeDelta;

				if (debug)
					System.out.println("cadence: " + cad);
				cad = Math.round(cad);
				i = setTimeStamp(msgData, ++i, gc, true);
				timeStampRead = true;

				speedCad.setCrankrev(crankrev);
				speedCad.setCranktime(cranktime);

			} else {
				speedCad.setCrankrev(crankrev);
				speedCad.setCranktime(cranktime);
				speedCad.setFirst12Cad(false);
				i = setTimeStamp(msgData, ++i, gc, false);
				timeStampRead = true;
			}
		}

		if (speedCad.getWheelrev() != wheelrev) {
			if (!speedCad.isFirst12Speed()) {

				if (megaDebug)
					System.out.println("WheelTime: " + wheeltime);
				if (megaDebug)
					System.out.println("LastWheelTime: "
							+ speedCad.getWheeltime());
				double wheelTimeDelta = Math.abs(wheeltime
						- speedCad.getWheeltime());

				if (megaDebug)
					System.out.println("WheelTimeDelta: " + wheelTimeDelta);
				wheelTimeDelta = (65536 + wheeltime - speedCad.getWheeltime()) % 65536;

				if (megaDebug)
					System.out.println("wheelrev: " + wheelrev);
				if (megaDebug)
					System.out.println("LastWheelRev: "
							+ speedCad.getWheelrev());

				double wheelRevDelta = Math.abs(wheelrev
						- speedCad.getWheelrev());
				wheelRevDelta = (65536 + wheelrev - speedCad.getWheelrev()) % 65536;
				if (megaDebug)
					System.out.println("wheelRevDelta: " + wheelRevDelta);

				double rpm = 1024.0 * 60.0 * wheelRevDelta / wheelTimeDelta;
				double speed = 60.0 * 1024.0 * 60.0 * speedCad.wheelCirc
						* wheelRevDelta / 1000 / 1000 / wheelTimeDelta;

				if (debug)
					System.out.println("rpm: " + rpm);
				if (debug)
					System.out.println("Speed: " + speed);
				if (!timeStampRead) {
					i = setTimeStamp(msgData, ++i, gc, true);
					timeStampRead = true;
				}

				gc.setSpeed(speed);
				if (gc.getPrevSpeedSecs() != gc.getSecs()) {
					gc.setDistance(gc.getDistance()
							+ (speed * (gc.getSecs() - gc.getPrevSpeedSecs()) / 3600.0));
					if (debug)
						System.out.println("Distance: " + gc.getDistance());
					gc.setPrevSpeedSecs(gc.getSecs());
				}
				speedCad.setWheelrev(wheelrev);
				speedCad.setWheeltime(wheeltime);
			} else {
				if (!timeStampRead) {
					i = setTimeStamp(msgData, ++i, gc, false);
					timeStampRead = true;
				}
				speedCad.setWheelrev(wheelrev);
				speedCad.setWheeltime(wheeltime);
				speedCad.setFirst12Speed(false);
			}
		}

		if (!timeStampRead)
			i = setTimeStamp(msgData, ++i, gc, false);

		return --i; // For Loop will advance itself.

	}

	private int ANTParsePower(byte[] msgData, int i, int size, GoldenCheetah gc) {
		if (megaDebug)
			System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i]));
		if (msgData[i] == 0x12) // Parse ANT+ 0x12 message (QUARQ)
		{
			i += 2;
			return i = ANTParsePower0x12(msgData, i, size, gc);
		} else if (msgData[i] == 0x11) // Parse ANT+ 0x11 messages (PowerTap)
		{
			i++;
			return i = ANTParsePower0x11(msgData, i, size, gc);
		} else {
			i = i + 2;
			return i + size;
		}
	}

	public int ANTParsePower0x11(byte[] msgData, int i, int size,
			GoldenCheetah gc) {
		// For parsing ANT+ 0x11 messages
		// Counter (u8) Unknown (u8) Cadence (u8) Wheel_RPM_Counter (u16)
		// Torque_Counter (u16)
		// Cnt_diff = (Last Counter - Current Counter + 256) mod 256
		// Torque_diff = (Last Torque - Current Torque + 65536) mod 65536
		// NM = Torque_diff / 32 / Cnt_diff
		// Wheel_RPM_diff = (Last Wheel RPM - Current Wheel_RPM + 65536) mod
		// 65536
		// Power = 122880 * Cnt_diff / Wheel_RPM_diff

		int c1, c2, pr1, t1, r1;

		double cdiff = 0;
		double tdiff = 0;
		double rdiff = 0;
		double nm = 0;
		double rpm = 0;
		double watts = 0;

		byte aByte;
		byte[] byteArray = new byte[2];

		// 0x11 Message counter
		aByte = msgData[i++];
		c1 = unsignedByteToInt(aByte);

		// Unknown counter
		aByte = msgData[i++];
		c2 = unsignedByteToInt(aByte);

		// Pedal RPM (cadence) value
		aByte = msgData[i++];
		pr1 = unsignedByteToInt(aByte);

		// Wheel RPM counter
		byteArray[1] = msgData[i++];
		byteArray[0] = msgData[i++];
		r1 = byteArrayToInt(byteArray, 0, 2);

		// Torque counter
		byteArray[1] = msgData[i++];
		byteArray[0] = msgData[i++];
		t1 = byteArrayToInt(byteArray, 0, 2);

		// System.out.println("c1: " + c1 + " c2: " + c2 + " pr1: " + pr1 +
		// " t1: " + t1 + " r1: " + r1 + "\n");

		if (power.first0x11) {
			power.first0x11 = false;
			power.setR(r1);
			power.setT(t1);
			power.setCnt(c1);
			i = setTimeStamp(msgData, ++i, gc, false);
		} else if (c1 != power.getCnt()) {
			cdiff = ((256 + c1 - power.getCnt()) % 256);
			tdiff = (65536 + t1 - power.getT()) % 65536;
			rdiff = (65536 + r1 - power.getR()) % 65536;

			if (tdiff != 0 && rdiff != 0) {
				nm = (float) tdiff / 32 / (float) cdiff;
				rpm = 122880 * (float) cdiff / (float) rdiff;
				watts = rpm * nm * 2 * PI / 60;

				if (debug) {
					System.out
							.format("ANTParsePower0x11 cad: %3d  nm: %5.2f  rpm: %5.2f  watts: %6.1f",
									pr1, nm, rpm, watts);
					System.out.println();
				}
				i = setTimeStamp(msgData, ++i, gc, true);

				if (rpm < 10000 && watts < 10000) {
					if (gc.newWatts == false) {
						power.setTotalWattCounter(0);
						power.setTotalCadCounter(0);
						power.setRpm(0);
						power.setWatts(0);
						gc.newWatts = true;
					}
					gc.setPrevCadSecs(gc.getSecs());
					power.setRpm(power.getRpm() + pr1);
					power.setWatts(power.getWatts() + watts);
					double wattCounter = power.getTotalWattCounter();
					double cadCounter = power.getTotalCadCounter();
					power.setTotalWattCounter(wattCounter + 1);
					power.setTotalCadCounter(cadCounter + 1);
					gc.setPrevWattsecs(gc.getSecs());
					gc.setPrevCadSecs(gc.getSecs());
				} else {
					if (debug)
						System.out.println("Spike Found: cdiff: " + cdiff
								+ " rdiff: " + rdiff + " tdiff: " + tdiff
								+ "\n");
					totalSpikes++;
				}
			} else
				i = setTimeStamp(msgData, ++i, gc, false);
		} else
			i = setTimeStamp(msgData, ++i, gc, false);

		power.setR(r1);
		power.setT(t1);
		power.setCnt(c1);

		return --i; // For Loop will advance itself.
	}

	public int ANTParsePower0x12(byte[] msgData, int i, int size,
			GoldenCheetah gc) {
		int t1;
		int p1;
		int r1;

		int end = i + size - 2;
		double rdiff = 0;
		double pdiff = 0;
		double tdiff = 0;
		double nm = 0;
		double rpm = 0;
		double watts = 0;
		Byte aByte;
		int msgN = 0;

		for (; i < end; i++) {
			if (megaDebug)
				System.out.println("0x"
						+ UnicodeFormatter.byteToHex(msgData[i]));
			if (msgN == 0) {
				if (power.first0x12) {
					// Just store it.
					aByte = new Byte(msgData[i]);
					power.setR(aByte.intValue());
					if (megaDebug)
						System.out.println("R: " + aByte.intValue());
				} else {
					// We can calculate and then store
					aByte = new Byte(msgData[i]);
					r1 = aByte.intValue();
					rdiff = (r1 + 256 - power.getR()) % 256;
					power.setR(r1);
					if (megaDebug)
						System.out.println("rdiff is: " + rdiff);
				}
				msgN++;
			} else if (msgN == 1) {
				byte[] pRdiff = new byte[2];
				i++;
				pRdiff[1] = msgData[i];
				if (megaDebug)
					System.out.println("0x"
							+ UnicodeFormatter.byteToHex(msgData[i]) + "\n");
				i++;
				pRdiff[0] = msgData[i];
				if (megaDebug)
					System.out.println("0x"
							+ UnicodeFormatter.byteToHex(msgData[i]) + "\n");
				p1 = byteArrayToInt(pRdiff, 0, 2);

				if (power.first0x12) {
					power.setP(p1);
					if (megaDebug)
						System.out.println("P1: " + p1);
				} else {
					pdiff = (65536 + p1 - power.getP()) % 65536;
					power.setP(p1);
					if (megaDebug)
						System.out.println("pdiff is: " + pdiff);
				}
				msgN++;
			} else if (msgN == 2) {
				byte[] pRdiff = new byte[2];
				pRdiff[1] = msgData[i];
				if (megaDebug)
					System.out.println("0x"
							+ UnicodeFormatter.byteToHex(msgData[i]) + "\n");
				i++;
				pRdiff[0] = msgData[i];
				if (megaDebug)
					System.out.println("0x"
							+ UnicodeFormatter.byteToHex(msgData[i]) + "\n");

				t1 = byteArrayToInt(pRdiff, 0, 2);

				if (power.first0x12) {
					power.setT(t1);
					if (megaDebug)
						System.out.println("T: " + t1);
				} else {
					tdiff = (t1 + 65536 - power.getT()) % 65536;
					power.setT(t1);
					if (megaDebug)
						System.out.println("tdiff is: " + tdiff);
				}

				i++;
				msgN++;
			}
		}
		if (tdiff != 0 && rdiff != 0) {
			nm = (float) tdiff / ((float) rdiff * 32.0);
			rpm = rdiff * 122880.0 / pdiff;
			watts = rpm * nm * 2 * PI / 60;

			if (debug)
				System.out.println("ANTParsePower0x12 nm: " + nm + " rpm: "
						+ rpm + " watts: " + watts + "\n");
			i = setTimeStamp(msgData, i, gc, true);

			if (rpm < 10000 && watts < 10000) {
				if (gc.newWatts == false) {
					power.setTotalWattCounter(0);
					power.setTotalCadCounter(0);
					power.setRpm(0);
					power.setWatts(0);
					gc.newWatts = true;
				}

				power.setRpm(power.getRpm() + rpm);
				power.setWatts(power.getWatts() + watts);
				double wattCounter = power.getTotalWattCounter();
				double cadCounter = power.getTotalCadCounter();
				power.setTotalWattCounter(wattCounter + 1);
				power.setTotalCadCounter(cadCounter + 1);

				flushPowerArray(gc);
			} else {
				if (debug)
					System.out.println("Spike Found: pdiff: " + pdiff
							+ " rdiff: " + rdiff + " tdiff: " + tdiff + "\n");
				totalSpikes++;
			}
		} else {
			i = setTimeStamp(msgData, i, gc, false);
		}
		if (power.first0x12)
			power.first0x12 = false;

		if (megaDebug)
			System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])
					+ "\n");

		return --i; // For Loop will advance itself.

	}

	private int ANTparseHRM(byte[] msgData, int i, GoldenCheetah gc) {

		byte aByte;
		int end = i + 8;
		int hrCountFinder = 0;
		int hr = 0;

		for (; i < end; i++) {
			aByte = msgData[i];
			if (megaDebug)
				System.out.println("Converting 0x"
						+ UnicodeFormatter.byteToHex(msgData[i]));
			if (hrCountFinder == 6) { // HR is the sixth byte
				if (megaDebug)
					System.out.println("Converting 0x"
							+ UnicodeFormatter.byteToHex(msgData[i]));
				hr = unsignedByteToInt(aByte);
				if (debug)
					System.out.println("Heart Rate is: " + hr);
			} else if (megaDebug)
				System.out.println("o" + i + "=" + unsignedByteToInt(aByte));
			hrCountFinder++;
		}

		i = setTimeStamp(msgData, i, gc, true);
		gc.setHr(hr);
		gc.setPrevHRSecs(gc.getSecs());

		return --i; // For Loop will advance itself.
	}

	private void ANTrxHandler(byte[] rxBuf, GoldenCheetah gc) {
		int msgN = 0;
		int i;
		int size = 0;
		boolean inMsg = true;

		for (i = 0; i < rxBuf.length; i++) {
			if (rxBuf[i] == MESG_TX_SYNC && inMsg) {
				inMsg = false;
				msgN = 0; // Always reset msg count if we get a sync
				msgN++;
				errorFlag = false;
				totalTrans++;
				if (megaDebug)
					System.out.println("RX: [sync]");
			} else if (msgN == 1) {
				Byte aByte = new Byte(rxBuf[i]);
				msgN++; // Size
				size = aByte.intValue();
			} else {
				if (rxBuf.length <= size + i + 1) {
					System.out.println("\nTotal Failed Checksums: : "
							+ totalErrors + "Out of Total ANT Messages: "
							+ totalTrans);
					System.out.println("% Failure: "
							+ (totalErrors / totalTrans) * 100.0);
					System.out.println("Total CAD or Watt Spikes: "
							+ totalSpikes);
					writeOutGCRecords();
					System.exit(0); // EOF
				}
				byte checksum = checkSum(rxBuf, size, i - 2);
				if (checksum == rxBuf[size + i + 1]) // Check if chksum = msg
				// chksum
				{
					inMsg = true;
					// Handle Message
					i = ANTrxMsg(rxBuf, i, size, gc);
				} else {
					if (megaDebug)
						System.out.println("CheckSum Mismatch 0x"
								+ UnicodeFormatter
										.byteToHex(rxBuf[size + i + 1])
								+ "!=: 0x"
								+ UnicodeFormatter.byteToHex(checksum));
					msgN = 0;
					inMsg = true;
					if (errorFlag == false) {
						totalErrors++;
						errorFlag = true;
					}
				}
			}
		}
		System.out.println("\n\nTotal Failed Checksums: " + totalErrors
				+ " Out of Total ANT Messages: " + totalTrans);
		System.out.println("% Failure: " + (totalErrors / totalTrans) * 100.0);
		System.out.println("Total CAD or Watt Spikes: " + totalSpikes);
		writeOutGCRecords();

	}

	private void closeGCFile() {
		fout.write(spacer1 + "</samples>\n");
		fout.write("</ride>\n");
		fout.flush();
		fout.close();
	}

	private byte checkSum(byte data[], int length, int pos) {

		byte chksum = 0x0;

		if (pos < 0)
			return chksum;

		for (int i = pos; i < length + 3 + pos; i++) {
			if (megaDebug)
				System.out.println("Checksum: 0x"
						+ UnicodeFormatter.byteToHex(data[i]));
			chksum ^= data[i]; // +1 since skip prefix sync code, we already
			// counted it
		}

		return chksum;
	}

	private int ANTresponseHandler(byte rxBuf[], int pos, int size,
			GoldenCheetah gc) {
		pos++;
		byte ch = rxBuf[0 + pos];
		byte id = rxBuf[1 + pos];
		byte code = rxBuf[2 + pos];

		if (debug) {
			System.out.println("Channel Num:" + UnicodeFormatter.byteToHex(ch));
			System.out.println("Message ID: " + UnicodeFormatter.byteToHex(id));
			System.out.println("Code: " + UnicodeFormatter.byteToHex(code));
		}

		switch (id) {
		case MESG_CHANNEL_SEARCH_TIMEOUT_ID:
			if (debug)
				System.out.println("[MESG_CHANNEL_SEARCH_TIMEOUT_ID]");
			break;
		case MESG_ASSIGN_CHANNEL_ID:
			if (debug)
				System.out.println("[MESG_ASSIGN_CHANNEL_ID]");
			break;
		case MESG_CHANNEL_RADIO_FREQ_ID:
			if (debug)
				System.out.println("[MESG_CHANNEL_RADIO_FREQ_ID]");
			break;
		case MESG_CHANNEL_MESG_PERIOD_ID:
			if (debug)
				System.out.println("[MESG_CHANNEL_MESG_PERIOD_ID]");
			break;
		case MESG_OPEN_CHANNEL_ID:
			if (debug)
				System.out.println("[MESG_OPEN_CHANNEL_ID]");
			break;
		case MESG_CHANNEL_ID_ID:
			if (debug)
				System.out.println("[MESG_CHANNEL_ID_ID]");
			break;
		case MESG_NETWORK_KEY_ID:
			if (debug)
				System.out.println("[MESG_NETWORK_KEY_ID]");
			break;
		case MESG_CHANNEL_EVENT_ERROR:
			if (code == 0x01) {
				if (ch == 0)
					System.out.println("Dropped HRM");
				else if (ch == 1)
					System.out.println("Dropped Power");
				else if (ch == 2)
					System.out.println("Dropped Cadence/Speed");
			}
			break;
		default:
			if (debug)
				System.out.println("[unknown]: "
						+ UnicodeFormatter.byteToHex(id));
			break;
		}

		pos = setTimeStamp(rxBuf, pos + 4, gc, false);

		return --pos; // For Loop will move 1 forward
	}

	private int ANTCfgCapabilties(int i, int size) {
		return i + size + 4;
	}

	public static int byteArrayToInt(byte[] b, int offset, int size) {
		uShort uint = new uShort(b, offset);
		return uint.getValue();
	}

	private void writeGCRecord(GoldenCheetah gc) {

		if (fout == null)
			createGCOutFile();

		fout.write(spacer1 + "<sample cad=\"" + gc.getCad() + "\" watts=\""
				+ gc.getWatts() + "\" kph=\"" + Round(gc.getSpeed(), 1)
				+ "\" km=\"" + Round(gc.getDistance(), 2) + "\" secs=\""
				+ gc.getSecs() + "\" hr=\"" + gc.getHr() + "\" len=\"1\"/>\n");
	}

	public static double Round(double Rval, int Rpl) {
		double p = Math.pow(10, Rpl);
		Rval = Rval * p;
		double tmp = Math.round(Rval);
		return tmp / p;
	}

	private void flushPowerArray(GoldenCheetah gc) {
		GoldenCheetah _gc;
		// Now create GC file records for the missing messages.

		if (gcArray.size() == 0)
			return;
		int startSecs = lastWattSecs;
		int endSecs = gc.getSecs(); // The next time we are about to save.
		int diffSecs = endSecs - startSecs;
		int watts = 0;
		long cad = 0;

		if (diffSecs >= 3) // Let's no be ridiculous.
		{
			watts = 0;
			cad = 0;
		} else if (diffSecs != 0) {
			watts = gc.getWatts() / diffSecs;
			cad = gc.getCad() / diffSecs;
		} else {
			watts = gc.getWatts();
			cad = gc.getCad();
		}
		for (int x = startSecs; x < endSecs; x++) {
			_gc = findGCByTime(x); // Do we already have a GC record for this
									// time ?
			if (_gc != null) {
				GoldenCheetah tmpGC = new GoldenCheetah();
				tmpGC = _gc.clone(_gc);
				tmpGC.setCad(cad);
				tmpGC.setWatts(watts);
				gcArray.set(gcArray.indexOf(_gc), tmpGC);
			} else {
				_gc = new GoldenCheetah();
				_gc = _gc.clone(gc);
				_gc.setSecs(x);
				_gc.setWatts(watts);
				_gc.setCad(cad);
				gcArray.add(_gc);
			}
		}
		lastWattSecs = endSecs + 1;

	}

	private void writeOutGCRecords() {
		Collections.sort(gcArray, new SortBySeconds());

		if (outGCFilePath != null) {
			Iterator<GoldenCheetah> iter = gcArray.iterator();
			while (iter.hasNext()) {
				GoldenCheetah _gc = iter.next();
				writeGCRecord(_gc);
				closeGCFile();
			}
		}

		if (outGnuPlotPath != null) {
			GnuPlot plot = new GnuPlot();
			plot.writeOutGnuPlot(gcArray, outGnuPlotPath);
		}

		if (intervalParam != null) {
			Intervals interval = new Intervals();
			interval.createInterval(gcArray, intervalParam);
		}
	}

	private GoldenCheetah findGCByTime(int secs) {
		Iterator<GoldenCheetah> iter = gcArray.iterator();
		GoldenCheetah _gc;
		while (iter.hasNext()) {
			_gc = iter.next();
			if (_gc.getSecs() == secs)
				return _gc;
		}

		return null;
	}

}
