/**
 * @author cramakrishnan
 *
 * Copyright (C) 2003, C. Ramakrishnan / Illposed Software All rights reserved.
 *
 * See license.txt (or license.rtf) for license information.
 *
 *
 * An simple (non-bundle) OSC message. An OSC message is made up of an address
 * (who is this message sent to) and arguments (what is the contents of this
 * message).
 */
package com.illposed.osc;

import com.illposed.osc.utility.OSCJavaToByteArrayConverter;
import java.util.ArrayList;

public class OSCMessage extends OSCPacket {

    protected String address;
    protected ArrayList<Object> arguments;

    /**
     * Create an empty OSC Message In order to send this osc message, you need
     * to set the address and, perhaps, some arguments.
     */
    public OSCMessage() {
        super();
        arguments = new ArrayList<Object>();
    }

    /**
     * Create an OSCMessage with an address already initialized
     *
     * @param newAddress The recepient of this OSC message
     */
    public OSCMessage(String newAddress) {
        this(newAddress, null);
    }

    /**
     * Create an OSCMessage with an address and arguments already initialized
     *
     * @param newAddress The recepient of this OSC message
     * @param newArguments The data sent to the receiver
     */
    public OSCMessage(String newAddress, Object[] newArguments) {
        super();
        address = newAddress;
        if (null != newArguments) {
            arguments = new ArrayList<Object>(newArguments.length);
            for (int i = 0; i < newArguments.length; i++) {
                arguments.add(newArguments[i]);
            }
        } else {
            arguments = new ArrayList<Object>();
        }
        init();
    }

    /**
     * @return the address of this OSC Message
     */
    public String getAddress() {
        return address;
    }

    /**
     * Set the address of this messsage
     *
     * @param anAddress
     */
    public void setAddress(String anAddress) {
        address = anAddress;
    }

    public void addArgument(Object argument) {
        arguments.add(argument);
    }

    public Object[] getArguments() {
        return arguments.toArray();
    }

    /**
     * @param stream OscPacketByteArrayConverter
     */
    protected void computeAddressByteArray(OSCJavaToByteArrayConverter stream) {
        stream.write(address);
    }

    /**
     * @param stream OscPacketByteArrayConverter
     */
    protected void computeArgumentsByteArray(OSCJavaToByteArrayConverter stream) {
		// SC starting at version 2.2.10 wants a comma at the beginning
        // of the arguments array.
        stream.write(',');
        if (null == arguments) {
            return;
        }
        stream.writeTypes(arguments);
        for(Object obj : arguments) {
            stream.write(obj);
        }
    }

    /**
     * @param stream OscPacketByteArrayConverter
     */
    protected void computeByteArray(OSCJavaToByteArrayConverter stream) {
        computeAddressByteArray(stream);
        computeArgumentsByteArray(stream);
        byteArray = stream.toByteArray();
    }

}
