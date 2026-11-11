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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import TUIO.osc.OscBundle;
import TUIO.osc.OscMessage;
import TUIO.osc.OscPacket;
import TUIO.osc.OscPacketBuilder;

public class TuioServerTest {

	/** OscSender that records all sent packets. */
	private static class RecordingSender implements OscSender {
		final List<byte[]> packets = new ArrayList<byte[]>();
		boolean connected = true;

		public boolean sendOscPacket(OscPacketBuilder packet) {
			packets.add(packet.toByteArray());
			return true;
		}

		public boolean sendPacket(byte[] packet) {
			byte[] copy = new byte[packet.length];
			System.arraycopy(packet, 0, copy, 0, packet.length);
			packets.add(copy);
			return true;
		}

		public boolean isConnected() { return connected; }
		public int getBufferSize() { return 65536; }
		public boolean isLocal() { return true; }
		public void close() { connected = false; }
		public String getTransportType() { return "record"; }
	}

	private static List<OscMessage> flatten(List<byte[]> packets) throws Exception {
		List<OscMessage> out = new ArrayList<OscMessage>();
		for (byte[] p : packets) {
			Object parsed = OscPacket.parse(p);
			if (parsed instanceof OscBundle) {
				for (Object el : ((OscBundle) parsed).getElements())
					if (el instanceof OscMessage) out.add((OscMessage) el);
			}
		}
		return out;
	}

	private static int countCmd(List<OscMessage> msgs, String cmd) {
		int n = 0;
		for (OscMessage m : msgs)
			if (cmd.equals(m.getArguments().get(0))) n++;
		return n;
	}

	@Test
	public void emptyBundlesOnInit() throws Exception {
		RecordingSender sender = new RecordingSender();
		new TuioServer(sender);
		List<OscMessage> msgs = flatten(sender.packets);
		// constructor sends empty cursor, object and blob bundles
		assertEquals(3, countCmd(msgs, "fseq"));
	}

	@Test
	public void cursorFrameBundle() throws Exception {
		RecordingSender sender = new RecordingSender();
		TuioServer server = new TuioServer(sender);
		server.enableCursorProfile(true);

		TuioTime.initSession();
		server.initFrame(TuioTime.getSessionTime());
		server.addTuioCursor(0.5f, 0.5f);
		server.commitFrame();

		List<OscMessage> msgs = flatten(sender.packets);
		assertEquals(1, countCmd(msgs, "set"));
		assertTrue(countCmd(msgs, "alive") > 0);
	}

	@Test
	public void cursorIdRecycling() {
		RecordingSender sender = new RecordingSender();
		TuioServer server = new TuioServer(sender);
		TuioTime.initSession();
		server.initFrame(TuioTime.getSessionTime());

		TuioCursor c1 = server.addTuioCursor(0.1f, 0.1f);
		TuioCursor c2 = server.addTuioCursor(0.2f, 0.2f);
		assertEquals(0, c1.getCursorID());
		assertEquals(1, c2.getCursorID());

		server.removeTuioCursor(c1);
		TuioCursor c3 = server.addTuioCursor(0.3f, 0.3f);
		// freed id 0 should be recycled
		assertEquals(0, c3.getCursorID());
	}

	@Test
	public void fseqSequence() throws Exception {
		RecordingSender sender = new RecordingSender();
		TuioServer server = new TuioServer(sender);
		server.enableCursorProfile(true);

		TuioTime.initSession();
		server.initFrame(TuioTime.getSessionTime());
		server.addTuioCursor(0.5f, 0.5f);
		server.commitFrame();
		server.initFrame(TuioTime.getSessionTime());
		server.updateTuioCursor(server.getTuioCursors().get(0), 0.6f, 0.6f);
		server.commitFrame();

		List<OscMessage> msgs = flatten(sender.packets);
		int lastFseq = Integer.MIN_VALUE;
		for (OscMessage m : msgs) {
			if ("fseq".equals(m.getArguments().get(0)) && "/tuio/2Dcur".equals(m.getAddress()))
				lastFseq = Math.max(lastFseq, ((Number) m.getArguments().get(1)).intValue());
		}
		assertEquals(1, lastFseq);
	}

	@Test
	public void entityStateTransitions() {
		TuioTime.initSession();
		TuioTime t1 = TuioTime.getSessionTime();
		TuioCursor cur = new TuioCursor(t1, 1, 0, 0.5f, 0.5f);
		assertEquals(TuioContainer.TUIO_ADDED, cur.getTuioState());
		cur.update(TuioTime.getSessionTime(), 0.6f, 0.6f);
		int state = cur.getTuioState();
		assertTrue(state == TuioContainer.TUIO_ACCELERATING
				|| state == TuioContainer.TUIO_DECELERATING
				|| state == TuioContainer.TUIO_STOPPED);
	}

	@Test
	public void predictPosition() {
		TuioTime.initSession();
		TuioCursor cur = new TuioCursor(TuioTime.getSessionTime(), 1, 0, 0.5f, 0.5f);
		TuioPoint p = cur.predictPosition();
		assertNotNull(p);
	}
}
