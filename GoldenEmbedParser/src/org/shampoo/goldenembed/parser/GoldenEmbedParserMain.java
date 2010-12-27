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

public class GoldenEmbedParserMain
{
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
    static final double KNOTS_TO_KILOMETERS= 1.85200;
    
    File outFile = null;

    float totalTrans = 0;
    float totalErrors = 0;
    boolean errorFlag = false;
    long totalSpikes = 0;
    boolean noGSC = false;
    int startTime = 0;

    private static final String spacer1 = "    ";
    private static final String spacer2 = "        ";

    Power power;
    SpeedCad speedCad;
    GoldenCheetah gc = new GoldenCheetah();
    int pos = 0; //Main Buffer Position

    boolean debug = false;
    boolean megaDebug = false;
    PrintWriter fout;

    /**
     * @param args
     */

    public static void main(String[] args) {
        new GoldenEmbedParserMain(args);
    }

    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    public GoldenEmbedParserMain() {
        power = new Power();
    }

    private void initGCFile(String year, String month, String day, String hour, String minute, String second)
    {
        fout.write("<!DOCTYPE GoldenCheetah>\n");
        fout.write("<ride>\n");
        fout.write(spacer1 + "<attributes>\n");
        fout.write(spacer2 + "<attribute key=\"Start time\" value=\""+year+"/"+ month +"/" + day+" " + hour+":" + minute + ":" + second + " UTC\" />\n");
        fout.write(spacer2 + "<attribute key=\"Device type\" value=\"Golden Embed GPS\" />\n");
        fout.write(spacer1 + "</attributes>\n");
        fout.write("<samples>\n");
    }

    private String formatDate(String toFormat)
    {
        if (toFormat.length() < 2)
        	toFormat = "0" + toFormat;

        return toFormat;

    }

