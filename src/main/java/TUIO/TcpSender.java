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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import TUIO.osc.OscPacketBuilder;

/**
 * The TcpSender implements the TCP transport method for OSC.
 * It supports both client mode (connect to host/port) and server mode
 * (listen on port and broadcast to all connected clients).
 * OSC messages are framed with a 4-byte big-endian length prefix.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public class TcpSender implements OscSender {

	private static final int MAX_TCP_SIZE = 65536;

	private final boolean serverMode;
	private Socket socket;
	private ServerSocket serverSocket;
	private OutputStream out;
	private final List<Socket> clients = new CopyOnWriteArrayList<Socket>();
	private final Object writeLock = new Object();
	private volatile boolean stop = false;
	private volatile boolean connected = false;

	/**
	 * The default constructor creates a TcpSender that sends to the
	 * default TUIO port 3333 on localhost
	 */
	public TcpSender() {
		this("localhost", 3333);
	}

	/**
	 * This constructor creates a TcpSender that sends to the provided port on the given host
	 *
	 * @param	host	the host to connect to
	 * @param	port	the port to connect to
	 */
	public TcpSender(String host, int port) {
		this.serverMode = false;
		connectTo(host, port);
	}

	/**
	 * This constructor creates a TcpSender that listens to the provided port
	 *
	 * @param	port	the port to listen on
	 */
	public TcpSender(int port) {
		this.serverMode = true;
		try {
			serverSocket = new ServerSocket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(port));
			connected = true;
			Thread acceptThread = new Thread(new Runnable() {
				public void run() { acceptLoop(); }
			});
			acceptThread.setDaemon(true);
			acceptThread.start();
		} catch (IOException e) {
			System.err.println("could not bind TCP sender to port " + port + ": " + e.getMessage());
		}
	}

	private boolean connectTo(String host, int port) {
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(host, port), 5000);
			out = socket.getOutputStream();
			connected = true;
			return true;
		} catch (IOException e) {
			System.err.println("could not connect to " + host + ":" + port + " - " + e.getMessage());
			return false;
		}
	}

	private void acceptLoop() {
		while (!stop && serverSocket != null) {
			try {
				Socket client = serverSocket.accept();
				clients.add(client);
			} catch (SocketException e) {
				break;
			} catch (IOException e) {
				break;
			}
		}
	}

	@Override
	public boolean sendOscPacket(OscPacketBuilder packet) {
		return sendPacket(packet.toByteArray());
	}

	@Override
	public boolean sendPacket(byte[] packet) {
		if (packet == null || packet.length <= 0 || packet.length > MAX_TCP_SIZE) return false;

		byte[] frame = new byte[packet.length + 4];
		frame[0] = (byte) (packet.length >>> 24);
		frame[1] = (byte) (packet.length >>> 16);
		frame[2] = (byte) (packet.length >>> 8);
		frame[3] = (byte) packet.length;
		System.arraycopy(packet, 0, frame, 4, packet.length);

		synchronized (writeLock) {
			if (serverMode) {
				boolean ok = true;
				for (Socket client : clients) {
					ok &= deliver(client, frame);
				}
				return ok;
			} else {
				return deliver(socket, frame);
			}
		}
	}

	private boolean deliver(Socket target, byte[] frame) {
		if (target == null || target.isClosed()) return false;
		try {
			OutputStream stream = (target == socket) ? out : target.getOutputStream();
			if (stream == null) return false;
			int offset = 0;
			while (offset < frame.length) {
				int remaining = frame.length - offset;
				stream.write(frame, offset, remaining);
				offset += remaining;
			}
			stream.flush();
			return true;
		} catch (IOException e) {
			System.err.println("error sending TUIO/TCP message: " + e.getMessage());
			try { target.close(); } catch (Exception ignored) {}
			clients.remove(target);
			return false;
		}
	}

	@Override
	public boolean isConnected() {
		if (serverMode) return connected && serverSocket != null && !serverSocket.isClosed();
		return connected && socket != null && !socket.isClosed();
	}

	@Override
	public void close() {
		connected = false;
		stop = true;
		if (serverSocket != null) {
			try { serverSocket.close(); } catch (Exception e) {}
			serverSocket = null;
		}
		for (Socket client : clients) {
			try { client.close(); } catch (Exception e) {}
		}
		clients.clear();
		if (socket != null) {
			try { socket.close(); } catch (Exception e) {}
			socket = null;
			out = null;
		}
	}

	@Override
	public String getTransportType() {
		return "TCP";
	}

	@Override
	public int getBufferSize() {
		return MAX_TCP_SIZE;
	}

	@Override
	public boolean isLocal() {
		return serverMode || (socket != null && socket.getInetAddress() != null
				&& socket.getInetAddress().isLoopbackAddress());
	}
}
