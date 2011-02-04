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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.shampoo.goldenembed.elevation.GoogleElevation;

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
	static final byte NEW_LINE = (byte) 0x0A;
	static final double PI = 3.14159265;
	static final double KNOTS_TO_KILOMETERS = 1.85200;

	File outFile = null;

	long lastWattSecs = 0; // To keep track of the last time watts were saved.

	boolean isFirstRecordedTime = true;
	long firstRecordedTime = 0;

	float totalTrans = 0;
	float totalErrors = 0;
	long totalSpikes = 0;
	boolean noGSC = false;
	int startTime = 0;
	String outFilePath;

	private static final String spacer1 = "    ";
	private static final String spacer2 = "        ";

	List<GoldenCheetah> gcArray = new ArrayList<GoldenCheetah>();

	Power power;
	SpeedCad speedCad;
	GoldenCheetah gc = new GoldenCheetah();
	int pos = 0; // Main Buffer Position

	boolean debug = false;
	boolean megaDebug = false;
	PrintWriter fout;
	String serElevationPath;

	GoogleElevation googleElevation;
	Options options = new Options();

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

	private void initGCFile(int year, int month, int day, int hour, int minute,
			int second) {
		fout.write("<!DOCTYPE GoldenCheetah>\n");
		fout.write("<ride>\n");
		fout.write(spacer1 + "<attributes>\n");
		fout.write(spacer2 + "<attribute key=\"Start time\" value=\""
				+ formatDate(year) + "/" + formatDate(++month) + "/"
				+ formatDate(day) + " " + formatDate(hour) + ":"
				+ formatDate(minute) + ":" + formatDate(second) + " UTC\" />\n");
		fout.write(spacer2
				+ "<attribute key=\"Device type\" value=\"Golden Embed GPS\" />\n");
		fout.write(spacer1 + "</attributes>\n");
		fout.write("<samples>\n");
	}

	private String formatDate(int _toFormat) {

		String toFormat = String.valueOf(_toFormat);
		if (toFormat.length() < 2)
			toFormat = "0" + toFormat;
		return toFormat;

	}

	public GoldenEmbedParserMain(String[] args) {

		Option inputFile = OptionBuilder.withArgName("inputfile").hasArg()
				.withDescription("Input file GoldenEmbedGPS")
				.create("inputfile");

		Option serialElevation = OptionBuilder.withArgName("serelevation")
				.hasArg()
				.withDescription("Path to the serialized elevation object")
				.create("serelevation");

		Option outputFile = OptionBuilder.withArgName("outfile").hasArg()
				.withDescription("Directory of where to write the Output File")
				.create("outfile");

		Option logfile = OptionBuilder.withArgName("logfile").hasArg()
				.withDescription("use given file for log").create("logfile");

		Option debugOption = OptionBuilder.withArgName("debug").hasArg()
				.withDescription("Level of debug").create("debug");

		options.addOption(inputFile);
		options.addOption(serialElevation);
		options.addOption(outputFile);
		options.addOption(logfile);
		options.addOption(debugOption);

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

			if (line.hasOption("serelevation"))
				serElevationPath = line.getOptionValue("serelevation");

			googleElevation = new GoogleElevation(serElevationPath);

			if (!line.hasOption("inputfile")) {
				printUsage();
				System.exit(1);
			} else
				file = new File(line.getOptionValue("inputfile"));

			if (line.hasOption("outfile"))
				outFilePath = line.getOptionValue("outfile");
			else {
				outFilePath = file.getAbsolutePath();
			}

			System.out.println("Input File: " + file.getAbsolutePath());
			byte[] readBytes;
			try {
				readBytes = getBytesFromFile(file);
				while (pos != file.length())
					pos = readBuffer(readBytes, file.getParent());

				System.out.println("\n\nTotal Failed Checksums: " + totalErrors
						+ " Out of Total ANT Messages: " + totalTrans);
				System.out.println("% Failure: " + (totalErrors / totalTrans)
						* 100.0);
				System.out.println("Total CAD or Watt Spikes: " + totalSpikes);
				writeOutGCRecords();
				closeGCFile();
				System.exit(0);

			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

	}

	public void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("Golden Embed Parser", options);
	}

	private void ANTParsePower(byte[] msgData, int size, GoldenCheetah gc) {
		int i = 4;
		if (megaDebug)
			System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i]));
		if (msgData[i] == 0x12) // Parse ANT+ 0x12 message (QUARQ)
		{
			ANTParsePower0x12(msgData, size, gc);
		}
	}

	private void ANTparseHRM(byte[] msgData, GoldenCheetah gc) {
		int i = 5;
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

		gc.setHr(hr);
	}

	private void ANTrxMsg(byte[] rxIN, int size, GoldenCheetah gc) {
		int i = 2;
		if (megaDebug)
			System.out.println("Converting 0x"
					+ UnicodeFormatter.byteToHex(rxIN[i]));
		switch (rxIN[i]) {
		case MESG_RESPONSE_EVENT_ID:
			if (debug)
				System.out.println("ID: MESG_RESPONSE_EVENT_ID\n");
			ANTresponseHandler(rxIN, size, gc);
			break;
		case MESG_CAPABILITIES_ID:
			if (debug)
				System.out.println("ID: MESG_CAPABILITIES_ID\n");
			i = ANTCfgCapabilties(i, size); // rxBuf[3] .. skip sync, size, msg
			break;
		case MESG_BROADCAST_DATA_ID:
			if (debug)
				System.out.println("ID: MESG_BROADCAST_DATA_ID\n");
			Byte aByte = new Byte(rxIN[++i]);
			int chan = aByte.intValue();
			if (chan == 0)
				ANTparseHRM(rxIN, gc);
			else if (chan == 1)
				ANTParsePower(rxIN, size, gc);
			break;

		case MESG_CHANNEL_ID_ID:
			if (debug)
				System.out.println("ID: MESG_CHANNEL_ID_ID\n");
			ANTChannelID(rxIN, gc);
			break;
		default:
			if (debug)
				System.out.println("ID: Unknown 0x"
						+ UnicodeFormatter.byteToHex(rxIN[i]));
		}
		return;
	}

	public void ANTChannelID(byte[] msgIN, GoldenCheetah gc) {
		byte[] devNo = new byte[2];

		int i = pos + 2;
		devNo[0] = msgIN[i];
		if (megaDebug)
			System.out.println("Device Type is: 0x"
					+ UnicodeFormatter.byteToHex(msgIN[i]));

		devNo[1] = msgIN[i];
		if (megaDebug)
			System.out.println("Device Type is: 0x"
					+ UnicodeFormatter.byteToHex(msgIN[i]));

		int deviceNum = byteArrayToInt(devNo, 0, 2);
		if (debug)
			System.out.println("Device Number is: " + deviceNum);
		if (debug)
			System.out.println("Device Type is: 0x"
					+ UnicodeFormatter.byteToHex(msgIN[i]));
		if (debug)
			System.out.println("Man ID is: 0x"
					+ UnicodeFormatter.byteToHex(msgIN[i]) + "\n");

		return;
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

	public void ANTParsePower0x12(byte[] msgData, int size, GoldenCheetah gc) {
		int t1;
		int p1;
		int r1;

		int end = 12;
		double rdiff = 0;
		double pdiff = 0;
		double tdiff = 0;
		double nm = 0;
		double rpm = 0;
		double watts = 0;
		Byte aByte;
		int msgN = 0;

		for (int i = 6; i < end; i++) {
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
		}
		if (power.first0x12)
			power.first0x12 = false;

	}

	private boolean ANTrxHandler(byte[] rxBuf, GoldenCheetah gc) {
		int msgN = 0;
		int i;
		int size = 0;
		boolean inMsg = true;
		boolean errorFlag = true;

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
			} else if (msgN == 2) {
				byte checksum = checkSum(rxBuf, size);
				if (checksum == rxBuf[size + 3]) // Check if chksum = msg
				// chksum
				{
					inMsg = true;
					// Handle Message
					ANTrxMsg(rxBuf, size, gc);
					msgN++;
					break;
				} else {
					if (megaDebug)
						System.out.println("CheckSum Mismatch 0x"
								+ UnicodeFormatter.byteToHex(rxBuf[size + 3])
								+ "!=: 0x"
								+ UnicodeFormatter.byteToHex(checksum));
					msgN = 0;
					inMsg = true;
					totalErrors++;
					errorFlag = true;
					return errorFlag;
				}
			} else
				return errorFlag;
		}
		return errorFlag;
	}

	private void closeGCFile() {
		fout.write(spacer1 + "</samples>\n");
		fout.write("</ride>\n");
		fout.flush();
		fout.close();
	}

	private byte checkSum(byte data[], int length) {
		byte chksum = 0x0;

		for (int i = 0; i < length + 3; i++) {
			chksum ^= data[i]; // +1 since skip prefix sync code, we already
			// counted it
		}

		return chksum;
	}

	private void ANTresponseHandler(byte rxBuf[], int size, GoldenCheetah gc) {
		byte ch = rxBuf[3];
		byte id = rxBuf[4];
		byte code = rxBuf[5];

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

		return; // For Loop will move 1 forward
	}

	private int ANTCfgCapabilties(int i, int size) {
		return i + size + 4;
	}

	public static int byteArrayToInt(byte[] b, int offset, int size) {
		uShort uint = new uShort(b, offset);
		return uint.getValue();
	}

	private void writeGCRecord(GoldenCheetah gc) {

		fout.write(spacer1 + "<sample cad=\"" + gc.getCad() + "\" watts=\""
				+ gc.getWatts() + "\" kph=\"" + Round(gc.getSpeed(), 1)
				+ "\" km=\"" + Round(gc.getDistance(), 2) + "\" secs=\""
				+ gc.getSecs() + "\" hr=\"" + gc.getHr() + "\" lon=\""
				+ gc.getLongitude() + "\" lat=\"" + gc.getLatitude()
				+ "\" alt=\"" + gc.getElevation() + "\" len=\"1\"/>\n");

		gc.setPrevsecs(gc.getSecs());
		gc.setHr(0);
		gc.setCad(0);
		power.setWatts(0);
		power.setRpm(0);
	}

	public static double Round(double Rval, int Rpl) {
		double p = Math.pow(10, Rpl);
		Rval = Rval * p;
		double tmp = Math.round(Rval);
		return tmp / p;
	}

	private String convertBytesToString(byte[] bytes) {
		return new String(bytes).trim();
	}

	private int readBuffer(byte[] readBytes, String filePath) {
		int bufPos = 0;
		GPS gps = new GPS();
		byte[] bufToSend;

		if (pos + bufPos >= readBytes.length) {
			System.out.println("\n\nTotal Failed Checksums: " + totalErrors
					+ " Out of Total ANT Messages: " + totalTrans);
			System.out.println("% Failure: " + (totalErrors / totalTrans)
					* 100.0);
			System.out.println("Total CAD or Watt Spikes: " + totalSpikes);
			writeOutGCRecords();
			closeGCFile();
			System.exit(0);
		}

		Byte aByte = new Byte(readBytes[pos + bufPos + 1]);
		int size = aByte.intValue();
		bufToSend = new byte[size + 4];
		for (; bufPos < bufToSend.length; bufPos++)
			bufToSend[bufPos] = readBytes[bufPos + pos];
		if (ANTrxHandler(bufToSend, gc) == true) {
			pos++;
			// We failed a checksum skip..
			while (readBytes[pos] != MESG_TX_SYNC)
				pos++;
			return pos;
		}

		pos = (pos + size + 4);
		try {

			// Now Parse GPS
			gps = GPSHandler(readBytes);

			byte[] timeStamp = new byte[6];

			for (int i = 0; i < 6; i++)
				timeStamp[i] = readBytes[pos++];

			long secs = parseTimeStamp(timeStamp);

			gc.setLatitude(gps.getLatitude());
			gc.setLongitude(gps.getLongitude());
			gc.setSpeed(gps.getSpeed() * KNOTS_TO_KILOMETERS);
			gc.setDistance(gc.getDistance()
					+ (gc.getSpeed() * (gc.getSecs() - gc.getPrevSpeedSecs()) / 3600.0));
			gc.setPrevSpeedSecs(gc.getSecs());
			gc.setSecs(secs);
			gc.setDate(gps.getDate());

			// If we haven't created the file, create it
			if (outFile == null)
				initOutFile(gps, outFilePath, timeStamp);
			if (gc.getSecs() - gc.getPrevWattsecs() >= 5) {
				gc.setWatts(0);
				gc.setCad(0);
			}

			if (gc.getPrevsecs() != gc.getSecs()) {
				gc.setWatts((int) Round(
						power.getWatts() / power.getTotalWattCounter(), 0));
				gc.setCad((int) Round(
						power.getRpm() / power.getTotalCadCounter(), 0));
				GoldenCheetah _gc = gc.clone(gc);
				gcArray.add(_gc);
				gc.setPrevsecs(gc.getSecs());
				gc.newWatts = false;

				/*
				 * Wait.manySec(2);
				 * gc.setElevation(googleElevation.getElevation(
				 * Float.parseFloat(gc.getLatitude()),
				 * Float.parseFloat(gc.getLongitude())));
				 */

			}

		} catch (NumberFormatException e) {
			while (readBytes[pos] != MESG_TX_SYNC)
				pos++;
			totalErrors++;
			return pos;
		} catch (StringIndexOutOfBoundsException ex) {
			while (readBytes[pos] != MESG_TX_SYNC)
				pos++;
			totalErrors++;
			return pos;

		}
		return pos;
	}

	private byte[] parseOutGPS(byte[] buf, int length, int pos) {
		byte[] position = new byte[length];

		for (int i = 0; i < length; i++) {
			position[i] = buf[pos++];
		}
		return position;

	}

	private GPS GPSHandler(byte[] gpsGGA) throws NumberFormatException,
			StringIndexOutOfBoundsException {
		GPS gps = new GPS();

		float degrees = 0;
		float mins = 0;

		byte[] position = parseOutGPS(gpsGGA, 9, pos);
		String strPosition = convertBytesToString(position);

		pos += 9;

		if (strPosition.startsWith("0")) {
			strPosition = strPosition.replaceFirst("0", "-");
			degrees = Float.parseFloat(strPosition.substring(0, 2));
			mins = Float.parseFloat(strPosition.substring(2,
					strPosition.length()));
			gps.setLatitude((String.valueOf(-1
					* (Math.abs(degrees) + (mins / 60)))));
		} else {
			degrees = Float.parseFloat(strPosition.substring(0, 2));
			mins = Float.parseFloat(strPosition.substring(2,
					strPosition.length()));
			gps.setLatitude(String.valueOf(degrees + (mins / 60)));
		}

		position = parseOutGPS(gpsGGA, 9, pos);
		strPosition = convertBytesToString(position);
		pos += 9;

		if (strPosition.startsWith("0")) {
			strPosition = strPosition.replaceFirst("0", "-");
			degrees = Float.parseFloat(strPosition.substring(0, 3));
			mins = Float.parseFloat(strPosition.substring(3,
					strPosition.length()));
			gps.setLongitude((String.valueOf(-1
					* (Math.abs(degrees) + (mins / 60)))));
		} else {
			degrees = Float.parseFloat(strPosition.substring(0, 2));
			mins = Float.parseFloat(strPosition.substring(2,
					strPosition.length()));
			gps.setLongitude(String.valueOf(Math.abs(degrees) + (mins / 60)));
		}
		// Speed
		position = parseOutGPS(gpsGGA, 4, pos);
		strPosition = convertBytesToString(position);
		pos += 4;

		if (strPosition.trim().length() != 0)
			gps.setSpeed(Double.parseDouble(strPosition));
		else
			gps.setSpeed(0.0);

		return gps;
	}

	private void initOutFile(GPS gps, String filePath, byte[] timeStamp) {
		if (outFile == null) {

			String strYear = "20" + timeStamp[0];
			int year = Integer.valueOf(strYear);
			int month = timeStamp[1];
			month--; // Zero based
			int day = timeStamp[2];

			int hr = timeStamp[3];
			int min = timeStamp[4];
			int sec = timeStamp[5];

			Calendar rideCal = new GregorianCalendar(
					TimeZone.getTimeZone("UTC"));
			rideCal.set(year, month, day, hr, min, sec);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");

			outFile = new File(filePath + "/" + sdf.format(rideCal.getTime())
					+ ".gc");

			System.out.println("GC Formatted File: " + outFile.toString());

			try {
				fout = new PrintWriter(new FileOutputStream(outFile));
				initGCFile(year, month, day, hr, min, sec);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
		}
	}

	private long parseTimeStamp(byte[] timeStamp) throws NumberFormatException {
		Calendar cal = new GregorianCalendar();

		Byte year;
		Byte month;
		Byte day;

		Byte hour;
		Byte min;
		Byte sec;
		int i = 0;

		year = new Byte(timeStamp[i++]);
		month = new Byte(timeStamp[i++]);
		day = new Byte(timeStamp[i++]);

		hour = new Byte(timeStamp[i++]);
		min = new Byte(timeStamp[i++]);
		sec = new Byte(timeStamp[i++]);

		cal.set(year, month, day, hour, min, sec);

		long totalSecs = cal.getTimeInMillis() / 1000;

		if (firstRecordedTime == 0)
			firstRecordedTime = totalSecs;

		return totalSecs - firstRecordedTime;
	}

	public boolean isDouble(String input) {
		try {
			Double.parseDouble(input);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void flushPowerArray(GoldenCheetah gc) {
		GoldenCheetah _gc;
		// Now create GC file records for the missing messages.

		if (gcArray.size() == 0)
			return;
		long startSecs = lastWattSecs;
		long endSecs = gc.getSecs(); // The next time we are about to save.
		long diffSecs = endSecs - startSecs;
		long watts = 0;
		long cad = 0;

		if (diffSecs >= 5) // Let's no be ridiculous.
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
		for (long x = startSecs; x < endSecs; x++) {
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
		List<GoldenCheetah> gcElevations = googleElevation
				.getGCElevations(gcArray);
		Iterator<GoldenCheetah> iterElevation = gcElevations.iterator();
		// Iterator<GoldenCheetah> iterElevation = gcArray.iterator();
		while (iterElevation.hasNext()) {
			GoldenCheetah _gc = iterElevation.next();
			writeGCRecord(_gc);
		}

		// Serialize the Elevation Map.
		googleElevation.serializeElevations();
		System.out.println("Finished");
	}

	private GoldenCheetah findGCByTime(long secs) {
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
