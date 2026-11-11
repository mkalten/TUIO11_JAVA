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

import java.nio.charset.StandardCharsets;

/**
 * OSC 1.0 packet decoding. Parses a byte array into an {@link OscBundle}
 * or {@link OscMessage} tree. All parsing is bounds-checked; any
 * truncation or malformed content results in a
 * {@link MalformedBundleException}.
 */
public final class OscPacket {

	private static final byte[] BUNDLE_TAG = "#bundle".getBytes(StandardCharsets.US_ASCII);

	private OscPacket() {}

	/**
	 * Parses the provided OSC data. Returns an {@link OscBundle} if the
	 * data starts with the "#bundle" tag, otherwise an {@link OscMessage}.
	 *
	 * @param data the raw OSC packet bytes
	 * @return the decoded OscBundle or OscMessage
	 * @throws MalformedBundleException if the data is not well-formed OSC
	 */
	public static Object parse(byte[] data) throws MalformedBundleException {
		return new Parser(data).parseTopLevel();
	}

	/**
	 * Parses the provided OSC data starting at the given offset.
	 *
	 * @param data   the raw OSC packet bytes
	 * @param offset the offset of the first element
	 * @param length the number of bytes available from the offset
	 * @return the decoded OscBundle or OscMessage
	 * @throws MalformedBundleException if the data is not well-formed OSC
	 */
	public static Object parse(byte[] data, int offset, int length)
			throws MalformedBundleException {
		return new Parser(data, offset, offset + length).parseElement();
	}

	private static final class Parser {

		private final byte[] data;
		private final int start;
		private final int end;
		private int pos;
		private int stringEnd;

		Parser(byte[] data) {
			this(data, 0, data.length);
		}

		Parser(byte[] data, int start, int end) {
			this.data = data;
			this.start = start;
			this.end = end;
			this.pos = start;
		}

		Object parseTopLevel() throws MalformedBundleException {
			Object element = parseElement();
			// trailing padding bytes are tolerated
			return element;
		}

		Object parseElement() throws MalformedBundleException {
			if (pos + 8 > end) throw error("truncated OSC element");
			if (matches(BUNDLE_TAG)) {
				return parseBundle();
			}
			return parseMessage();
		}

		private OscBundle parseBundle() throws MalformedBundleException {
			// "#bundle\0" occupies 8 bytes, followed by an 8-byte timetag
			pos += 8;
			if (pos + 8 > end) throw error("truncated bundle timetag");
			long timetag = readLong();

			OscBundle bundle = new OscBundle();
			bundle.setTimetag(timetag);
			while (pos < end) {
				// each bundle element is prefixed with a 4-byte size
				if (pos + 4 > end) throw error("truncated bundle element size");
				int elementSize = readInt();
				if (elementSize < 4 || (elementSize % 4) != 0
						|| pos + elementSize > end)
					throw error("invalid bundle element size");
				int elementEnd = pos + elementSize;
				Object element = parseElement();
				if (pos > elementEnd)
					throw error("bundle element exceeds its declared size");
				pos = elementEnd;
				bundle.addElement(element);
			}
			return bundle;
		}

		private OscMessage parseMessage() throws MalformedBundleException {
			String address = readString();
			// the type tag string must start with ','
			if (pos >= end || data[pos] != ',') throw error("missing type tag string");
			String tags = readString();
			if (tags.length() < 1 || tags.charAt(0) != ',')
				throw error("invalid type tag string");

			OscMessage message = new OscMessage(address);
			for (int i = 1; i < tags.length(); i++) {
				readArgument(tags.charAt(i), message);
			}
			return message;
		}

		private void readArgument(char tag, OscMessage message)
				throws MalformedBundleException {
			switch (tag) {
				case 'i':
					message.addArgument(readInt());
					break;
				case 'f':
					message.addArgument(readFloat());
					break;
				case 's':
				case 'S':
					message.addArgument(readString());
					break;
				case 'b': {
					int size = readInt();
					if (size < 0 || pos + size > end)
						throw error("truncated blob data");
					byte[] blob = new byte[size];
					System.arraycopy(data, pos, blob, 0, size);
					message.addArgument(blob);
					pos = align4(pos + size);
					break;
				}
				case 'h':
					message.addArgument(readLong());
					break;
				case 'd':
					message.addArgument(readDouble());
					break;
				case 'c':
					message.addArgument(readInt());
					break;
				case 'T':
					message.addArgument(Boolean.TRUE);
					break;
				case 'F':
					message.addArgument(Boolean.FALSE);
					break;
				case 'N':
					message.addArgument(OscNil.NIL);
					break;
				case 'I':
					message.addArgument(Double.POSITIVE_INFINITY);
					break;
				default:
					throw error("unsupported OSC type tag '" + tag + "'");
			}
		}

		private String readString() throws MalformedBundleException {
			int stringStart = pos;
			while (pos < end && data[pos] != 0) pos++;
			if (pos >= end) throw error("unterminated OSC string");
			String value = new String(data, stringStart, pos - stringStart,
					StandardCharsets.UTF_8);
			pos++;
			pos = align4(pos);
			stringEnd = pos;
			return value;
		}

		private int readInt() throws MalformedBundleException {
			if (pos + 4 > end) throw error("truncated 32-bit argument");
			int value = (data[pos] << 24) | ((data[pos + 1] & 0xFF) << 16)
					| ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
			pos += 4;
			return value;
		}

		private float readFloat() throws MalformedBundleException {
			return Float.intBitsToFloat(readInt());
		}

		private long readLong() throws MalformedBundleException {
			if (pos + 8 > end) throw error("truncated 64-bit argument");
			long value = 0;
			for (int i = 0; i < 8; i++)
				value = (value << 8) | (data[pos + i] & 0xFFL);
			pos += 8;
			return value;
		}

		private double readDouble() throws MalformedBundleException {
			return Double.longBitsToDouble(readLong());
		}

		private boolean matches(byte[] tag) {
			if (pos + tag.length > end) return false;
			for (int i = 0; i < tag.length; i++)
				if (data[pos + i] != tag[i]) return false;
			return true;
		}

		private int align4(int value) {
			return (value + 3) & ~3;
		}
	}

	private static MalformedBundleException error(String message) {
		return new MalformedBundleException(message);
	}
}
