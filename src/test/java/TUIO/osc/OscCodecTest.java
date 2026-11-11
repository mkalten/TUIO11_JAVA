/*
 * TUIO OSC Codec tests
 * Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 */

package TUIO.osc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

public class OscCodecTest {

	@Test
	public void messageRoundTrip() throws Exception {
		OscPacketBuilder builder = new OscPacketBuilder(1024);
		builder.beginMessage("/tuio/2Dcur");
		builder.push("set").push(1).push(0.5f).push(0.25f);
		builder.endMessage();

		Object parsed = OscPacket.parse(builder.toByteArray());
		assertTrue(parsed instanceof OscMessage);
		OscMessage msg = (OscMessage) parsed;
		assertEquals("/tuio/2Dcur", msg.getAddress());
		List<Object> args = msg.getArguments();
		assertEquals(4, args.size());
		assertEquals("set", args.get(0));
		assertEquals(1, args.get(1));
		assertEquals(0.5f, (Float) args.get(2), 1e-6);
		assertEquals(0.25f, (Float) args.get(3), 1e-6);
	}

	@Test
	public void bundleRoundTrip() throws Exception {
		OscPacketBuilder builder = new OscPacketBuilder(4096);
		builder.beginBundle();
		builder.beginMessage("/tuio/2Dobj").push("alive").push(1).push(2).endMessage();
		builder.beginMessage("/tuio/2Dobj").push("fseq").push(42).endMessage();
		builder.endBundle();

		Object parsed = OscPacket.parse(builder.toByteArray());
		assertTrue(parsed instanceof OscBundle);
		OscBundle bundle = (OscBundle) parsed;
		assertEquals(2, bundle.size());
		assertTrue(bundle.getElements().get(0) instanceof OscMessage);
		OscMessage fseq = (OscMessage) bundle.getElements().get(1);
		assertEquals("fseq", fseq.getArguments().get(0));
		assertEquals(42, fseq.getArguments().get(1));
	}

	@Test
	public void nestedBundle() throws Exception {
		OscPacketBuilder builder = new OscPacketBuilder(4096);
		builder.beginBundle();
		builder.beginBundle();
		builder.beginMessage("/test").push("x").endMessage();
		builder.endBundle();
		builder.endBundle();

		Object parsed = OscPacket.parse(builder.toByteArray());
		OscBundle outer = (OscBundle) parsed;
		OscBundle inner = (OscBundle) outer.getElements().get(0);
		assertEquals(1, inner.size());
		assertEquals("/test", ((OscMessage) inner.getElements().get(0)).getAddress());
	}

	@Test
	public void timetagConversion() {
		long tag = OscTimeTag.fromDate(new java.util.Date(0));
		assertEquals(OscTimeTag.SECONDS_FROM_1900_TO_1970, OscTimeTag.toDate(tag).getTime() == 0
				? OscTimeTag.SECONDS_FROM_1900_TO_1970 : -1);
		assertTrue(OscTimeTag.isImmediate(OscTimeTag.IMMEDIATE));
	}

	@Test
	public void malformedPacketThrows() {
		assertThrows(MalformedBundleException.class,
				() -> OscPacket.parse(new byte[] { 1, 2, 3 }));
	}

	@Test
	public void truncatedBundleThrows() {
		byte[] bundle = ("#bundle\0").getBytes();
		assertThrows(MalformedBundleException.class,
				() -> OscPacket.parse(bundle));
	}

	@Test
	public void truncatedArgumentThrows() {
		assertThrows(MalformedBundleException.class, () -> {
			OscPacketBuilder builder = new OscPacketBuilder(1024);
			builder.beginMessage("/test").push(123456789).endMessage();
			byte[] data = builder.toByteArray();
			// truncate inside the argument section
			OscPacket.parse(data, 0, data.length - 3);
		});
	}

	@Test
	public void bufferOverflow() {
		OscPacketBuilder builder = new OscPacketBuilder(64);
		for (int i = 0; i < 20; i++)
			builder.beginMessage("/test").push("overflow me overflow me").endMessage();
		assertTrue(builder.isOverflow());
	}

	@Test
	public void parserIsThreadSafe() throws Exception {
		OscPacketBuilder builder = new OscPacketBuilder(1024);
		builder.beginMessage("/concurrent").push(7).endMessage();
		final byte[] data = builder.toByteArray();

		Runnable task = () -> {
			for (int i = 0; i < 1000; i++) {
				try {
					OscMessage m = (OscMessage) OscPacket.parse(data);
					assertEquals("/concurrent", m.getAddress());
				} catch (Exception e) {
					fail(e);
				}
			}
		};
		Thread t1 = new Thread(task);
		Thread t2 = new Thread(task);
		t1.start(); t2.start();
		t1.join(); t2.join();
	}
}
