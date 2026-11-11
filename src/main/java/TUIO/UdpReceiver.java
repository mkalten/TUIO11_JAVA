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
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * The UdpReceiver provides the OscReceiver functionality for the UDP transport method
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public class UdpReceiver extends OscReceiver {

	private DatagramSocket socket;
	private Thread thread;
	private volatile boolean stop = false;
	private boolean locked = false;

	/**
	 * This constructor creates a UdpReceiver instance listening to the provided UDP port
	 *
	 * @param	port	the number of the UDP port to listen to, defaults to 3333
	 */
	public UdpReceiver(int port) {
		this(port, InetAddress.getLoopbackAddress());
	}

	/**
	 * This constructor creates a UdpReceiver instance listening to the provided UDP port
	 * and local address
	 *
	 * @param	port	the number of the UDP port to listen to
	 * @param	addr	the local address to bind to
	 */
	public UdpReceiver(int port, InetAddress addr) {
		try {
			socket = new DatagramSocket(new InetSocketAddress(addr, port));
			System.out.println("listening to TUIO/UDP messages on port " + port);
		} catch (IOException e) {
			System.err.println("could not bind to UDP port " + port);
			socket = null;
		}
	}

	/**
	 * Returns the underlying DatagramSocket or null if the bind failed.
	 */
	public DatagramSocket getSocket() {
		return socket;
	}

	/**
	 * The UdpReceiver connects and starts receiving TUIO messages via UDP.
	 * With the default parameter the OSC receiver runs in a dedicated background thread,
	 * when set to true the method blocks in the current thread until disconnect() is invoked.
	 *
	 * @param	lock	blocks in the current thread if set to true, runs in the background if set to false (default)
	 */
	@Override
	public void connect(boolean lock) {
		if (connected || socket == null) return;
		locked = lock;
		connected = true;

		if (!locked) {
			stop = false;
			thread = new Thread(new Runnable() {
				public void run() {
					runReceiveLoop();
				}
			});
			thread.setDaemon(false);
			thread.start();
		} else {
			stop = false;
			runReceiveLoop();
			connected = false;
			locked = false;
		}
	}

	/**
	 * The UdpReceiver disconnects and stops receiving TUIO messages via UDP
	 */
	@Override
	public void disconnect() {
		if (!connected) return;
		stop = true;
		if (!locked) {
			Thread t = thread;
			if (t != null) {
				try { t.join(2000); } catch (InterruptedException e) {}
			}
			thread = null;
		}
		if (socket != null) {
			try { socket.close(); } catch (Exception e) {}
			socket = null;
		}
		locked = false;
		connected = false;
	}

	private void runReceiveLoop() {
		byte[] buffer = new byte[65536];
		try {
			if (socket != null) socket.setSoTimeout(200);
		} catch (SocketException e) {}
		while (!stop && socket != null) {
			try {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				processPacket(buffer, packet.getOffset(), packet.getLength());
			} catch (java.net.SocketTimeoutException e) {
				// poll the stop flag
			} catch (SocketException e) {
				break;
			} catch (IOException e) {
				break;
			} catch (RuntimeException e) {
				System.err.println("error receiving TUIO/UDP message: " + e.getMessage());
			}
		}
	}
}
