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
	static final byte MESG_TX_SYNC = (byte)0xA4;
	static final byte MESG_CHANNEL_SEARCH_TIMEOUT_ID =0x44;
	static final byte MESG_ASSIGN_CHANNEL_ID = 0x42;
	static final byte MESG_CHANNEL_RADIO_FREQ_ID = 0x45;
    static final byte MESG_CHANNEL_MESG_PERIOD_ID = 0x43;
    static final byte MESG_OPEN_CHANNEL_ID = (byte)0x4B;
    static final byte MESG_CHANNEL_ID_ID = (byte)0x51; 
    static final byte MESG_NETWORK_KEY_ID  = 0x46;
    static final double PI = 3.14159265;
    
    float totalTrans = 0;
    float totalErrors = 0;
    boolean errorFlag = false;
    
    private static final String spacer1 = "    ";
    private static final String spacer2 = "        ";
	
    Power power ;

	int debug = 0;
	
	PrintWriter fout;
	

	/**
	 * @param args
	 */

	public static void main(String[] args) 
	{
		new GoldenEmbedParserMain(args);
	}

	public static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
	}

	public GoldenEmbedParserMain()
	{
		power = new Power();
	}
	
	private void initGCFile()
	{
	    fout.write("<!DOCTYPE GoldenCheetah>\n");
	    fout.write("<ride>\n");
	    fout.write(spacer1 + "<attributes>\n");
	    fout.write(spacer2 + "<attribute key=\"Start time\" value=\"2010/01/01 00:00:00 UTC\" />\n");
	    fout.write(spacer2 + "<attribute key=\"Device type\" value=\"Golden Embed\" />\n");
	    fout.write(spacer1 + "</attributes>\n");
	    fout.write("<samples>\n");
	}
	public GoldenEmbedParserMain(String[] args) 
	{
		// Load up the file
		File file;
		power = new Power();
		GoldenCheetah gc = new GoldenCheetah();
		
		if(args.length  != 1)
			file = new File("/Volumes//DATALOGGER//LOG07.txt");
		else
			file = new File(args[0]);
		
		String parentDir = file.getParent();
		
		File outFile = new File(parentDir+"/2010_01_01_00_00_00.gc");
		try {
			fout = new PrintWriter(new FileOutputStream(outFile));
			initGCFile();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
            System.exit(1);
		}
		
		System.out.println("\n"+file.getAbsolutePath()+"\n");
		
		try {
			ANTrxHandler(getBytesFromFile(file), gc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int ANTrxMsg(byte[] rxIN, int i, int size, GoldenCheetah gc) 
	{
		    //System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(rxIN[i]));
			switch (rxIN[i]) 
			{
				case MESG_RESPONSE_EVENT_ID:
					System.out.println("ID: MESG_RESPONSE_EVENT_ID\n");
					i = ANTresponseHandler(rxIN, i, size, gc);
					break;
				case MESG_CAPABILITIES_ID:
					System.out.println("ID: MESG_CAPABILITIES_ID\n");
					i = ANTCfgCapabilties(i, size); // rxBuf[3] .. skip sync, size, msg
					break;
				case MESG_BROADCAST_DATA_ID:
					System.out.println("ID: MESG_BROADCAST_DATA_ID\n");
					if(gc.getChannel() == 0)
					    i = ANTparseHRM(rxIN, i+3, gc);
					else
					    i = ANTParsePower(rxIN, i+4, size, gc);
					break;
				case MESG_CHANNEL_ID_ID:
					System.out.println("ID: MESG_CHANNEL_ID_ID\n");
					i = ANTChannelID(rxIN, ++i, gc);
					break;
					
					
				default:
					//System.out.println("ID: Unknown 0x" + UnicodeFormatter.byteToHex(rxIN[i]));
			}
			return i;
	}

	public int ANTChannelID(byte[] msgIN, int pos, GoldenCheetah gc)
	{
		byte[] devNo = new byte[2];
		
		pos +=2;
		devNo[0] = msgIN[pos];

		pos--;
		devNo[1] = msgIN[pos];
		
		int deviceNum = byteArrayToInt(devNo, 0, 2);
		System.out.println("Device Number is: " + deviceNum);

		pos += 2;
		System.out.println("Device Type is: 0x" + UnicodeFormatter.byteToHex(msgIN[pos]));
		System.out.println("Man ID is: 0x" + UnicodeFormatter.byteToHex(msgIN[++pos])+"\n");

		pos +=2;
		pos = setTimeStamp(msgIN, pos, gc);
		
		return --pos; 
	}
	
	
	public int setTimeStamp(byte[] msgData, int i, GoldenCheetah gc)
	{
		Byte hr;
	    Byte min;
	    Byte sec;
		
        //Print out the time stamp
        hr = new Byte(msgData[i++]);
        min = new Byte(msgData[i++]);
        sec = new Byte(msgData[i++]);
        
        gc.setSecs((hr*60*60)+(min*60)+sec);
        
        
        System.out.println("Time stamp: "  + hr.intValue() +":"+min.intValue()+":"+sec.intValue());

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
		return bytes;
	}

	
	public int ANTParsePower(byte[] msgData, int i, int size, GoldenCheetah gc)
	{
		int t1;
		int p1;
		int r1;
		
		int end = i + size -2;
		double rdiff = 0;
		double pdiff = 0;
		double tdiff = 0;
		double nm = 0;
		double rpm = 0;
		double watts = 0;
		Byte aByte;
		int msgN = 0;
		
		for (; i < end; i++) 
		{
			//System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");
			if(msgN == 0)
			{
	   			 if(power.first12)
	   			 {
    			     //Just store it.
				     aByte = new Byte(msgData[i]);
	                 power.setR(aByte.intValue());
	                 //System.out.println("R: " + aByte.intValue());
	   			 }
	   			 else
	   			 {
	   				 //We can calculate and then store
	  			     aByte = new Byte(msgData[i]);
	 				 r1 =  aByte.intValue();
	 				 rdiff = power.getR() - r1;
	  			     power.setR(aByte.intValue());
	                 //System.out.println("rdiff is: " + rdiff);
	   			 }
                 msgN++;
			}
			else if(msgN == 1)
			{
				byte[] pRdiff = new byte[2];
				i++;
				pRdiff[1] = msgData[i];
				//System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");
				i++;
				pRdiff[0] = msgData[i];
				//System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");
				p1 = byteArrayToInt(pRdiff, 0, 2);

				if(power.first12)
				{
					power.setP(p1);
					//System.out.println("P1: " + p1);
				}
				else
				{
					pdiff =  power.getP() - p1;
					power.setP(p1);
					//System.out.println("pdiff is: " + pdiff);
				}			
				msgN++;
			}
			else if(msgN == 2)
			{
				byte[] pRdiff = new byte[2];
				pRdiff[1] = msgData[i];
				//System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");
				i++;
				pRdiff[0] = msgData[i];
				//System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");

				t1 = byteArrayToInt(pRdiff, 0, 2);
				
				if(power.first12)
				{
		            power.setT(t1);
		            //System.out.println("T: " + t1);
				}
				else
				{
		             tdiff = power.getT() - t1;
		             power.setT(t1);
					 //System.out.println("tdiff is: " + tdiff);
				}

				i++;				
				msgN++;
			}
		}
        if(tdiff != 0 && rdiff != 0)
        {
		    nm = (float)Math.abs(tdiff)/(float)(Math.abs(rdiff)*32.0);
            rpm = (double)Math.abs(rdiff)*122880.0/(double)Math.abs(pdiff);
            watts = rpm*nm*2*PI/60;
            System.out.println("nm: " + nm + " rpm: " + rpm + " watts: " + watts + "\n");
            
            gc.setCad((int)rpm);
            gc.setWatts((int)watts);
    		i = setTimeStamp(msgData, i, gc);
            writeGCRecord(gc);
        }
        else
    		i = setTimeStamp(msgData, i, gc);
        	
        if(power.first12)
        	power.first12 = false;
        
        //System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i])+"\n");
		
		return --i; //For Loop will advance itself.
		
	}
	
	private int ANTparseHRM(byte[] msgData, int i, GoldenCheetah gc) {

		byte aByte;
		int end = i + 8;
		int hrCountFinder = 0;
		int hr = 0;

		for (; i < end; i++) {
			aByte = msgData[i];
			//System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(msgData[i]));
			if (hrCountFinder == 6) { //HR is the sixth byte
	//			System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(msgData[i]));
				hr = unsignedByteToInt(aByte);
				System.out.println("Heart Rate is: " + hr);
			}
			 //else
			 //    System.out.println("o" + i + "=" + unsignedByteToInt(aByte));
			hrCountFinder++;
		}

		i = setTimeStamp(msgData, i, gc);
        gc.setHr(hr);
		writeGCRecord(gc);
		return --i; //For Loop will advance itself.
	}

	private void ANTrxHandler(byte[] rxBuf, GoldenCheetah gc )
	{
		int msgN = 0;
		int i;
		int size = 0;
		boolean inMsg = true;
		
		for (i = 0; i < rxBuf.length; i++)
	    {
			//System.out.println("0x"+ UnicodeFormatter.byteToHex(rxBuf[i]));
			
	    	if (rxBuf[i] == MESG_TX_SYNC && inMsg)
	        {
	    		    inMsg = false;
	        		msgN = 0; // Always reset msg count if we get a sync
	                msgN++;
	                errorFlag = false;
	                totalTrans++;
	                //System.out.println("RX: [sync]");
	         }
	         else if (msgN == 1)
	         {
	        	 Byte aByte = new Byte(rxBuf[i]);
	             msgN++; //Size
	             size = aByte.intValue();
	         }
	         else
	         {
	        	 if(rxBuf.length < size+3+i-2)
	        	 {
	        		 System.out.println("\n\nTotal Errors: " + totalErrors);
	        		 System.out.println("Total Messages " + totalTrans);
	        		 System.out.println("%: " +totalErrors / totalTrans * 100.0);
	        		 System.exit(0); //EOF
	        	 }
	        	 byte checksum = checkSum(rxBuf, size, i-2);
	             if (checksum == rxBuf[size+i+1]) // Check if chksum = msg chksum
	             {
	            	 inMsg = true;
	            	 // Handle Message
	                 i = ANTrxMsg(rxBuf, i, size, gc);
	             }
	             else
	             {
		        	 //System.out.println("CheckSum Mismatch 0x"+ UnicodeFormatter.byteToHex(rxBuf[size+i+1]) + "!=: 0x" + UnicodeFormatter.byteToHex(checksum));
	                 msgN = 0;
	                 inMsg = true; 
	                 if(errorFlag == false)
	                 {
	                	 totalErrors++;
	                	 errorFlag = true;
	                 }
	             } 
	         }
          }
		
		 closeGCFile();
		 System.out.println("\n\nTotal Errors: " + totalErrors);
		 System.out.println("Total Messages " + totalTrans);
		 System.out.println("%: " + (totalErrors / totalTrans) * 100.0);

    }
	
	private void closeGCFile()
	{
		fout.write(spacer1 + "</samples>\n");
		fout.write("</ride>\n");
		fout.flush();
		fout.close();
	}
	private byte checkSum(byte data[], int length, int pos)
	{

	        byte chksum = 0x0;

	        for (int i = pos; i < length+3+pos; i++)
	        {  
	        	    //System.out.println("Checksum: 0x"+ UnicodeFormatter.byteToHex(data[i]));
	                chksum ^= data[i];  // +1 since skip prefix sync code, we already counted it
	        }
	        
	        return chksum;
	}
	
	private int ANTresponseHandler(byte rxBuf[], int pos, int size, GoldenCheetah gc)
	{
		    pos++;
	        byte ch = rxBuf[0+pos];
	        byte id = rxBuf[1+pos];
	        byte code = rxBuf[2+pos];

	        System.out.println("Channel Num:" + UnicodeFormatter.byteToHex(ch));
	        Byte aByte = new Byte(code);
	        gc.setChannel(aByte.intValue()); //Store the channel.
	        System.out.println("Message ID: " + UnicodeFormatter.byteToHex(id));
	        System.out.println("Code: " + UnicodeFormatter.byteToHex(code));

	        switch (id)
	        {
	                case MESG_CHANNEL_SEARCH_TIMEOUT_ID:
	                        System.out.println("[MESG_CHANNEL_SEARCH_TIMEOUT_ID]");
	                        break;
	                case MESG_ASSIGN_CHANNEL_ID :
	                        System.out.println("[MESG_ASSIGN_CHANNEL_ID]");
	                        break;
	                case MESG_CHANNEL_RADIO_FREQ_ID :
	                        System.out.println("[MESG_CHANNEL_RADIO_FREQ_ID]");
	                        break;
	                case MESG_CHANNEL_MESG_PERIOD_ID :
	                        System.out.println("[MESG_CHANNEL_MESG_PERIOD_ID]");
	                        break;
	                case MESG_OPEN_CHANNEL_ID :
	                        System.out.println("[MESG_OPEN_CHANNEL_ID]");
	                        break;
	                case MESG_CHANNEL_ID_ID :
	                        System.out.println("[MESG_CHANNEL_ID_ID]");
	                        break;
	                case MESG_NETWORK_KEY_ID :
	                        System.out.println("[MESG_NETWORK_KEY_ID]");
	                        break;
	                default :
	                        System.out.println("[unknown]: "+ UnicodeFormatter.byteToHex(id));
	                        break;
	        }

	        pos = setTimeStamp(rxBuf, pos+4, gc);

	        return --pos; //For Loop will move 1 forward
	}
	
	private int ANTCfgCapabilties(int i, int size)
	{
		return i+size+4;
	}	
	
    public static int byteArrayToInt(byte[] b, int offset, int size) {
        int value = 0;
        for (int i = 0; i < size; i++) {
            int shift = (size - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    private void writeGCRecord(GoldenCheetah gc)
    {
        fout.write(spacer1 + "<sample cad=\""+gc.getCad()+ "\" watts=\""+ gc.getWatts() + "\" secs=\"" + gc.getSecs() +"\" hr=\""+gc.getHr()+"\" len=\"1\"/>\n");	
    }
}
