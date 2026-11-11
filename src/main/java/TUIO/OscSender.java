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

import TUIO.osc.OscPacketBuilder;

/**
 * The OscSender class is the base interface for the various OSC transport
 * methods such as UDP, TCP ...
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public interface OscSender {

	/**
	 * Sends the OSC packet assembled in the provided OutboundPacketStream.
	 *
	 * @param	packet	the OSC packet to send
	 * @return	true if the packet was sent successfully
	 */
	boolean sendOscPacket(OscPacketBuilder packet);

	/**
	 * Sends the provided raw OSC packet bytes.
	 *
	 * @param	packet	the raw OSC packet bytes to send
	 * @return	true if the packet was sent successfully
	 */
	boolean sendPacket(byte[] packet);

	/**
	 * Returns true if this OscSender is currently connected.
	 *
	 * @return	true if this OscSender is currently connected
	 */
	boolean isConnected();

	/**
	 * Returns the maximum OSC packet size of this OscSender in bytes.
	 *
	 * @return	the maximum OSC packet size in bytes
	 */
	int getBufferSize();

	/**
	 * Returns whether this OscSender delivers locally
	 * (e.g. to a host name different from a network address).
	 *
	 * @return	true if this OscSender delivers locally
	 */
	boolean isLocal();

	/**
	 * Closes the connection and releases all resources.
	 */
	void close();

	/**
	 * Returns a string describing the OSC transport type of this OscSender
	 *
	 * @return	a string describing the OSC transport type
	 */
	String getTransportType();
}
