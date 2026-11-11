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

import java.util.Date;

/**
 * OSC 1.0 timetag handling. A timetag of 1 marks an "immediate" bundle;
 * otherwise the 64-bit value is an NTP timestamp (seconds since 1900
 * plus a 32-bit fractional part).
 */
public final class OscTimeTag {

	/** The timetag value marking an immediate bundle. */
	public static final long IMMEDIATE = 1L;

	/** Seconds between the NTP epoch (1900) and the Unix epoch (1970). */
	public static final long SECONDS_FROM_1900_TO_1970 = 2208988800L;

	private OscTimeTag() {}

	/** Returns true if the provided timetag marks an immediate bundle. */
	public static boolean isImmediate(long timetag) {
		return timetag == IMMEDIATE;
	}

	/** Converts an NTP timetag to a java.util.Date. */
	public static Date toDate(long timetag) {
		if (isImmediate(timetag)) return new Date();
		long seconds = (timetag >>> 32) - SECONDS_FROM_1900_TO_1970;
		long fraction = timetag & 0xFFFFFFFFL;
		long millis = (fraction * 1000L) >>> 32;
		return new Date(seconds * 1000L + millis);
	}

	/** Converts a java.util.Date to an NTP timetag. */
	public static long fromDate(Date date) {
		if (date == null) return IMMEDIATE;
		long millis = date.getTime();
		long seconds = millis / 1000L + SECONDS_FROM_1900_TO_1970;
		long fraction = ((millis % 1000L) << 32) / 1000L;
		return (seconds << 32) | fraction;
	}
}
