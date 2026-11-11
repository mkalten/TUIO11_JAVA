/*
 * TUIO OSC Codec
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

package TUIO.osc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Date;

/**
 * OSC 1.0 packet encoder with fixed-capacity buffer semantics
 * equivalent to oscpack's OutboundPacketStream: {@link #clear()}
 * resets the stream, {@link #size()} reports the bytes written and
 * {@link #capacity()} the buffer capacity. Writes that would exceed
 * the capacity set the overflow flag so callers can split bundles
 * using {@code capacity() - size()} estimates.
 *
 * <p>Bundle elements are emitted with their mandatory 4-byte
 * big-endian size prefix, nested bundles are supported.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * OscPacketBuilder packet = new OscPacketBuilder(4096);
 * packet.beginBundle();
 * packet.beginMessage("/tuio/2Dcur");
 * packet.push("set");
 * packet.push(1);
 * packet.push(0.5f);
 * packet.endMessage();
 * packet.endBundle();
 * byte[] data = packet.toByteArray();
 * </pre>
 */
public class OscPacketBuilder {

	private final int capacity;
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();
	private final Deque<ByteArrayOutputStream> bundleStack = new ArrayDeque<ByteArrayOutputStream>();
	private boolean overflow = false;

	// current message state
	private ByteArrayOutputStream messageData = null;
	private StringBuilder messageTags = null;
	private ByteArrayOutputStream messageArgs = null;

	/** Creates a builder with the provided buffer capacity in bytes. */
	public OscPacketBuilder(int capacity) {
		this.capacity = capacity;
	}

	/** Resets the stream for reuse. */
	public void clear() {
		out.reset();
		bundleStack.clear();
		messageData = null;
		messageTags = null;
		messageArgs = null;
		overflow = false;
	}

	/**
	 * Returns the size of the encoded packet in bytes, including
	 * an estimate for all open messages and bundles.
	 */
	public int size() {
		int size = out.size();
		for (ByteArrayOutputStream frame : bundleStack) size += frame.size() + 4;
		if (messageData != null) {
			size += messageData.size() + messageArgs.size() + 8;
		}
		return size;
	}

	/** Returns the buffer capacity in bytes. */
	public int capacity() {
		return capacity;
	}

	/** Returns true if a write exceeded the buffer capacity. */
	public boolean isOverflow() {
		return overflow;
	}

	/** Begins an immediate bundle (timetag 1). */
	public OscPacketBuilder beginBundle() {
		return beginBundle(OscTimeTag.IMMEDIATE);
	}

	/** Begins a bundle with the provided NTP timetag. */
	public OscPacketBuilder beginBundle(long timetag) {
		endMessage(); // close a dangling message defensively
		ByteArrayOutputStream frame = new ByteArrayOutputStream();
		writeString(frame, "#bundle");
		writeLong(frame, timetag);
		bundleStack.push(frame);
		return this;
	}

	/** Begins a bundle with the provided timestamp. */
	public OscPacketBuilder beginBundle(Date timestamp) {
		return beginBundle(OscTimeTag.fromDate(timestamp));
	}

	/** Ends the current bundle, writing it as a size-prefixed element. */
	public OscPacketBuilder endBundle() {
		endMessage();
		ByteArrayOutputStream frame = bundleStack.poll();
		if (frame == null) return this;
		writeElement(frame.toByteArray());
		return this;
	}

	/** Begins a message with the provided address pattern. */
	public OscPacketBuilder beginMessage(String address) {
		endMessage(); // close a dangling message defensively
		messageData = new ByteArrayOutputStream();
		messageTags = new StringBuilder();
		messageArgs = new ByteArrayOutputStream();
		writeString(messageData, address);
		return this;
	}

	/** Ends the current message, writing the accumulated type tags. */
	public OscPacketBuilder endMessage() {
		if (messageData == null) return this;
		writeString(messageData, "," + messageTags);
		messageData.write(messageArgs.toByteArray(), 0, messageArgs.size());
		byte[] data = messageData.toByteArray();
		messageData = null;
		messageTags = null;
		messageArgs = null;
		writeElement(data);
		return this;
	}

