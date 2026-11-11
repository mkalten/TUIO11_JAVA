/*
 * TUIO Java library
 * Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
*/

package TUIO;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import TUIO.osc.MalformedBundleException;
import TUIO.osc.OscBundle;
import TUIO.osc.OscMessage;
import TUIO.osc.OscPacket;

/**
 * The OscReceiver is the base class for the various OSC transport methods
 * such as UDP, TCP ...
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public abstract class OscReceiver {

	protected final List<TuioClient> clientList = new CopyOnWriteArrayList<TuioClient>();
	protected boolean connected = false;

	/**
	 * The OscReceiver connects and starts receiving TUIO messages via OSC.
	 * With the default parameter the OSC receiver runs in a dedicated background thread,
	 * when set to true the method blocks in the current thread until disconnect() is invoked.
	 *
	 * @param	lock	blocks in the current thread if set to true, runs in the background if set to false (default)
	 */
	public abstract void connect(boolean lock);

	/**
	 * The OscReceiver connects and starts receiving TUIO messages via OSC
	 * in a dedicated background thread.
	 */
	public void connect() {
		connect(false);
	}

	/**
	 * The OscReceiver disconnects and stops receiving TUIO messages via OSC.
	 * This method waits for the receiver threads to terminate and therefore
	 * must not be called from within a TuioListener callback.
	 */
	public abstract void disconnect();

	/**
	 * Returns true if this OscReceiver is currently connected.
	 * @return	true if this OscReceiver is currently connected
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Attaches the provided TuioClient to this OscReceiver
	 *
	 * @param	client	the TuioClient to attach
	 */
	public void addTuioClient(TuioClient client) {
		clientList.add(client);
	}

	/**
	 * The OSC callback method where the incoming OSC data is received
	 *
	 * @param	data	the received OSC data
	 */
	public void processPacket(byte[] data) {
		processPacket(data, 0, data.length);
	}

	/**
	 * The OSC callback method where the incoming OSC data is received
	 *
	 * @param	data		the received OSC data
	 * @param	offset		the offset of the OSC data within data
	 * @param	length		the length of the received OSC data
	 */
	public void processPacket(byte[] data, int offset, int length) {
		try {
			for (TuioClient client : clientList) client.processBundleBegin();

			Object packet = OscPacket.parse(data, offset, length);
			if (packet instanceof OscBundle)
				processBundle((OscBundle) packet);
			else if (packet instanceof OscMessage)
				processMessage((OscMessage) packet);
		} catch (MalformedBundleException e) {
			System.err.println("malformed OSC bundle: " + e.getMessage());
		}
	}

	protected void processBundle(OscBundle bundle) {
		try {
			for (Object element : bundle.getElements()) {
				if (element instanceof OscBundle)
					processBundle((OscBundle) element);
				else if (element instanceof OscMessage)
					processMessage((OscMessage) element);
			}
		} catch (RuntimeException e) {
			System.err.println("malformed OSC bundle: " + e.getMessage());
		}
	}

	protected void processMessage(OscMessage message) {
		for (TuioClient client : clientList) client.processOSC(message);
	}
}
