/**
 * @author cramakrishnan
 *
 * Copyright (C) 2003, C. Ramakrishnan / Illposed Software All rights reserved.
 *
 * See license.txt (or license.rtf) for license information.
 *
 *
 * OSCJavaToByteArrayConverter is a helper class that translates from Java types
 * to the format the OSC spec specifies for those types.
 *
 * This implementation is based on Markus Gaelli and Iannis Zannos' OSC
 * implementation in Squeak: http://www.emergent.de/Goodies/
 */
package com.illposed.osc.utility;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class OSCJavaToByteArrayConverter {

    protected ByteArrayOutputStream stream = new ByteArrayOutputStream();

    /*public OSCJavaToByteArrayConverter() {
     super();
     }*/
    /**
     * Creation date: (2/23/2001 2:43:25 AM)
     *
     *
     */
    public void appendNullCharToAlignStream() {
        int mod = stream.size() % 4;
        int pad = 4 - mod;
        for (int i = 0; i < pad; i++) {
            stream.write(0);
        }
    }

    /**
     * Creation date: (2/23/2001 2:21:53 AM)
     *
     * @return byte[]
     */
    public byte[] toByteArray() {
        return stream.toByteArray();
    }

    /**
     * Creation date: (2/23/2001 2:14:23 AM)
     *
     * @param bytes byte[]
     */
    public void write(byte[] bytes) {
        writeBigEndToFourByteBoundry(bytes);
    }

    /**
     * Creation date: (2/23/2001 2:21:04 AM)
     *
     * @param i int
     */
    public void write(int i) {
        writeIntegerToByteArray(i);
    }

    /**
     * Creation date: (2/23/2001 2:03:57 AM)
     *
     * @param f java.lang.Float
     */
    public void write(Float f) {
        writeIntegerToByteArray(Float.floatToIntBits(f));
    }

    /**
     * Creation date: (2/23/2001 2:08:36 AM)
     *
     * @param i java.lang.Integer
     */
    public void write(Integer i) {
        writeIntegerToByteArray(i);
    }

    /**
     * Creation date: (2/23/2001 1:57:35 AM)
     *
     * @param str java.lang.String
     */
    public void write(String str) {
        writeLittleEndToFourByteBoundry(str.getBytes());
    }

    /**
     * Creation date: (2/23/2001 2:08:36 AM)
     *
     * @param c char
     */
    public void write(char c) {
        stream.write(c);
    }

    /**
     * Creation date: (2/23/2001 2:02:54 AM)
     *
     * @param anObject java.lang.Object
     */
    public void write(Object anObject) {
        // Can't do switch on class
        if (null == anObject) {
            return;
        }
        if (anObject instanceof Float) {
            write((Float) anObject);
            return;
        }
        if (anObject instanceof String) {
            write((String) anObject);
            return;
        }
        if (anObject instanceof Integer) {
            write((Integer) anObject);
        }
    }

    /**
     * Creation date: (2/23/2001 2:43:25 AM)
     *
     * @param c
     */
    public void writeType(Class<?> c) {
        // A big ol' case statement -- what's polymorphism mean, again?
        // I really wish I could extend the base classes!

        // use the appropriate flags to tell SuperCollider what kind of 
        // thing it is looking at
        if (Integer.class.equals(c)) {
            stream.write('i');
        } else if (java.math.BigInteger.class.equals(c)) {
            stream.write('h');
        } else if (Float.class.equals(c)) {
            stream.write('f');
        } else if (Double.class.equals(c)) {
            stream.write('d');
        } else if (String.class.equals(c)) {
            stream.write('s');
        } else if (Character.class.equals(c)) {
            stream.write('c');
        }
    }

    /**
     * Creation date: (2/23/2001 2:43:25 AM)
     *
     * @param array
     */
    public void writeTypesArray(Object[] array) {
        // A big ol' case statement in a for loop -- what's polymorphism mean, again?
        // I really wish I could extend the base classes!
        for (Object array1 : array) {
            if (null == array1) {
            } else  if (array1.getClass().isArray()) {
                // if the array at i is a type of array write a [
                // This is used for nested arguments
                stream.write('[');
                // fill the [] with the SuperCollider types corresponding to the object
                // (i.e. Object of type String needs -s).
                writeTypesArray((Object[]) array1);
                // close the array
                stream.write(']');
            } else if (Boolean.TRUE.equals(array1)) {
                // Create a way to deal with Boolean type objects
                stream.write('T');
            } else if (Boolean.FALSE.equals(array1)) {
                // Create a way to deal with Boolean type objects
                stream.write('F');
            } else {
                // go through the array and write the superCollider types as shown in the 
                // above method. the Classes derived here are used as the arg to the above method
                writeType(array1.getClass());
            }
        }
        // align the stream with padded bytes
        appendNullCharToAlignStream();
    }

    /**
     * @param vector the collection I am to write out types for
     */
    public void writeTypes(ArrayList<Object> vector) {
        writeTypesArray(vector.toArray(new Object[]{}));
    }

    /**
     * convert an integer to byte array
     *
     * @param value int
     */
    private void writeIntegerToByteArray(int value) {
        byte[] intBytes = new byte[4];

        intBytes[3] = (byte) value;
        value >>>= 8;
        intBytes[2] = (byte) value;
        value >>>= 8;
        intBytes[1] = (byte) value;
        value >>>= 8;
        intBytes[0] = (byte) value;

        try {
            stream.write(intBytes);
        } catch (IOException e) {
            throw new RuntimeException("You're screwed: IOException writing to a ByteArrayOutputStream");
        }
    }

    /**
     * Line up the BigEnd of the bytes to a 4 byte boundary
     *
     * @param bytes byte[]
     */
    private void writeBigEndToFourByteBoundry(byte[] bytes) {
        int mod = bytes.length % 4;
        // if the remainder == 0 write the bytes
        if (mod == 0) {
            try {
                stream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException("You're screwed: IOException writing to a ByteArrayOutputStream");
            }
            return;
        }
        // pad the bytes to lineup correctly
        int pad = 4 - mod;
        byte[] newBytes = new byte[pad + bytes.length];
        System.arraycopy(bytes, 0, newBytes, pad, bytes.length);

        try {
            stream.write(newBytes);
        } catch (IOException e) {
            throw new RuntimeException("You're screwed: IOException writing to a ByteArrayOutputStream");
        }
    }

    /**
     * Line up the LittleEnd of the bytes to a 4 byte boundary
     *
     * @param bytes byte[]
     */
    private void writeLittleEndToFourByteBoundry(byte[] bytes) {
        int mod = bytes.length % 4;
        // if the remainder == 0 write the bytes
        if (mod == 4) {
            try {
                stream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException("You're screwed: IOException writing to a ByteArrayOutputStream");
            }
            return;
        }
        // pad the bytes to lineup correctly
        int pad = 4 - mod;
        byte[] newBytes = new byte[pad + bytes.length];
        System.arraycopy(bytes, 0, newBytes, 0, bytes.length);

        try {
            stream.write(newBytes);
        } catch (IOException e) {
            throw new RuntimeException("You're screwed: IOException writing to a ByteArrayOutputStream");
        }
    }

}
