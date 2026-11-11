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

public class TuioPointTest {

	@Test
	public void tuiopoint_ctors() {
		TuioPoint a = new TuioPoint(0.3f, 0.7f);
		assertEquals(0.3, a.getX(), 1e-6);
		assertEquals(0.7, a.getY(), 1e-6);

		TuioTime tt = new TuioTime(5, 0);
		TuioPoint b = new TuioPoint(tt, 0.1f, 0.2f);
		assertEquals(tt, b.getTuioTime());
		assertEquals(tt, b.getStartTime());

		TuioPoint c = new TuioPoint(a);
		assertEquals(0.3, c.getX(), 1e-6);
		assertEquals(0.7, c.getY(), 1e-6);

		TuioPoint d = new TuioPoint(b);
		assertEquals(0.1, d.getX(), 1e-6);
		assertEquals(tt, d.getTuioTime());
	}

	@Test
	public void tuiopoint_update() {
		TuioPoint p = new TuioPoint(0.0f, 0.0f);
		p.update(0.5f, 0.5f);
		assertEquals(0.5, p.getX(), 1e-6);

		TuioPoint q = new TuioPoint(0.9f, 0.9f);
		p.update(q);
		assertEquals(0.9, p.getX(), 1e-6);

		TuioTime tt = new TuioTime(2, 0);
		p.update(tt, 0.1f, 0.1f);
		assertEquals(0.1, p.getX(), 1e-6);
		assertEquals(tt, p.getTuioTime());
	}

	@Test
	public void tuiopoint_distance_angle() {
		TuioPoint p = new TuioPoint(0.0f, 0.0f);
		assertEquals(0.5, p.getDistance(0.3f, 0.4f), 1e-5);
		assertEquals(500, p.getScreenDistance(0.3f, 0.4f, 1000, 1000), 1e-2);

		TuioPoint q = new TuioPoint(0.6f, 0.8f);
		assertEquals(1.0, p.getDistance(q), 1e-5);

		// coincident points return 0 (division-by-zero guard)
		assertEquals(0.0, p.getAngle(0.0f, 0.0f), 1e-6);
		assertEquals(0.0, p.getAngle(p), 1e-6);

		// getAngle(xp,yp) is the direction from (xp,yp) to this point
		assertEquals(0.0, p.getAngle(1.0f, 0.0f), 1e-4);                     // east
		assertEquals(Math.PI, p.getAngle(-1.0f, 0.0f), 1e-4);              // west
		assertEquals(Math.PI/2, p.getAngle(0.0f, -1.0f), 1e-4);            // height>0, no wrap
		assertEquals(3*Math.PI/2, p.getAngle(0.0f, 1.0f), 1e-4);           // height<0 wraps
	}

	@Test
	public void tuiopoint_screen() {
		TuioPoint p = new TuioPoint(0.504f, 0.496f);
		assertEquals(504, p.getScreenX(1000));
		assertEquals(496, p.getScreenY(1000));
	}

	@Test
	public void tuiopoint_threshold() {
		TuioPoint p = new TuioPoint(0.5f, 0.5f);
		p.addPositionThreshold(0.1f);

		// small change inside the threshold is suppressed
		p.update(new TuioTime(1,0), 0.55f, 0.55f);
		assertEquals(0.5, p.getX(), 1e-6);

		// larger change is applied
		p.update(new TuioTime(2,0), 0.8f, 0.5f);
		assertEquals(0.8, p.getX(), 1e-6);

		p.removePositionThreshold();
		p.update(new TuioTime(3,0), 0.55f, 0.5f);
		assertEquals(0.55, p.getX(), 1e-6);
	}

	@Test
	public void tuiopoint_copy_independent_filters() {
		TuioPoint p = new TuioPoint(0.5f, 0.5f);
		p.addPositionFilter(1.0f, 0.1f);

		// deep-copy: mutating the copy's filter must not affect the original
		TuioPoint copy = new TuioPoint(p);
		copy.update(new TuioTime(1,0), 0.9f, 0.9f);
		copy.update(new TuioTime(2,0), 0.9f, 0.9f);

		p.update(new TuioTime(3,0), 0.6f, 0.6f);
		assertTrue(p.getX() < 0.9);  // would equal copy's filtered value if filters were shared
		assertTrue(Float.isFinite(p.getX()));
		assertTrue(Float.isFinite(copy.getX()));
	}
}
