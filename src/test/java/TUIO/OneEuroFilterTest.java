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

public class OneEuroFilterTest {

	@Test
	public void oneeuro_ctor_validation() {
		assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(0.0));
		assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(60.0, 0.0, 1.0));
		assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(60.0, 1.0, 0.0));

		// valid parameters do not throw
		OneEuroFilter ok = new OneEuroFilter(60.0, 1.0, 0.5, 1.0);
		assertTrue(Double.isFinite(ok.filter(1.0, 0.016)));
	}

	@Test
	public void oneeuro_converges() {
		OneEuroFilter f = new OneEuroFilter(60.0, 1.0, 0.5, 1.0);
		// repeated same-dt updates converge toward the step input
		double r = 0.0;
		for (int i = 0; i < 100; i++) r = f.filter(1.0, 1.0/60.0);
		assertEquals(1.0, r, 1e-3);
		assertTrue(r <= 1.0);
	}

	@Test
	public void oneeuro_dt_guard() {
		OneEuroFilter f = new OneEuroFilter(60.0, 1.0, 0.5, 1.0);
		double first = f.filter(2.0, 0.016);
		// dt<=0 must not produce NaN (division-by-zero guard in filter)
		double r = f.filter(5.0, 0.0);
		assertTrue(Double.isFinite(r));
		r = f.filter(5.0, -1.0);
		assertTrue(Double.isFinite(r));
		assertTrue(Double.isFinite(first));
	}

	@Test
	public void oneeuro_copy_independence() {
		OneEuroFilter f = new OneEuroFilter(60.0, 1.0, 0.5, 1.0);
		for (int i = 0; i < 10; i++) f.filter(0.5, 1.0/60.0);

		OneEuroFilter copy = new OneEuroFilter(f);

		// mutating the copy must not disturb the original's state
		for (int i = 0; i < 10; i++) copy.filter(10.0, 1.0/60.0);

		double orig = f.filter(0.5, 1.0/60.0);
		double cp   = copy.filter(0.5, 1.0/60.0);
		assertTrue(Double.isFinite(orig) && Double.isFinite(cp));
		// the copy chased 10.0: it must differ from the original
		assertNotEquals(orig, cp);
	}
}
