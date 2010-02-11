/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shampoo.goldenembed.parser;

import java.nio.ByteBuffer;

/**
 *
 * Written by Evangelos Haleplidis, e_halep at yahoo dot gr, and Sean
 * R Owens, sean at guild dot net, released to the public domain.  Share
 * and enjoy.  Since some people argue that it is impossible to release
 * software to the public domain, you are also free to use this code
 * under any version of the GPL, LPGL, or BSD licenses, or contact the
 * authors for use of another license.
 *
 * The unsigned Short Class.
 * @author Sean Owens [sean at guild dot net].
 * @author Ehalep [e_halep at yahoo dot gr].
 */

public class uShort {

    private char unShort;

     /**
     * Unsigned Short constructor with no value.
     */
    public uShort(){
    }

    /**
     * Unsigned Short constructor with an already read unsigned short to be converted to Java format.
     * @param unsigned The already read unsigned short that needs to be converted to Java format.
     */
    public uShort(short unsigned){
        this.read(unsigned);
    }

    /**
     * Unsigned Short constructor with a value in Java format.
     * @param tobecomeunsigned The value in javaformat.
     */
    public uShort(char tobecomeunsigned){
        this.setValue(tobecomeunsigned);
    }

    /**
     * Unsigned Short constructor that read gets an already read unsigned Short
     * that exists inside a ByteBuffer at position offset and converts it into
     * the proper format for Java
     * Caution, the position of the bytebuffer is not changed inside the constructor.
     * @param bb The ByteBuffer in which the already read unsigned Short exists
     * @param offset The position that the 2 bytes of the unsigned Short starts
     */
    public uShort(ByteBuffer bb, int offset){
        this.read(bb, offset);
    }

    /**
     * Unsigned Short constructor that gets an already read unsigned Integer
     * that exists inside a Byte Array at position offset and converts it into
     * the proper format for Java
     * @param bytes The byte array
     * @param offset The position that the 2 bytes of the unsigned integer starts
     */
    public uShort(byte[] bytes, int offset){
        this.read(bytes, offset);
    }

    /**
     * Read gets an already read unsigned Short and converts it into the proper format for Java
     * @param i The read unsigned short that needs to be converted to Java format.
     */
    public void read(short i){

        byte[] buf = new byte[2];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.putShort(i);

        int firstByte = (0x000000FF & ((int)buf[0]));
        int secondByte = (0x000000FF & ((int)buf[1]));

        unShort  = (char)(firstByte << 8 | secondByte);
    }

    /**
     * Read gets an already read unsigned Short that exists inside a ByteBuffer at position offset and converts it into the proper format for Java
     * Caution, the position of the bytebuffer is not changed inside this read call.
     * @param bb The ByteBuffer in which the already read unsigned Short exists
     * @param offset The position that the 2 bytes of the unsigned Short starts
     */
    public void read(ByteBuffer bb, int offset){

        int initial_pos = bb.position();
        bb.position(offset);

        int firstByte = (0x000000FF & ((int)bb.get()));
        int secondByte = (0x000000FF & ((int)bb.get()));

        unShort  = (char)(firstByte << 8 | secondByte);
        bb.position(initial_pos);
    }

    /**
     * Read gets an already read unsigned Integer that exists inside a Byte Array at position offset and converts it into the proper format for Java
     * @param bytes The byte array
     * @param offset The position that the 2 bytes of the unsigned integer starts
     */
    public void read(byte[] bytes, int offset){

        int firstByte = (0x000000FF & ((int)bytes[offset]));
        int secondByte = (0x000000FF & ((int)bytes[offset+1]));

        unShort  = (char)(firstByte << 8 | secondByte);
   }

    /**
     * WriteUnShort prepares the unsigned Shot from Java format to be written into the proper unsigned format for the wire.
     * @return Returns the unsigned short in a byte Array format
     */
    public byte[] write(){
        byte[] buf = new byte[2];

    	buf[0] = (byte)((unShort & 0xFF00) >> 8);
    	buf[1] = (byte)(unShort & 0x00FF);

        return buf;
    }

    /**
     * Write prepares the unsigned Short from Java format to be written into the proper unsigned format for the wire and puts it at a ByteBuffer at position equal to offset.
     * @param bb The Bytebuffer to put the Unsigned Short
     * @param offset The position to start putting the 2 bytes
     * @return Returns true if the short was written, false if the offset was wrong.
     */
    public boolean write(ByteBuffer bb, int offset){
        if(bb.limit()-2<offset){
            return false;
        }
        bb.position(offset);

        byte[] buf = new byte[2];

    	buf[0] = (byte)((unShort & 0xFF00) >> 8);
    	buf[1] = (byte)(unShort & 0x00FF);

        bb.put(buf);
        return true;
    }

    /**
     * Write prepares the unsigned Short from Java format to be written into the proper unsigned format for the wire and puts it at a ByteArray at position equal to offset.
     * @param bytes The Byte Array to put the Unsigned Short
     * @param offset The position to start putting the 2 bytes
     * @return Returns true if the short was written, false if the offset was wrong.
     */
    public boolean write(byte[] bytes, int offset){
        if(bytes.length-2<offset){
            return false;
        }

    	bytes[offset] = (byte)((unShort & 0xFF00) >> 8);
    	bytes[offset+1] = (byte)(unShort & 0x00FF);

        return true;
    }

    /**
     * getValue returns the actual value of the unsigned short. Caution, returns a char.
     * @return Returns the actual value of the unsigned short, in char data type.
     */
    public char getValue(){
        return unShort;
    }

    /**
     * SetValue sets a unSigned short value.
     * @param value The value in char format.
     */
    public void setValue(char value){
        unShort = value;
    }

}
