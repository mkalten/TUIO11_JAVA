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

import org.junit.jupiter.api.Test;

public class TuioTimeTest {

	@Test
	public void tuiotime_ctors() {
		TuioTime t0 = new TuioTime();
		assertEquals(0, t0.getSeconds());
		assertEquals(0, t0.getMicroseconds());
		assertEquals(0, t0.getTotalMilliseconds());

		// msec ctor takes total milliseconds
		TuioTime t1 = new TuioTime(1500);
		assertEquals(1, t1.getSeconds());
		assertEquals(500000, t1.getMicroseconds());
		assertEquals(1500, t1.getTotalMilliseconds());

		TuioTime t2 = new TuioTime(3, 250000);
		assertEquals(3, t2.getSeconds());
		assertEquals(250000, t2.getMicroseconds());
		assertEquals(3250, t2.getTotalMilliseconds());
	}

	@Test
	public void tuiotime_arithmetic() {
		TuioTime t = new TuioTime(2, 500000);

		// add(long us): micro-second carry is computed per component
		TuioTime sum = t.add(400000);
		assertEquals(2, sum.getSeconds());
		assertEquals(2900, sum.getTotalMilliseconds());

		// add(TuioTime) normalizes the sum
		TuioTime sum2 = t.add(new TuioTime(1, 700000));
		assertEquals(4, sum2.getSeconds());
		assertEquals(200000, sum2.getMicroseconds());

		// subtract borrows correctly
		TuioTime diff = t.subtract(600000);
		assertEquals(1, diff.getSeconds());
		assertEquals(900000, diff.getMicroseconds());

		TuioTime diff2 = t.subtract(new TuioTime(1, 700000));
		assertEquals(0, diff2.getSeconds());
		assertEquals(800000, diff2.getMicroseconds());

		// operations return new objects, the original is unchanged
		assertEquals(500000, t.getMicroseconds());
	}

	@Test
	public void tuiotime_compare_reset() {
		TuioTime a = new TuioTime(1, 500000), b = new TuioTime(1, 500000), c = new TuioTime(2, 0);
		assertEquals(a, b);
		assertNotEquals(a, c);

		b = new TuioTime(c);
		assertEquals(b, c);
		b.reset();
		assertEquals(0, b.getSeconds());
		assertEquals(0, b.getMicroseconds());
	}

	@Test
	public void tuiotime_session() {
		TuioTime.initSession();
		TuioTime start = TuioTime.getStartTime();
		TuioTime session = TuioTime.getSessionTime();
		assertTrue(session.getTotalMilliseconds() >= 0);

		// session time is monotonic
		TuioTime later = TuioTime.getSessionTime();
		assertTrue(later.getTotalMilliseconds() >= session.getTotalMilliseconds());

		TuioTime sys = TuioTime.getSystemTime();
		assertTrue(sys.getTotalMilliseconds() >= start.getTotalMilliseconds());
		assertTrue(sys.getTotalMilliseconds() > 0);
	}
}
