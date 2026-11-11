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

public class TuioObjectTest {

	@Test
	public void tuioobject_symbol_angle() {
		TuioObject tobj = new TuioObject(new TuioTime(0,0), 1, 42, 0.5f, 0.5f, (float)Math.PI/2);
		assertEquals(42, tobj.getSymbolID());
		assertEquals(Math.PI/2, tobj.getAngle(), 1e-5);
		assertEquals(90.0, tobj.getAngleDegrees(), 1e-3);

		tobj.setSymbolID(7);
		assertEquals(7, tobj.getSymbolID());
	}

	@Test
	public void tuioobject_rotation() {
		TuioObject tobj = new TuioObject(new TuioTime(0,0), 1, 1, 0.0f, 0.0f, 0.0f);
		// rotate by pi/2 over 1 second
		tobj.update(new TuioTime(1,0), 0.0f, 0.0f, (float)Math.PI/2);
		assertEquals(Math.PI/2, tobj.getAngle(), 1e-4);
		// rotation_speed is in turns per second: 0.25 turns/s
		assertEquals(0.25, tobj.getRotationSpeed(), 1e-4);
		assertNotEquals(0.0f, tobj.getRotationAccel());
		assertTrue(tobj.isMoving()); // TUIO_ROTATING counts as moving
	}

	@Test
	public void tuioobject_rotation_wrap() {
		TuioObject tobj = new TuioObject(new TuioTime(0,0), 1, 1, 0.0f, 0.0f, 0.1f);
		// rotate past the 2pi boundary wraps into [0,2pi)
		tobj.update(new TuioTime(1,0), 0.0f, 0.0f, (float)(2*Math.PI + 0.2));
		assertTrue(tobj.getAngle() >= 0.0f && tobj.getAngle() <= 2*Math.PI);
	}

	@Test
	public void tuioobject_angle_threshold() {
		TuioObject tobj = new TuioObject(new TuioTime(0,0), 1, 1, 0.0f, 0.0f, 0.0f);
		tobj.addAngleThreshold(0.1f);
		// change below the threshold is suppressed
		tobj.update(new TuioTime(1,0), 0.0f, 0.0f, 0.05f);
		assertEquals(0.0, tobj.getAngle(), 1e-6);
		// change above it is applied
		tobj.update(new TuioTime(2,0), 0.0f, 0.0f, 0.5f);
		assertEquals(0.5, tobj.getAngle(), 1e-5);
		tobj.removeAngleThreshold();
	}

	@Test
	public void tuioobject_angle_filter_copy() {
		TuioObject tobj = new TuioObject(new TuioTime(0,0), 1, 1, 0.0f, 0.0f, 0.0f);
		tobj.addAngleFilter(1.0f, 0.1f);
		tobj.update(new TuioTime(1,0), 0.0f, 0.0f, 0.4f);

		// deep copy: independent filter state
		TuioObject copy = new TuioObject(tobj);
		copy.update(new TuioTime(2,0), 0.0f, 0.0f, 3.0f);
		tobj.update(new TuioTime(2,0), 0.0f, 0.0f, 0.5f);
		assertTrue(Float.isFinite(tobj.getAngle()));
		assertTrue(Float.isFinite(copy.getAngle()));
		assertNotEquals(tobj.getAngle(), copy.getAngle());

		tobj.removeAngleFilter();
		tobj.update(new TuioTime(3,0), 0.0f, 0.0f, 1.0f);
		assertEquals(1.0, tobj.getAngle(), 1e-5);
	}

	@Test
	public void tuioobject_update_full() {
		TuioObject tobj = new TuioObject(new TuioTime(0,0), 1, 1, 0.0f, 0.0f, 0.0f);
		tobj.update(0.2f, 0.3f, 0.4f, 1.0f, 0.0f, 0.25f, 0.5f, 0.1f);
		assertEquals(0.2, tobj.getX(), 1e-6);
		assertEquals(0.4, tobj.getAngle(), 1e-6);
		assertEquals(1.0, tobj.getXSpeed(), 1e-6);
		assertEquals(0.25, tobj.getRotationSpeed(), 1e-6);
	}
}
