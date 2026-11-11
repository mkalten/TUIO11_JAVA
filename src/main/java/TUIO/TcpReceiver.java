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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The TcpReceiver provides the OscReceiver functionality for the TCP transport method.
 * It supports both listening mode (server) and connecting mode (client).
 * OSC messages are framed with a 4-byte big-endian length prefix.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public class TcpReceiver extends OscReceiver {

	/** Maximum size of a single OSC TCP frame in bytes. */
	public static final int MAX_TCP_SIZE = 65536;

	private final String host;
	private final int port;
	private ServerSocket serverSocket;
	private Socket clientSocket;
	private final List<Socket> clientSockets = new CopyOnWriteArrayList<Socket>();
	private final List<Thread> threads = new CopyOnWriteArrayList<Thread>();
	private volatile boolean stop = false;
	private boolean locked = false;

	/**
	 * This constructor creates a TcpReceiver instance listening to the provided TCP port
	 *
	 * @param	port	the number of the TCP port to listen to, defaults to 3333
	 */
	public TcpReceiver(int port) {
		this(null, port);
	}

	/**
	 * This constructor creates a TcpReceiver connected to the provided host and TCP port
	 *
	 * @param	host	the host to connect to
	 * @param	port	the port to connect to
	 */
	public TcpReceiver(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * The TcpReceiver connects and starts receiving TUIO messages via TCP.
	 * With the default parameter the OSC receiver runs in a dedicated background thread,
	 * when set to true the method blocks in the current thread until disconnect() is invoked.
	 *
	 * @param	lock	blocks in the current thread if set to true, runs in the background if set to false (default)
	 */
	@Override
	public void connect(boolean lock) {
		if (connected) return;
		locked = lock;
		stop = false;
		connected = true;

		if (host == null) {
			// listening (server) mode
			try {
				serverSocket = new ServerSocket();
				serverSocket.setReuseAddress(true);
				serverSocket.bind(new InetSocketAddress(port));
				System.out.println("listening to TUIO/TCP messages on port " + port);
			} catch (IOException e) {
				System.err.println("could not bind to TCP port " + port + ": " + e.getMessage());
				connected = false;
				return;
			}
			startThread(new Runnable() {
				public void run() { acceptLoop(); }
			});
		} else {
			// connecting (client) mode
			try {
				clientSocket = new Socket();
				clientSocket.connect(new InetSocketAddress(host, port), 5000);
				clientSockets.add(clientSocket);
			} catch (IOException e) {
				System.err.println("could not connect to " + host + ":" + port + " - " + e.getMessage());
				connected = false;
				return;
			}
			if (locked) {
				readLoop(clientSocket);
				connected = false;
				locked = false;
			} else {
				final Socket socket = clientSocket;
				startThread(new Runnable() {
					public void run() { readLoop(socket); }
				});
			}
		}
	}

	private void startThread(Runnable runnable) {
		Thread t = new Thread(runnable);
		t.setDaemon(false);
		t.start();
		threads.add(t);
	}

	/**
	 * The TcpReceiver disconnects and stops receiving TUIO messages via TCP
	 */
	@Override
	public void disconnect() {
		if (!connected) return;
		stop = true;

		if (serverSocket != null) {
			try { serverSocket.close(); } catch (Exception e) {}
			serverSocket = null;
		}
		for (Socket socket : clientSockets) {
			try { socket.close(); } catch (Exception e) {}
		}
		clientSockets.clear();

		for (Thread t : threads) {
			try { t.join(2000); } catch (InterruptedException e) {}
		}
		threads.clear();
		clientSocket = null;
		locked = false;
		connected = false;
	}

	private void acceptLoop() {
		while (!stop && serverSocket != null) {
			try {
				Socket socket = serverSocket.accept();
				clientSockets.add(socket);
				final Socket client = socket;
				Thread t = new Thread(new Runnable() {
					public void run() {
						readLoop(client);
						clientSockets.remove(client);
					}
				});
				t.setDaemon(false);
				t.start();
				threads.add(t);
			} catch (SocketException e) {
				break;
			} catch (IOException e) {
				break;
			}
		}
	}

	private void readLoop(Socket socket) {
		byte[] buffer = new byte[8192];
		byte[] frameBuffer = new byte[MAX_TCP_SIZE];
		int frameLength = 0;
		int headerBytes = 0;
		int expectedLength = -1;

		try {
			InputStream in = socket.getInputStream();
			while (!stop) {
				int count = in.read(buffer);
				if (count < 0) break; // client disconnected

				int offset = 0;
				while (offset < count) {
					if (expectedLength < 0) {
						// accumulate the 4-byte big-endian length header
						int headerNeed = 4 - headerBytes;
						int take = Math.min(headerNeed, count - offset);
						for (int i = 0; i < take; i++) {
							frameBuffer[headerBytes + i] = buffer[offset + i];
						}
						headerBytes += take;
						offset += take;
						if (headerBytes == 4) {
							expectedLength = ((frameBuffer[0] & 0xFF) << 24)
									| ((frameBuffer[1] & 0xFF) << 16)
									| ((frameBuffer[2] & 0xFF) << 8)
									| (frameBuffer[3] & 0xFF);
							if (expectedLength <= 0 || expectedLength > MAX_TCP_SIZE) {
								System.err.println("invalid OSC TCP frame size: " + expectedLength);
								return;
							}
							headerBytes = 0;
							frameLength = 0;
						}
					} else {
						// accumulate the frame payload
						int need = expectedLength - frameLength;
						int take = Math.min(need, count - offset);
						System.arraycopy(buffer, offset, frameBuffer, frameLength, take);
						frameLength += take;
						offset += take;
						if (frameLength == expectedLength) {
							processPacket(frameBuffer, 0, frameLength);
							expectedLength = -1;
							frameLength = 0;
						}
					}
				}
			}
		} catch (IOException e) {
			// socket closed or peer disconnected
		} finally {
			try { socket.close(); } catch (Exception e) {}
		}
	}
}
