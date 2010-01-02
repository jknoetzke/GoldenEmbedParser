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
import java.io.IOException;
import java.io.InputStream;

public class GoldenEmbedParserMain 
{

	static final byte MESG_RESPONSE_EVENT_ID = 0x40;
	static final byte MESG_CAPABILITIES_ID = 0x54;
	static final byte MESG_BROADCAST_DATA_ID = 0x4E;
	static final byte MESG_TX_SYNC = (byte)0xA4;

	int debug = 0;

	/**
	 * @param args
	 */

	// [0x09]..[0x4e]..[0x00]..[0x50]..[0xa0]..[0x01]..[0x08]..[0xa5]..[0x7f]..[0xff]..[0x5c]..[0x63]
	// a4 09 4e 00 50 a0 01 08 db a1 32 66 34
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		new GoldenEmbedParserMain();

		// [0x00]..[0x50]..[0xa0]..[0x01]..[0x08]..[0xe1]..[0x3f]..[0xdb]..[0x50]..[0x4f]
		// byte someBytes[] = new byte[10];
		/*
		 * someBytes[0] = (byte)0x00; someBytes[1] = (byte)0x50; someBytes[2] =
		 * (byte)0xA0; someBytes[3] = (byte)0x01; someBytes[4] = (byte)0x08;
		 * someBytes[5] = (byte)0xE1; someBytes[6] = (byte)0x3F; someBytes[7] =
		 * (byte)0xDB; someBytes[8] = (byte)0x50; someBytes[9] = (byte)0x4F;
		 * 
		 * for (int x=0; x < 9; x++) { Byte aByte = someBytes[x];
		 * System.out.println("Converting 0x" +
		 * UnicodeFormatter.byteToHex(aByte)); //System.out.println("o" + x +
		 * "=" + aByte.intValue()); System.out.println("o" + x + "=" +
		 * unsignedByteToInt(aByte)); }
		 */
	}

	public static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
	}

	GoldenEmbedParserMain() {
		// Load up the file
		File file = new File("//Users//jknotzke//Dropbox//LOG06.txt");
		try {
			ANTrxHandler(getBytesFromFile(file));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int ANTrxMsg(byte[] rxIN, int i) 
	{
			switch (rxIN[i]) 
			{
				case MESG_RESPONSE_EVENT_ID:
					// System.out.println("ID: MESG_RESPONSE_EVENT_ID\n");
					ANTResponseHandler(rxIN, i);
					break;
				case MESG_CAPABILITIES_ID:
					// System.out.println("ID: MESG_CAPABILITIES_ID\n");
					ANTCfgCapabilties(rxIN, i); // rxBuf[3] .. skip sync, size, msg
					break;
				case MESG_BROADCAST_DATA_ID:
					// System.out.println("ID: MESG_BROADCAST_DATA_ID\n");
					i = ANTparseHRM(rxIN, i);
					break;
				default:
					// System.out.println("ID: Unknown 0x" +
					// UnicodeFormatter.byteToHex(rxIN[i]));
			}
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

	private int ANTResponseHandler(byte[] msgData, int i) {
		return 0;
	}

	private int ANTCfgCapabilties(byte[] msgData, int i) {
		return 0;
	}

	private int ANTparseHRM(byte[] msgData, int i) {

		byte aByte;
		int end = i + 11;
		int hrCountFinder = 0;

		for (; i < end; i++) {
			aByte = msgData[i];
			System.out.println("Converting 0x"
					+ UnicodeFormatter.byteToHex(msgData[i]));
			if (hrCountFinder == 9) {
				int hr = unsignedByteToInt(aByte);
				System.out.println("Heart Rate is: " + hr);
				debug++;
			}
			// else
			// System.out.println("o" + i + "=" + unsignedByteToInt(aByte));
			hrCountFinder++;
		}

		return i;
	}

	private void ANTrxHandler(byte[] rxBuf )
	{

		boolean inMsg = false;
		int msgN = 0;
		
		
		for (int i = 0; i < rxBuf.length; i++)
	    {
	    	if ((rxBuf[i] == MESG_TX_SYNC) && (inMsg == false))
	        {
	        		msgN = 0; // Always reset msg count if we get a sync
	                inMsg = true;
	                msgN++;
	                System.out.println("RX: [sync]");
	         }
	         else if (msgN == 1)
	         {
	             msgN++;
	         }
	         else if (msgN == 2)
	         {
	        	 msgN++;
	         }
	         else if (msgN < rxBuf[1]+3) // sync, size, checksum x 1 byte
	         {
	        	 msgN++;
	         }
	         else
	         {
	        	 inMsg = false;
	             if (checkSum(rxBuf, msgN) == rxBuf[msgN]) // Check if chksum = msg chksum
	             {
	            	 // Handle Message
	                 ANTrxMsg(rxBuf, i);
	             }
	             else
	             {
	            	 System.out.println("RX: chksum mismatch");
                 }
              }
          }
    }
	
	private byte checkSum(byte data[], int length)
	{
	        int i;
	        byte chksum = data[0];

	        for (i = 1; i < length; i++)
	                chksum ^= data[i];  // +1 since skip prefix sync code, we already counted it

	        return chksum;
	}

}