	private void writeElement(byte[] data) {
		if (data == null) return;
		if (bundleStack.isEmpty()) {
			write(out, data);
		} else {
			ByteArrayOutputStream frame = bundleStack.peek();
			writeInt(frame, data.length);
			frame.write(data, 0, data.length);
		}
	}

	public OscPacketBuilder push(int value) {
		return pushArgument('i', encodeInt(value));
	}

	public OscPacketBuilder push(float value) {
		return pushArgument('f', encodeInt(Float.floatToIntBits(value)));
	}

	public OscPacketBuilder push(String value) {
		return pushArgument('s', encodeString(value));
	}

	public OscPacketBuilder push(byte[] value) {
		return pushArgument('b', encodeBlob(value));
	}

	public OscPacketBuilder push(long value) {
		return pushArgument('h', encodeLong(value));
	}

	public OscPacketBuilder push(double value) {
		return pushArgument('d', encodeLong(Double.doubleToLongBits(value)));
	}

	public OscPacketBuilder push(boolean value) {
		messageTags.append(value ? 'T' : 'F');
		return this;
	}

	/** Pushes a generic argument, dispatching on its runtime type. */
	public OscPacketBuilder push(Object value) {
		if (value == null) {
			messageTags.append('N');
		} else if (value instanceof Integer) {
			push(((Integer) value).intValue());
		} else if (value instanceof Float) {
			push(((Float) value).floatValue());
		} else if (value instanceof Double) {
			push(((Double) value).doubleValue());
		} else if (value instanceof Long) {
			push(((Long) value).longValue());
		} else if (value instanceof String) {
			push((String) value);
		} else if (value instanceof byte[]) {
			push((byte[]) value);
		} else if (value instanceof Boolean) {
			push(((Boolean) value).booleanValue());
		} else {
			push(value.toString());
		}
		return this;
	}

	private OscPacketBuilder pushArgument(char tag, byte[] encoded) {
		messageTags.append(tag);
		messageArgs.write(encoded, 0, encoded.length);
		return this;
	}

	/** Returns the encoded packet bytes. */
	public byte[] toByteArray() {
		// close any dangling structures defensively
		endMessage();
		while (!bundleStack.isEmpty()) endBundle();
		if (out.size() > capacity) overflow = true;
		return out.toByteArray();
	}

	// ---- argument encoders (always 4-byte aligned length) ----

	private static byte[] encodeInt(int value) {
		return new byte[] {
			(byte) (value >>> 24), (byte) (value >>> 16),
			(byte) (value >>> 8), (byte) value };
	}

	private static byte[] encodeLong(long value) {
		byte[] out = new byte[8];
		for (int i = 7; i >= 0; i--) {
			out[i] = (byte) value;
			value >>>= 8;
		}
		return out;
	}

	private static byte[] encodeString(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		byte[] result = new byte[align4(bytes.length + 1)];
		System.arraycopy(bytes, 0, result, 0, bytes.length);
		return result;
	}

	private static byte[] encodeBlob(byte[] value) {
		byte[] result = new byte[4 + align4(value.length)];
		int size = value.length;
		result[0] = (byte) (size >>> 24);
		result[1] = (byte) (size >>> 16);
		result[2] = (byte) (size >>> 8);
		result[3] = (byte) size;
		System.arraycopy(value, 0, result, 4, size);
		return result;
	}

	// ---- stream writers ----

	private void write(ByteArrayOutputStream target, byte[] data) {
		if (target.size() + data.length > capacity) {
			overflow = true;
		}
		target.write(data, 0, data.length);
	}

	private static void writeInt(ByteArrayOutputStream target, int value) {
		target.write((value >>> 24) & 0xFF);
		target.write((value >>> 16) & 0xFF);
		target.write((value >>> 8) & 0xFF);
		target.write(value & 0xFF);
	}

	private static void writeLong(ByteArrayOutputStream target, long value) {
		for (int i = 7; i >= 0; i--) target.write((int) ((value >>> (8 * i)) & 0xFF));
	}

	private void writeString(ByteArrayOutputStream target, String value) {
		write(target, encodeString(value));
	}

	private static int align4(int value) {
		return (value + 3) & ~3;
	}
}
