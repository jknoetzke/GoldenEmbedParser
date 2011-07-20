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
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.shampoo.goldenembed.elevation.AltitudePressure;
import org.shampoo.goldenembed.elevation.GoogleElevation;
import org.shampoo.goldenembed.tools.FusionTables;
import org.shampoo.goldenembed.tools.GnuPlot;
import org.shampoo.goldenembed.tools.IntervalBean;
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
    static final byte NEW_LINE = (byte) 0x0A;
    static final double PI = 3.14159265;
    static final double KNOTS_TO_KILOMETERS = 1.85200;

    File outFile = null;

    long lastWattSecs = 0; // To keep track of the last time watts were saved.
    long sanitySeconds = 0;
    double sanityDistance = 0;

    boolean isFirstRecordedTime = true;
    long firstRecordedTime = 0;

    float totalTrans = 0;
    float totalErrors = 0;
    long totalSpikes = 0;
    boolean noGSC = false;
    int startTime = 0;

    private static final String spacer1 = "    ";
    private static final String spacer2 = "        ";

    String rideDate;

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
    AltitudePressure altiPressure;
    Options options = new Options();

    String outGCFilePath;
    String outGnuPlotPath;
    String intervalParam;
    boolean wantsGoogleElevation = false;
    String serializedElevationPath = null;

    LogManager lm = LogManager.getLogManager();
    Logger logger = null;
    String logFilePath = "";
    String username = null;
    String password = null;
    boolean isGPS = false;
    long smoothFactor = 0;

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

        Option usernameOption = OptionBuilder.withArgName("username").hasArg()
                .withDescription("Username for Fusion Tables")
                .create("username");

        Option passwordOption = OptionBuilder.withArgName("password").hasArg()
                .withDescription("Password for Fusion Tables")
                .create("password");

        Option serializedElevationOption = OptionBuilder
                .withArgName("serelevation")
                .hasArg()
                .withDescription(
                        "To use Google Elevation Service set path to serialized elevation file")
                .create("serelevation");

        Option gpsOption = OptionBuilder.withArgName("gps").hasArg()
                .withDescription("true for GEGPS false for GE").create("gps");

        Option smoothOption = OptionBuilder
                .withArgName("smooth")
                .hasArg()
                .withDescription("Enter amount in seconds for smoothing factor")
                .create("smooth");

        options.addOption(smoothOption);
        options.addOption(inputFile);
        options.addOption(outputGCFile);
        options.addOption(outputGnuPlotFile);
        options.addOption(debugOption);
        options.addOption(intervalOption);
        options.addOption(usernameOption);
        options.addOption(passwordOption);
        options.addOption(serializedElevationOption);
        options.addOption(gpsOption);

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

            if (line.hasOption("serelevation"))
                serializedElevationPath = line.getOptionValue("serelevation");

            if (line.hasOption("gps"))
                isGPS = true;

            if ((line.hasOption("username") == true || line
                    .hasOption("password") == true)) {
                username = line.getOptionValue("username");
                password = line.getOptionValue("password");

                if (username.length() == 0 || password.length() == 0) {
                    printUsage();
                    System.exit(1);
                }
            }

            try {
                if (line.hasOption("smooth"))
                    smoothFactor = Long
                            .parseLong(line.getOptionValue("smooth"));
            } catch (NumberFormatException ex) {
                printUsage();
                System.exit(1);
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
                System.exit(0);

            } catch (IOException e) {
                if (logger != null)
                    logger.log(Level.SEVERE, e.toString());
                else
                    System.out.println(e);
            }
        } catch (ParseException exp) {
            // oops, something went wrong
            System.out.println("Parsing failed.  Reason: " + exp.getMessage());
            printUsage();
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, e.toString());
        }

    }

    public void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar GoldenEmbedParser.jar", options);
    }

    private void ANTParsePower(byte[] msgData, int size, GoldenCheetah gc) {
        int i = 4;
        if (megaDebug)
            System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i]));
        if (msgData[i] == 0x12) // Parse ANT+ 0x12 message (QUARQ)
        {
            ANTParsePower0x12(msgData, size, gc);
        } else if (msgData[i] == 0x11) {
            ANTParsePower0x11(msgData, size, gc);
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

        int i = 2;
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
                    // Just store it.di
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

                flushPowerArray(gc, power);

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

    // Powertap support
    public void ANTParsePower0x11(byte[] msgData, int size, GoldenCheetah gc) {
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
        int i = 5;

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
            }
        }

        power.setR(r1);
        power.setT(t1);
        power.setCnt(c1);

        return; // For Loop will advance itself.
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
        byte[] timeStamp;
        long secs = 0;

        if ((pos + bufPos + 64) >= readBytes.length - 1) {
            System.out.println("\n\nTotal Failed Checksums: " + totalErrors
                    + " Out of Total ANT Messages: " + totalTrans);
            System.out.println("% Failure: " + (totalErrors / totalTrans)
                    * 100.0);
            System.out.println("Total CAD or Watt Spikes: " + totalSpikes);
            writeOutGCRecords();
            System.exit(0);
        }

        Byte aByte = new Byte(readBytes[pos + bufPos + 1]);
        int size = aByte.intValue();
        if (size < 0) {
            pos++;
            // We failed a checksum skip..
            while (readBytes[pos] != MESG_TX_SYNC)
                pos++;
            return pos;

        }
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

            if (isGPS) {

                // Now Parse GPS
                gps = GPSHandler(readBytes);

                // Get the intial elevation from Google use Lat and Lon
                if (altiPressure == null) {
                    altiPressure = new AltitudePressure(
                            GoogleElevation.getElevation(
                                    Float.valueOf(gps.getLatitude()),
                                    Float.valueOf(gps.getLongitude())));
                }
                int pressureCounter = 0;
                byte[] pressureByte = new byte[16];

                while (readBytes[pos] != 0x07) {
                    if (pressureCounter == 15)
                        throw new NumberFormatException();
                    pressureByte[pressureCounter++] = readBytes[pos++];
                }
                pos++; // skip the delimeter
                String strPressure = convertBytesToString(pressureByte);
                float pressure = Float.parseFloat(strPressure);

                strPressure = convertBytesToString(pressureByte);
                pressure = Float.parseFloat(strPressure);

                gc.setElevation(altiPressure.altiCalc(pressure / 100.0f));

                timeStamp = new byte[6];

                for (int i = 0; i < 6; i++)
                    timeStamp[i] = readBytes[pos++];
                secs = parseTimeStamp(timeStamp);
            } else {
                timeStamp = new byte[3];
                for (int i = 0; i < 3; i++)
                    timeStamp[i] = readBytes[pos++];
                secs = parseTimeStamp(timeStamp);
            }

            if (secs == 0)
                secs = 0;

            if (rideDate == null && isGPS == true)
                createRideDate(gps, timeStamp);
            else if (rideDate == null)
                createRideDate();

            gc.setLatitude(gps.getLatitude());
            gc.setLongitude(gps.getLongitude());
            gc.setSpeed(gps.getSpeed() * KNOTS_TO_KILOMETERS);

            gc.setDistance(gc.getDistance()
                    + (gc.getSpeed() * (gc.getSecs() - gc.getPrevSpeedSecs()) / 3600.0));

            gc.setPrevSpeedSecs(gc.getSecs());
            if (secs > 86400) // Only fools ride for more then 24hrs a time..
                throw new NumberFormatException();
            gc.setSecs(secs);
            gc.setDate(gps.getDate());

            // If we haven't created the file, create it
            if (outFile == null && outGCFilePath != null) {
                if (isGPS)
                    initOutFile(gps, outGCFilePath, timeStamp);
                else
                    createGCOutFile();
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

        try {

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
        } catch (NumberFormatException e) {
            throw new NumberFormatException();
        }
        return gps;
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

    private void createRideDate() {

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

        rideDate = cal.get(Calendar.YEAR) + "/" + strMonth + "/" + strDay + " "
                + strHour + ":" + strMin + ":" + strSec;

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

        rideDate = cal.get(Calendar.YEAR) + "/" + strMonth + "/" + strDay + " "
                + strHour + ":" + strMin + ":" + strSec;

        File outFile = new File(outGCFilePath + "/" + cal.get(Calendar.YEAR)
                + "_" + strMonth + "_" + strDay + "_" + strHour + "_" + strMin
                + "_" + strSec + ".gc");

        return outFile;

    }

    private void createGCOutFile() {

        outFile = createGCFileName();
        try {
            fout = new PrintWriter(new FileOutputStream(outFile));
            initGCFile();
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            System.exit(1);
        }

    }

    private String createRideDate(GPS gps, byte[] timeStamp) {

        String strYear = "20" + timeStamp[0];
        int year = Integer.valueOf(strYear);
        int month = timeStamp[1];
        month--; // Zero based
        int day = timeStamp[2];

        int hr = timeStamp[3];
        int min = timeStamp[4];
        int sec = timeStamp[5];

        Calendar rideCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        rideCal.set(year, month, day, hr, min, sec);

        SimpleDateFormat rideFormat = new SimpleDateFormat(
                "yyyy/MM/dd hh:mm:ss");
        rideDate = rideFormat.format(rideCal.getTime());

        return rideDate;

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

            try {
                fout = new PrintWriter(new FileOutputStream(outFile));
                initGCFile(year, month, day, hr, min, sec);
            } catch (FileNotFoundException e1) {
                logger.log(Level.SEVERE, e1.toString());
                e1.printStackTrace();
                System.exit(1);
            }
        }
    }

    private long parseTimeStamp(byte[] timeStamp) throws NumberFormatException {
        Calendar cal = new GregorianCalendar();

        try {

            int year = 0;
            int month = 0;
            int day = 0;

            int hour;
            int min;
            int sec;
            int i = 0;

            if (isGPS) {
                year = new Byte(timeStamp[i++]);
                month = new Byte(timeStamp[i++]);
                day = new Byte(timeStamp[i++]);
            }

            if (year >= 0 && year <= 99 && month >= 0 && month <= 12
                    && day >= 0 && day <= 31) {
                hour = new Byte(timeStamp[i++]);
                min = new Byte(timeStamp[i++]);
                sec = new Byte(timeStamp[i++]);

                year += 2000;
            } else
                throw new NumberFormatException();

            if (hour <= 24 && hour >= 0 && min <= 60 && min >= 0 && sec <= 60
                    && sec >= 0)
                cal.set(year, month, day, hour, min, sec);
            else
                throw new NumberFormatException();

            long totalSecs = cal.getTimeInMillis() / 1000;

            SimpleDateFormat rideFormat = new SimpleDateFormat(
                    "yyyy/MM/dd hh:mm:ss");
            rideDate = rideFormat.format(cal.getTime());

            System.out.println(rideDate);

            if (firstRecordedTime == 0)
                firstRecordedTime = totalSecs;

            System.out.println(totalSecs - firstRecordedTime);
            return totalSecs - firstRecordedTime;
        } catch (NumberFormatException e) {
            throw new NumberFormatException();

        }

    }

    public boolean isDouble(String input) {
        try {
            Double.parseDouble(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void flushPowerArray(GoldenCheetah gc, Power power) {
        GoldenCheetah _gc;
        // Now create GC file records for the missing messages.

        if (gcArray.size() == 0)
            return;
        long startSecs = lastWattSecs;
        long endSecs = gc.getSecs(); // The next time we are about to save.
        long diffSecs = endSecs - startSecs;
        long watts = 0;
        long cad = 0;

        if (diffSecs != 0) {
            watts = (long) Round(
                    (power.getWatts() / power.getTotalWattCounter()) / diffSecs,
                    0);
            cad = (long) Round((power.getRpm() / power.getTotalCadCounter())
                    / diffSecs, 0);
        } else {
            watts = (long) Round(
                    power.getWatts() / power.getTotalWattCounter(), 0);
            cad = (long) Round(power.getRpm() / power.getTotalCadCounter(), 0);
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
                _gc.setPrevSpeedSecs(x);
                _gc.setWatts(watts);
                _gc.setCad(cad);
                gcArray.add(_gc);
            }
        }
        lastWattSecs = endSecs + 1;

    }

    private void writeOutGCRecords() {
        Collections.sort(gcArray, new SortBySeconds());
        List<IntervalBean> gcIntervals = new ArrayList<IntervalBean>();

        if (serializedElevationPath != null) {
            googleElevation = new GoogleElevation(serializedElevationPath,
                    logger);
            gcArray = googleElevation.getGCElevations(gcArray);
        }

        if (smoothFactor != 0)
            gcArray = smooth(gcArray, smoothFactor);

        if (outGCFilePath != null) {
            Iterator<GoldenCheetah> iter = gcArray.iterator();
            while (iter.hasNext()) {
                GoldenCheetah _gc = iter.next();
                writeGCRecord(_gc);
            }
            closeGCFile();
        }

        if (outGnuPlotPath != null) {
            GnuPlot plot = new GnuPlot();
            plot.writeOutGnuPlot(gcArray, outGnuPlotPath);
        }

        if (intervalParam != null) {
            Intervals interval = new Intervals();
            gcIntervals = interval.createInterval(gcArray, intervalParam);
        }

        if (username != null) {
            if (rideDate == null) {
                Calendar rideCal = Calendar.getInstance();
                SimpleDateFormat rideFormat = new SimpleDateFormat(
                        "yyyy/MM/dd hh:mm:ss");
                rideDate = rideFormat.format(rideCal.getTime());
            }
            FusionTables ft = new FusionTables(username, password);
            ft.uploadToFusionTables("Golden Embed", gcArray, rideDate,
                    gcIntervals);
        }
        System.out.println("");
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

    public List<GoldenCheetah> smooth(List<GoldenCheetah> gcArray, long secs) {
        ArrayList<GoldenCheetah> gcSmoothedArray = new ArrayList<GoldenCheetah>();

        long totalWatts = 0;
        long totalCad = 0;
        long totalSpeed = 0;
        int totalHR = 0;
        float totalElevation = 0;
        long counter = 0;
        GoldenCheetah smoothedGC;

        for (GoldenCheetah gc : gcArray) {
            if (gc.getSecs() % secs == 0 && counter != 0) {
                smoothedGC = new GoldenCheetah();
                smoothedGC.setCad(totalCad / counter);
                smoothedGC.setWatts(totalWatts / counter);
                smoothedGC.setSpeed(totalSpeed / counter);
                smoothedGC.setHr(totalHR / (int) counter);
                smoothedGC.setElevation(totalElevation / counter);
                smoothedGC.setSecs(gc.getSecs());
                gcSmoothedArray.add(smoothedGC);

                totalWatts = 0;
                totalCad = 0;
                totalSpeed = 0;
                totalHR = 0;
                totalElevation = 0;
                counter = 0;

            } else {
                totalWatts += gc.getWatts();
                totalCad += gc.getCad();
                totalSpeed += gc.getSpeed();
                totalElevation += gc.getElevation();
                totalHR += gc.getHr();
                counter++;
            }
        }

        return gcSmoothedArray;
    }

}
