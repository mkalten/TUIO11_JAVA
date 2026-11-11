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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import TUIO.osc.OscPacketBuilder;

/**
 * The UdpSender implements the UDP transport method for OSC
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public class UdpSender implements OscSender {

	private DatagramSocket socket;
	private InetAddress address;
	private int port;
	private int bufferSize;
	private boolean connected = false;

	/**
	 * The default constructor creates a UdpSender that sends to the
	 * default UDP port 3333 on localhost
	 */
	public UdpSender() {
		this("localhost", 3333, 65536);
	}

	/**
	 * This constructor creates a UdpSender that sends to the provided port on the given host
	 *
	 * @param	host	the host to send to
	 * @param	port	the port to send to
	 */
	public UdpSender(String host, int port) {
		this(host, port, 65536);
	}

	/**
	 * This constructor creates a UdpSender that sends to the provided port on the given host,
	 * using the provided buffer size
	 *
	 * @param	host			the host to send to
	 * @param	port			the port to send to
	 * @param	bufferSize		the send buffer size
	 */
	public UdpSender(String host, int port, int bufferSize) {
		this.bufferSize = bufferSize;
		try {
			address = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			System.err.println("could not resolve host " + host);
			address = null;
		}
		this.port = port;
		try {
			socket = new DatagramSocket();
			connected = true;
		} catch (SocketException e) {
			System.err.println("could not create UDP socket: " + e.getMessage());
			socket = null;
		}
	}

	@Override
	public boolean sendOscPacket(OscPacketBuilder packet) {
		return sendPacket(packet.toByteArray());
	}

	@Override
	public boolean sendPacket(byte[] packet) {
		if (socket == null || address == null || packet == null) return false;
		if (packet.length > bufferSize) {
			System.err.println("UDP packet size " + packet.length + " exceeds buffer size " + bufferSize);
			return false;
		}
		try {
			DatagramPacket datagram = new DatagramPacket(packet, packet.length, address, port);
			socket.send(datagram);
			return true;
		} catch (IOException e) {
			System.err.println("error sending TUIO/UDP message: " + e.getMessage());
			return false;
		}
	}

	@Override
	public boolean isConnected() {
		return connected && socket != null;
	}

	@Override
	public void close() {
		connected = false;
		if (socket != null) {
			socket.close();
			socket = null;
		}
	}

	@Override
	public String getTransportType() {
		return "UDP";
	}

	@Override
	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public boolean isLocal() {
		return address == null || address.isLoopbackAddress();
	}
}
