/* $Id: OSCPacketDispatcher.java,v 1.2 2008/07/01 15:29:46 modin Exp $
 * Created on 28.10.2003
 */
package com.illposed.osc.utility;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;
import java.util.Date;
import java.util.HashMap;

/**
 * @author cramakrishnan
 *
 * Copyright (C) 2003, C. Ramakrishnan / Auracle All rights reserved.
 *
 * See license.txt (or license.rtf) for license information.
 *
 * Dispatches OSCMessages to registered listeners.
 *
 */
public class OSCPacketDispatcher {

    private final HashMap<String, OSCListener> addressToClassTable = new HashMap<String, OSCListener>();

    /**
     *
     */
    public OSCPacketDispatcher() {
        super();
    }

    public void addListener(String address, OSCListener listener) {
        addressToClassTable.put(address, listener);
    }

    public void dispatchPacket(OSCPacket packet) {
        if (packet instanceof OSCBundle) {
            dispatchBundle((OSCBundle) packet);
        } else {
            dispatchMessage((OSCMessage) packet);
        }
    }

    public void dispatchPacket(OSCPacket packet, Date timestamp) {
        if (packet instanceof OSCBundle) {
            dispatchBundle((OSCBundle) packet);
        } else {
            dispatchMessage((OSCMessage) packet, timestamp);
        }
    }

    private void dispatchBundle(OSCBundle bundle) {
        Date timestamp = bundle.getTimestamp();
        OSCPacket[] packets = bundle.getPackets();
        for (OSCPacket packet : packets) {
            dispatchPacket(packet, timestamp);
        }
    }

    private void dispatchMessage(OSCMessage message) {
        dispatchMessage(message, null);
    }

    private void dispatchMessage(OSCMessage message, Date time) {
        if (addressToClassTable.containsKey(message.getAddress())) {
            addressToClassTable.get(message.getAddress()).acceptMessage(time, message);
        }
    }
}