    public GoldenEmbedParserMain(String[] args)
    {
        // Load up the file
        File file = null;
        power = new Power();
        speedCad = new SpeedCad();

        if (args.length < 1) {
            System.out.println("Missing Input File");
            System.exit(1);
        }
        else
            file = new File(args[0]);

        if (args.length >= 2)
        {
            if (args[1].equals("-d"))
                debug = true;
            else if(args[1].equals("-dd"))
            {
               debug = true;
               megaDebug = true;
            }
        }

        byte[] readBytes;
        try {
            readBytes = getBytesFromFile(file);
            while(pos != file.length() )
                pos = readBuffer(readBytes, file.getParent());

            System.out.println("\n\nTotal Failed Checksums: " + totalErrors + " Out of Total ANT Messages: " + totalTrans);
            System.out.println("% Failure: " + (totalErrors / totalTrans) * 100.0);
            System.out.println("Total CAD or Watt Spikes: " + totalSpikes);
            closeGCFile();
            System.exit(0);


        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private int ANTParsePower(byte[] msgData, int size, GoldenCheetah gc)
    {
        int i = 5;
        if(megaDebug) System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i]));
        if (msgData[i] == 0x12) // Parse ANT+ 0x12 message (QUARQ)
        {
            i += 2;
            return i = ANTParsePower0x12(msgData, i, size, gc);
        }
        else if(msgData[i] == 0x11)  // Parse ANT+ 0x11 messages (PowerTap)
        {
            i++;
            return i = ANTParsePower0x11(msgData, i, size, gc);
        }
        else
        {
            i = i+2;
            return i + size;
        }
    }

    private int ANTparseHRM(byte[] msgData, GoldenCheetah gc)
    {
        int i = 5;
        byte aByte;
        int end = i + 8;
        int hrCountFinder = 0;
        int hr = 0;

        for (; i < end; i++) {
            aByte = msgData[i];
            if(megaDebug) System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(msgData[i]));
            if (hrCountFinder == 6) { // HR is the sixth byte
                if(megaDebug) System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(msgData[i]));
                hr = unsignedByteToInt(aByte);
                if (debug)
                    System.out.println("Heart Rate is: " + hr);
            }
            else if(megaDebug)
                System.out.println("o" + i + "=" + unsignedByteToInt(aByte));
            hrCountFinder++;
        }

        gc.setHr(hr);

        return --i; // For Loop will advance itself.
    }

    private void ANTrxMsg(byte[] rxIN, int size, GoldenCheetah gc)
    {
        int i = 2;
        if(megaDebug) System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(rxIN[i]));
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
                i = ANTparseHRM(rxIN, gc);
            else if(chan == 1)
                ANTParsePower(rxIN, size, gc);
            else if(chan == 2)
                ANTParseSpeedCad(rxIN, size, gc);

            if(gc.getPrevsecs() != gc.getSecs())
            {
                gc.setWatts((int)Round(power.getWatts() / power.getTotalWattCounter(),0));
                gc.setCad((int)Round(power.getRpm() / power.getTotalCadCounter(),0));

                if (!noGSC) {
            	    if (!power.first0x12)
            	        gc.setCad((int)Round(power.getRpm() / power.getTotalCadCounter(),0));
                }

                if(gc.getSecs () - gc.getPrevSpeedSecs() >= 5)
                    gc.setSpeed(0);

                writeGCRecord(gc);
                gc.setPrevWattsecs(gc.getSecs());
                gc.newWatts = false;
            }
            break;

        case MESG_CHANNEL_ID_ID:
            if (debug)
                System.out.println("ID: MESG_CHANNEL_ID_ID\n");
            ANTChannelID(rxIN, gc);
            break;
        default:
            if (debug)
                System.out.println("ID: Unknown 0x" + UnicodeFormatter.byteToHex(rxIN[i]));
        }
        return;
    }

    public void ANTChannelID(byte[] msgIN, GoldenCheetah gc)
    {
        byte[] devNo = new byte[2];

        int i = pos + 2;
        devNo[0] = msgIN[i];
        if (megaDebug)
            System.out.println("Device Type is: 0x" + UnicodeFormatter.byteToHex(msgIN[i]));

        i--;
        devNo[1] = msgIN[i];
        if (megaDebug)
            System.out.println("Device Type is: 0x" + UnicodeFormatter.byteToHex(msgIN[i]));

        int deviceNum = byteArrayToInt(devNo, 0, 2);
        if (debug)
            System.out.println("Device Number is: " + deviceNum);

        i += 2;
        if (debug)
            System.out.println("Device Type is: 0x" + UnicodeFormatter.byteToHex(msgIN[i]));
        pos++;
        if (debug)
            System.out.println("Man ID is: 0x" + UnicodeFormatter.byteToHex(msgIN[i]) + "\n");

        return;
    }

    public byte[] getBytesFromFile(File file) throws IOException
    {
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
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        if(megaDebug) System.out.println("Total Bytes: " + bytes.length);
        return bytes;
    }

    private int ANTParseSpeedCad(byte[] msgData, int size, GoldenCheetah gc)
    {
        int i = 5;

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

        if(speedCad.getCrankrev() != crankrev)
        {
            if(!speedCad.isFirst12Cad())
            {
                double crankTimeDelta = (65536 + cranktime - speedCad.getCranktime()) % 65536;
                double crankRevDelta = (65536 + crankrev - speedCad.getCrankrev()) % 65536;
                double cad = 1024.0*60.0*crankRevDelta/crankTimeDelta;

                if(debug)System.out.println("cadence: " + cad);
                cad = Math.round(cad);
                if(!noGSC)
                {
                    gc.setPrevCadSecs(gc.getSecs());
                    gc.setCad(Math.round(cad));
                }

                speedCad.setCrankrev(crankrev);
                speedCad.setCranktime(cranktime);

            } else
            {
                speedCad.setCrankrev(crankrev);
                speedCad.setCranktime(cranktime);
                speedCad.setFirst12Cad(false);
            }
        }

        if(speedCad.getWheelrev() != wheelrev)
        {
            if(!speedCad.isFirst12Speed())
            {

                if(megaDebug) System.out.println("WheelTime: " + wheeltime);
                if(megaDebug) System.out.println("LastWheelTime: " + speedCad.getWheeltime());
                double wheelTimeDelta = Math.abs(wheeltime - speedCad.getWheeltime());

                if(megaDebug) System.out.println("WheelTimeDelta: " + wheelTimeDelta);
                wheelTimeDelta = (65536 + wheeltime - speedCad.getWheeltime()) % 65536;

                if(megaDebug) System.out.println("wheelrev: " + wheelrev);
                if(megaDebug) System.out.println("LastWheelRev: " + speedCad.getWheelrev());

                double wheelRevDelta = Math.abs(wheelrev - speedCad.getWheelrev());
                wheelRevDelta = (65536 + wheelrev - speedCad.getWheelrev()) % 65536;
                if(megaDebug) System.out.println("wheelRevDelta: " + wheelRevDelta);

                double rpm = 1024.0*60.0*wheelRevDelta/wheelTimeDelta;
                double speed = 60.0*1024.0*60.0*speedCad.wheelCirc*wheelRevDelta/1000/1000/wheelTimeDelta;

                if(debug)System.out.println("rpm: " + rpm);
                if(debug)System.out.println("Speed: " + speed);

                gc.setSpeed(speed);
                if(gc.getPrevSpeedSecs() != gc.getSecs())
                {
                    gc.setDistance(gc.getDistance() + (speed * (gc.getSecs() - gc.getPrevSpeedSecs()) / 3600.0));
                    if(debug)System.out.println("Distance: " + gc.getDistance());
                    gc.setPrevSpeedSecs(gc.getSecs());
                }
                speedCad.setWheelrev(wheelrev);
                speedCad.setWheeltime(wheeltime);
            }
            else
            {
                speedCad.setWheelrev(wheelrev);
                speedCad.setWheeltime(wheeltime);
                speedCad.setFirst12Speed(false);
            }
        }

        return --i; // For Loop will advance itself.

    }

    public int ANTParsePower0x11(byte[] msgData, int i, int size, GoldenCheetah gc)
    {
    	// For parsing ANT+ 0x11 messages
    	// Counter (u8) Unknown (u8) Cadence (u8) Wheel_RPM_Counter (u16) Torque_Counter (u16)
    	// Cnt_diff = (Last Counter - Current Counter + 256) mod 256
    	// Torque_diff = (Last Torque - Current Torque + 65536) mod 65536
    	// NM = Torque_diff / 32 / Cnt_diff
    	// Wheel_RPM_diff = (Last Wheel RPM - Current Wheel_RPM + 65536) mod 65536
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
        //System.out.println("c1: " + c1 + " c2: " + c2 + " pr1: " + pr1 + " t1: " + t1 + " r1: " + r1 + "\n");
        if (power.first0x11) {
        	power.first0x11 = false;
        	power.setR(r1);
        	power.setT(t1);
        	power.setCnt(c1);
        }
        else if (c1 != power.getCnt())
        {
        		cdiff = (( 256 + c1 - power.getCnt()) % 256);
            	tdiff = ( 65536 + t1 - power.getT() ) % 65536;
        		rdiff = ( 65536 + r1 - power.getR() ) % 65536;

        		if (tdiff != 0 && rdiff != 0)
        		{
        			nm = (float)tdiff / 32 / (float)cdiff;
        			rpm = 122880 * (float)cdiff / (float)rdiff;
        			watts = rpm * nm * 2 * PI / 60;

        			if (debug) {
                        System.out.format("ANTParsePower0x11 cad: %3d  nm: %5.2f  rpm: %5.2f  watts: %6.1f", pr1, nm, rpm, watts);
                        System.out.println();
        			}
                    if (rpm < 10000 && watts < 10000) {
                        if(gc.newWatts == false)
                        {
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
                            System.out.println("Spike Found: cdiff: " + cdiff + " rdiff: " + rdiff + " tdiff: " + tdiff+ "\n");
                        totalSpikes++;
                    }
              }
        }

        power.setR(r1);
    	power.setT(t1);
    	power.setCnt(c1);

        return --i; // For Loop will advance itself.
    }

    public int ANTParsePower0x12(byte[] msgData, int i, int size, GoldenCheetah gc)
    {
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
                System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i]));
            if (msgN == 0) {
                if (power.first0x12) {
                    // Just store it.
                    aByte = new Byte(msgData[i]);
                    power.setR(aByte.intValue());
                    power.first0x12 = false;
                    if(megaDebug) System.out.println("R: " + aByte.intValue());
                } else {
                    // We can calculate and then store
                    aByte = new Byte(msgData[i]);
                    r1 = aByte.intValue();
                    rdiff = (r1 + 256 - power.getR()) % 256;
                    power.setR(r1);
                    if(megaDebug) System.out.println("rdiff is: " + rdiff);
                }
                msgN++;
            } else if (msgN == 1) {
                byte[] pRdiff = new byte[2];
                i++;
                pRdiff[1] = msgData[i];
                if(megaDebug) System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");
                i++;
                pRdiff[0] = msgData[i];
                if(megaDebug) System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");
                p1 = byteArrayToInt(pRdiff, 0, 2);

                if (power.first0x12) {
                    power.setP(p1);
                    if(megaDebug) System.out.println("P1: " + p1);
                } else {
                    pdiff = (65536 + p1 - power.getP()) % 65536;
                    power.setP(p1);
                    if(megaDebug) System.out.println("pdiff is: " + pdiff);
                }
                msgN++;
            } else if (msgN == 2) {
                byte[] pRdiff = new byte[2];
                pRdiff[1] = msgData[i];
                if(megaDebug) System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");
                i++;
                pRdiff[0] = msgData[i];
                if(megaDebug) System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");

                t1 = byteArrayToInt(pRdiff, 0, 2);

                if (power.first0x12) {
                    power.setT(t1);
                    if(megaDebug) System.out.println("T: " + t1);
                } else {
                    tdiff = (t1 + 65536 - power.getT()) % 65536;
                    power.setT(t1);
                    if(megaDebug) System.out.println("tdiff is: " + tdiff);
                }

                i++;
                msgN++;
            }
        }
        if (tdiff != 0 && rdiff != 0) {
            nm = (float)tdiff / ((float)rdiff * 32.0);
            rpm = (double)rdiff * 122880.0 / (double)pdiff;
            watts = rpm * nm * 2 * PI / 60;

            if (debug)
                System.out.println("ANTParsePower0x12 nm: " + nm + " rpm: " + rpm + " watts: " + watts + "\n");
            if (rpm < 10000 && watts < 10000) {
                if(gc.newWatts == false)
                {
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
            } else {
                if (debug)
                    System.out.println("Spike Found: pdiff: " + pdiff + " rdiff: " + rdiff + " tdiff: " + tdiff+ "\n");
                totalSpikes++;
            }
        if (power.first0x12)
            power.first0x12 = false;

        if(megaDebug)  System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");
        }
        else
        {
        	power.setWatts(0);
        	power.setRpm(0);
            double wattCounter = power.getTotalWattCounter();
            double cadCounter = power.getTotalCadCounter();
            power.setTotalWattCounter(wattCounter + 1);
            power.setTotalCadCounter(cadCounter + 1);
        }

        return --i; // For Loop will advance itself.

    }

    private boolean ANTrxHandler(byte[] rxBuf, GoldenCheetah gc)
    {
        int msgN = 0;
        int i;
        int size = 0;
        boolean inMsg = true;

        for (i = 0; i < rxBuf.length; i++)
	    {
            if (rxBuf[i] == MESG_TX_SYNC && inMsg)
	        {
                inMsg = false;
                msgN = 0; // Always reset msg count if we get a sync
                msgN++;
                errorFlag = false;
                totalTrans++;
                if(megaDebug) System.out.println("RX: [sync]");
            }
	        else if (msgN == 1)
	        {
                Byte aByte = new Byte(rxBuf[i]);
                msgN++; // Size
                size = aByte.intValue();
            }
	        else if(msgN == 2)
	        {
                byte checksum = checkSum(rxBuf, size);
                if (checksum == rxBuf[size + i + 1]) // Check if chksum = msg
                // chksum
                {
                    inMsg = true;
                    // Handle Message
                    ANTrxMsg(rxBuf, size, gc);
                    msgN++;
                }
		        else
	       	    {
                    if(megaDebug) System.out.println("CheckSum Mismatch 0x"+ UnicodeFormatter.byteToHex(rxBuf[size+i+1]) + "!=: 0x" + UnicodeFormatter.byteToHex(checksum));
                    msgN = 0;
                    inMsg = true;
                    if (errorFlag == false) {
                        totalErrors++;
                        errorFlag = true;
                    }
                }
            }
	        else
	            return errorFlag;
        }
        return errorFlag;
    }

    private void closeGCFile()
    {
        fout.write(spacer1 + "</samples>\n");
        fout.write("</ride>\n");
        fout.flush();
        fout.close();
    }

    private byte checkSum(byte data[], int length)
    {
        byte chksum = 0x0;

        for (int i = 0; i < length+3; i++) {
            chksum ^= data[i]; // +1 since skip prefix sync code, we already
            // counted it
        }

        return chksum;
    }

    private void ANTresponseHandler(byte rxBuf[], int size, GoldenCheetah gc)
    {
        byte ch = rxBuf[3];
        byte id = rxBuf[4];
        byte code = rxBuf[5];

        if (debug)
        {
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
             if(code == 0x01)
             {
        	if(ch == 0)
                System.out.println("Dropped HRM");
        	else if(ch == 1)
        	System.out.println("Dropped Power");
       		else if(ch == 2)
       		System.out.println("Dropped Cadence/Speed");
       	     }
       	break;
        default:
            if (debug)
                System.out.println("[unknown]: " + UnicodeFormatter.byteToHex(id));
            break;
        }

        return; // For Loop will move 1 forward
    }

    private int ANTCfgCapabilties(int i, int size)
    {
        return i + size + 4;
    }

    public static int byteArrayToInt(byte[] b, int offset, int size)
    {
    	uShort uint = new uShort(b, offset);
    	return uint.getValue();
    }

    private void writeGCRecord(GoldenCheetah gc)
    {
    	if(gc.getSecs() != gc.getPrevsecs())
    	{
    		if(gc.getLatitude().length() != 0 && gc.getLongitude().length() != 0) //TODO Big Patch
    		{
                fout.write(spacer1 + "<sample cad=\"" + gc.getCad() + "\" watts=\"" + gc.getWatts() + "\" kph=\"" + Round(gc.getSpeed(),1) + "\" km=\"" + Round(gc.getDistance(),2) + "\" secs=\""
                         + gc.getSecs() + "\" hr=\"" + gc.getHr() + "\" lon=\"" + gc.getLongitude() + "\" lat=\"" + gc.getLatitude() +"\" len=\"1\"/>\n");

                gc.setPrevsecs(gc.getSecs());
    		}
    	}
    }

    public static double Round(double Rval, int Rpl)
    {
          double p = (double)Math.pow(10,Rpl);
          Rval = Rval * p;
          double tmp = Math.round(Rval);
          return (double)tmp/p;
    }

    private String convertBytesToString(byte[] bytes)
    {
        return new String(bytes).trim();
    }

    private int readBuffer(byte[] readBytes, String filePath)
    {
        int bufPos = 0;
        GPS gps = new GPS();
        byte[] bufToSend;

        if(pos+bufPos >= readBytes.length)
        {
            System.out.println("\n\nTotal Failed Checksums: " + totalErrors + " Out of Total ANT Messages: " + totalTrans);
            System.out.println("% Failure: " + (totalErrors / totalTrans) * 100.0);
            System.out.println("Total CAD or Watt Spikes: " + totalSpikes);
            closeGCFile();
            System.exit(0);
        }

        Byte aByte = new Byte(readBytes[pos+bufPos+1]);
        int size = aByte.intValue();
        bufToSend = new byte[size+4];
        for(; bufPos < bufToSend.length; bufPos++)
            bufToSend[bufPos] = readBytes[bufPos+pos];
        if(ANTrxHandler(bufToSend, gc) == true)
        {
            pos++;
            //We failed a checksum skip..
            while(readBytes[pos] != MESG_TX_SYNC)
                pos++;
            return pos;
        }

        pos = (pos + size+ 4);
        //Now Parse GPS
        gps = GPSHandler(readBytes);

        gc.setLatitude(gps.getLatitude());
        gc.setLongitude(gps.getLongitude());
        gc.setSpeed(gps.getSpeed() * KNOTS_TO_KILOMETERS);

        gc.setSecs(parseTimeStamp(gps.getTime()));

        //If we haven't created the file, create it
        if(outFile == null)
            initOutFile(gps, filePath);

        writeGCRecord(gc);

        return ++pos;
    }

    private byte[] parseOutGPS(byte[] buf, int length, int pos)
    {
        byte[] position = new byte[length];

        for(int i=0; i < length; i++)
        {
            position[i] = buf[pos++];
        }
        return position;

    }

    private GPS GPSHandler(byte[] gpsGGA)
    {
    	GPS gps = new GPS();

    	float degrees=0;
        float mins=0;

        byte[] position = parseOutGPS(gpsGGA, 15, pos);
        String strPosition = convertBytesToString(position);

        pos += 15;

        if(strPosition.startsWith("0"))
        {
            strPosition = strPosition.replaceFirst("0", "-");
            degrees = Float.parseFloat(strPosition.substring(0, 2));
            mins = Float.parseFloat(strPosition.substring(2, strPosition.length()));
            gps.setLatitude((String.valueOf(-1 * (Math.abs(degrees)+(mins/60)))));
        }
        else
         {
             degrees = Float.parseFloat(strPosition.substring(0, 2));
             mins = Float.parseFloat(strPosition.substring(2, strPosition.length()));
             gps.setLatitude(String.valueOf(degrees+(mins/60)));
         }

        position = parseOutGPS(gpsGGA, 15, pos);
        strPosition = convertBytesToString(position);
        pos += 15;

        if(strPosition.startsWith("0"))
        {
            strPosition = strPosition.replaceFirst("0", "-");
        	degrees = Float.parseFloat(strPosition.substring(0, 3));
        	mins = Float.parseFloat(strPosition.substring(3,strPosition.length()));
        	gps.setLongitude((String.valueOf(-1 * (Math.abs(degrees)+(mins/60)))));
        }
        else
        {
            degrees = Float.parseFloat(strPosition.substring(0, 2));
            mins = Float.parseFloat(strPosition.substring(2, strPosition.length()));
            gps.setLongitude(String.valueOf(Math.abs(degrees)+(mins/60)));
        }
        //Speed
        position = parseOutGPS(gpsGGA, 6, pos);
        strPosition = convertBytesToString(position);
        pos += 6;
        gps.setSpeed(Double.parseDouble(strPosition));
        
        //Date
        position = parseOutGPS(gpsGGA, 6, pos);
        strPosition = convertBytesToString(position);
        pos += 6;
        gps.setDate(strPosition);

        //Time
        position = parseOutGPS(gpsGGA, 10, pos);
        strPosition = convertBytesToString(position);
        pos += 10;
        gps.setTime(strPosition);

       return gps;
     }

    private void initOutFile(GPS gps, String filePath)
    {
        if(outFile == null)
        {
            String day = formatDate(gps.getDate().substring(0, 2));
            String month = formatDate(gps.getDate().substring(2,4));
            String year = "20" + gps.getDate().substring(4,6);
            String hour = formatDate(gps.getTime().substring(0, 2));
            String min = formatDate(gps.getTime().substring(3, 4));
            String sec = formatDate(gps.getTime().substring(4, 6));

            outFile = new File(filePath + "/" + year + "_" + month + "_" + day + "_"
                + hour + "_" + min + "_" + sec + ".gc");

            System.out.println("Input File: " + outFile.getAbsolutePath());
            System.out.println("GC Formatted File: " + outFile.toString());

            try {
                fout = new PrintWriter(new FileOutputStream(outFile));
                initGCFile(year, month, day, hour, min, sec);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
                System.exit(1);
            }
        }
    }

    private int parseTimeStamp(String strTimeStamp)
    {
    	if(strTimeStamp.trim().length() < 6)
    		return 0;
        String hour = strTimeStamp.substring(0, 2);
        String min = strTimeStamp.substring(2, 4);
        String sec = strTimeStamp.substring(4, 6);
        try
        {
            return (Integer.parseInt(hour)*60*60) + (Integer.parseInt(min) * 60) + Integer.parseInt(sec);
        }
        catch(NumberFormatException e)
        {
        	return 0;
        }
    }

    public boolean isDouble( String input )
    {
       try
       {
          Double.parseDouble( input );
          return true;
       }
       catch( Exception e)
       {
          return false;
       }
    }
 }
