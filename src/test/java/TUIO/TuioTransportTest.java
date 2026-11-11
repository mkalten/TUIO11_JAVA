/*
 * TUIO Java library tests
 * Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 */

package TUIO;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import TUIO.osc.OscPacketBuilder;

public class TuioTransportTest {

	private static byte[] buildFrame(int fseq, int sessionId, float x, float y) {
		OscPacketBuilder builder = new OscPacketBuilder(1024);
		builder.beginBundle();
		builder.beginMessage("/tuio/2Dcur").push("alive").push(sessionId).endMessage();
		builder.beginMessage("/tuio/2Dcur").push("set")
				.push(sessionId).push(x).push(y).push(0f).push(0f).push(0f).endMessage();
		builder.beginMessage("/tuio/2Dcur").push("fseq").push(fseq).endMessage();
		builder.endBundle();
		return builder.toByteArray();
	}

	private static byte[] buildFrame(String source, int fseq, int sessionId, float x, float y) {
		OscPacketBuilder builder = new OscPacketBuilder(1024);
		builder.beginBundle();
		builder.beginMessage("/tuio/2Dcur").push("source").push(source).endMessage();
		builder.beginMessage("/tuio/2Dcur").push("alive").push(sessionId).endMessage();
		builder.beginMessage("/tuio/2Dcur").push("set")
				.push(sessionId).push(x).push(y).push(0f).push(0f).push(0f).endMessage();
		builder.beginMessage("/tuio/2Dcur").push("fseq").push(fseq).endMessage();
		builder.endBundle();
		return builder.toByteArray();
	}

	private static byte[] tcpFrame(byte[] payload) {
		byte[] out = new byte[payload.length + 4];
		out[0] = (byte) (payload.length >>> 24);
		out[1] = (byte) (payload.length >>> 16);
		out[2] = (byte) (payload.length >>> 8);
		out[3] = (byte) payload.length;
		System.arraycopy(payload, 0, out, 4, payload.length);
		return out;
	}

	private static int freePort() throws Exception {
		java.net.ServerSocket probe = new java.net.ServerSocket(0);
		int port = probe.getLocalPort();
		probe.close();
		return port;
	}

	private static int serverPort(TcpReceiver receiver) {
		try {
			java.lang.reflect.Field f = TcpReceiver.class.getDeclaredField("serverSocket");
			f.setAccessible(true);
			return ((java.net.ServerSocket) f.get(receiver)).getLocalPort();
		} catch (Exception e) { fail(e); return -1; }
	}

	private static boolean waitFor(java.util.concurrent.Callable<Boolean> cond, long timeoutMs)
			throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (cond.call()) return true;
			Thread.sleep(1);
		}
		return cond.call();
	}

	private static class TestListener implements TuioListener {
		final List<Long> added = new CopyOnWriteArrayList<Long>();
		final List<Long> removed = new CopyOnWriteArrayList<Long>();
		final CountDownLatch refreshLatch = new CountDownLatch(1);
		final java.util.concurrent.atomic.AtomicInteger refreshCount = new java.util.concurrent.atomic.AtomicInteger();

		public void addTuioObject(TuioObject o) {}
		public void updateTuioObject(TuioObject o) {}
		public void removeTuioObject(TuioObject o) {}
		public void addTuioCursor(TuioCursor c) { added.add(c.getSessionID()); }
		public void updateTuioCursor(TuioCursor c) {}
		public void removeTuioCursor(TuioCursor c) { removed.add(c.getSessionID()); }
		public void addTuioBlob(TuioBlob b) {}
		public void updateTuioBlob(TuioBlob b) {}
		public void removeTuioBlob(TuioBlob b) {}
		public void refresh(TuioTime t) { refreshLatch.countDown(); refreshCount.incrementAndGet(); }
	}

	@Test
	public void udpReceiverDeliversPacket() throws Exception {
		UdpReceiver receiver = new UdpReceiver(0);
		assertNotNull(receiver.getSocket());
		int port = receiver.getSocket().getLocalPort();

		TestListener listener = new TestListener();
		TuioClient client = new TuioClient(receiver);
		client.addTuioListener(listener);
		client.connect();

		byte[] packet = buildFrame(1, 1, 0.5f, 0.5f);
		java.net.DatagramSocket sender = new java.net.DatagramSocket();
		sender.send(new java.net.DatagramPacket(packet, packet.length,
				java.net.InetAddress.getLoopbackAddress(), port));
		sender.close();

		assertTrue(listener.refreshLatch.await(3, TimeUnit.SECONDS));
		client.disconnect();
		assertEquals(1, listener.added.size());
	}

	@Test
	public void tcpSenderAndReceiver() throws Exception {
		TcpReceiver server = new TcpReceiver(0);
		server.connect(false);
		int port = 0;
		try {
			java.lang.reflect.Field f = TcpReceiver.class.getDeclaredField("serverSocket");
			f.setAccessible(true);
			java.net.ServerSocket ss = (java.net.ServerSocket) f.get(server);
			port = ss.getLocalPort();
		} catch (Exception e) { fail(e); }

		TestListener listener = new TestListener();
		TuioClient client = new TuioClient(server);
		client.addTuioListener(listener);

		TcpSender sender = new TcpSender("127.0.0.1", port);
		assertTrue(sender.isConnected());

		byte[] packet = buildFrame(1, 5, 0.1f, 0.2f);
		sender.sendPacket(packet); // sendPacket already frames
		assertTrue(listener.refreshLatch.await(3, TimeUnit.SECONDS));
		sender.close();
		client.disconnect();
	}

	@Test
	public void tcpPartialFrameFraming() throws Exception {
		// unit-test the frame parsing: send a frame split across writes
		TcpReceiver server = new TcpReceiver(0);
		server.connect(false);
		int port;
		try {
			java.lang.reflect.Field f = TcpReceiver.class.getDeclaredField("serverSocket");
			f.setAccessible(true);
			port = ((java.net.ServerSocket) f.get(server)).getLocalPort();
		} catch (Exception e) { fail(e); return; }

		TestListener listener = new TestListener();
		TuioClient client = new TuioClient(server);
		client.addTuioListener(listener);

		java.net.Socket sock = new java.net.Socket("127.0.0.1", port);
		java.io.OutputStream out = sock.getOutputStream();
		byte[] packet = buildFrame(1, 7, 0.3f, 0.4f);
		byte[] framed = tcpFrame(packet);
		out.write(framed, 0, 10);
		out.flush();
		Thread.sleep(50);
		out.write(framed, 10, framed.length - 10);
		out.flush();

		assertTrue(listener.refreshLatch.await(3, TimeUnit.SECONDS));
		sock.close();
		server.disconnect();
		assertEquals(1, listener.added.size());
	}

	@Test
	public void multiSourceSessionIds() throws Exception {
		UdpReceiver receiver = new UdpReceiver(0);
		TestListener listener = new TestListener();
		TuioClient client = new TuioClient(receiver);
		client.addTuioListener(listener);
		client.connect();

		byte[] p1 = buildFrame("src1@host1", 1, 1, 0.5f, 0.5f);
		byte[] p2 = buildFrame("src2@host2", 2, 1, 0.6f, 0.6f);

		java.net.DatagramSocket sender = new java.net.DatagramSocket();
		int port = receiver.getSocket().getLocalPort();
		sender.send(new java.net.DatagramPacket(p1, p1.length,
				java.net.InetAddress.getLoopbackAddress(), port));
		sender.send(new java.net.DatagramPacket(p2, p2.length,
				java.net.InetAddress.getLoopbackAddress(), port));
		sender.close();

		Thread.sleep(500);
		List<TuioCursor> src1 = client.getTuioCursors(0);
		List<TuioCursor> src2 = client.getTuioCursors(1);
		client.disconnect();
		assertEquals(1, src1.size());
		assertEquals(1, src2.size());
		assertEquals("src1", src1.get(0).getTuioSourceName());
		assertEquals("src2", src2.get(0).getTuioSourceName());
	}

	@Test
	public void udpReceiverBusyPort() throws Exception {
		// first receiver binds the port; second must fail gracefully
		UdpReceiver first = new UdpReceiver(0);
		assertNotNull(first.getSocket());
		int port = first.getSocket().getLocalPort();

		UdpReceiver second = new UdpReceiver(port);
		assertNull(second.getSocket()); // bind failure -> null socket path
		second.connect();               // must not spawn a thread or crash
		assertFalse(second.isConnected());

		first.disconnect();
	}

	@Test
	public void udpReceiverConnectCycle() {
		UdpReceiver recv = new UdpReceiver(0);
		recv.connect();
		assertTrue(recv.isConnected());
		// disconnect must return promptly (socket close + join)
		recv.disconnect();
		assertFalse(recv.isConnected());
		// idempotent disconnect is safe
		recv.disconnect();
	}

	@Test
	public void tcpSenderListenMode() throws Exception {
		int port = freePort();
		TcpSender listener = new TcpSender(port);
		TcpReceiver client = new TcpReceiver("127.0.0.1", port);
		assertFalse(client.isConnected()); // not connected until connect()
		client.connect();
		assertTrue(waitFor(() -> listener.isConnected(), 2000));
		client.disconnect();
		listener.close();
	}

	@Test
	public void tcpSenderCtorFailure() throws Exception {
		int port = freePort();
		TcpSender first = new TcpSender(port);
		assertTrue(first.isConnected());
		// a second listener on the same port must fail without becoming connected
		TcpSender second = new TcpSender(port);
		assertFalse(second.isConnected());
		first.close();
		second.close();
	}

	@Test
	public void tcpSenderListenTeardown() throws Exception {
		// closing a listening sender without clients must unblock accept()
		TcpSender idle = new TcpSender(freePort());
		Thread t = new Thread(idle::close);
		t.start();
		t.join(2000);
		assertFalse(t.isAlive());

		// ... and with a connected client
		TcpSender sender = new TcpSender(freePort());
		int port = 0;
		try {
			java.lang.reflect.Field f = TcpSender.class.getDeclaredField("serverSocket");
			f.setAccessible(true);
			port = ((java.net.ServerSocket) f.get(sender)).getLocalPort();
		} catch (Exception e) { fail(e); }
		TcpReceiver receiver = new TcpReceiver("127.0.0.1", port);
		receiver.connect();
		assertTrue(waitFor(() -> sender.isConnected(), 2000));

		Thread t2 = new Thread(sender::close);
		t2.start();
		t2.join(2000);
		assertFalse(t2.isAlive());
		receiver.disconnect();
	}

	@Test
	public void tcpCoalescedFrames() throws Exception {
		// two length-prefixed frames in a single write must both be processed
		TcpReceiver server = new TcpReceiver(0);
		server.connect(false);
		int port = serverPort(server);

		TestListener listener = new TestListener();
		TuioClient client = new TuioClient(server);
		client.addTuioListener(listener);
		client.connect();

		java.net.Socket sock = new java.net.Socket("127.0.0.1", port);
		java.io.OutputStream out = sock.getOutputStream();
		byte[] f1 = tcpFrame(buildFrame(1, 8, 0.3f, 0.4f));
		byte[] f2 = tcpFrame(buildFrame(2, 9, 0.5f, 0.6f));
		byte[] both = new byte[f1.length + f2.length];
		System.arraycopy(f1, 0, both, 0, f1.length);
		System.arraycopy(f2, 0, both, f1.length, f2.length);
		out.write(both);
		out.flush();

		assertTrue(waitFor(() -> listener.refreshCount.get() == 2, 2000));
		sock.close();
		client.disconnect();
		assertEquals(2, listener.added.size());
	}
}
