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

// shared TuioContainer behavior, exercised through each subclass
public class TuioContainerTest {

	@Test
	public void container_initial_state() {
		TuioTime tt = new TuioTime(1, 0);
		TuioObject tobj = new TuioObject(tt, 42, 7, 0.1f, 0.2f, 0.5f);
		TuioCursor tcur = new TuioCursor(tt, 43, 0, 0.3f, 0.4f);
		TuioBlob tblb = new TuioBlob(tt, 44, 1, 0.5f, 0.6f, 0.2f, 0.1f, 0.1f, 0.01f);

		assertEquals(42, tobj.getSessionID());
		assertEquals(43, tcur.getSessionID());
		assertEquals(44, tblb.getSessionID());
		assertEquals(TuioContainer.TUIO_ADDED, tobj.getTuioState());
		assertEquals(TuioContainer.TUIO_ADDED, tcur.getTuioState());
		assertEquals(TuioContainer.TUIO_ADDED, tblb.getTuioState());
		assertEquals(tt, tobj.getTuioTime());

		// ctor seeds the path with the initial position
		assertEquals(1, tobj.getPath().size());
		assertEquals(1, tcur.getPath().size());
		assertEquals(1, tblb.getPath().size());
	}

	@Test
	public void container_update_speeds() {
		TuioObject tobj = new TuioObject(new TuioTime(0,0), 1, 1, 0.0f, 0.0f, 0.0f);
		// move 0.4 units in x over 1 second -> x_speed 0.4, accelerating
		tobj.update(new TuioTime(1,0), 0.4f, 0.0f, 0.0f);
		assertEquals(0.4, tobj.getXSpeed(), 1e-5);
		assertEquals(0.0, tobj.getYSpeed(), 1e-6);
		assertEquals(0.4, tobj.getMotionSpeed(), 1e-5);
		assertTrue(tobj.getMotionAccel() > 0);
		assertEquals(TuioContainer.TUIO_ACCELERATING, tobj.getTuioState());
		assertTrue(tobj.isMoving());
		assertEquals(2, tobj.getPath().size());

		// slowing to a stop first reports decelerating ...
		tobj.update(new TuioTime(2,0), 0.5f, 0.0f, 0.0f);
		assertEquals(0.1, tobj.getXSpeed(), 1e-5);
		assertTrue(tobj.getMotionAccel() < 0);
		assertEquals(TuioContainer.TUIO_DECELERATING, tobj.getTuioState());

		// ... one further motionless update is required before it stops
		tobj.update(new TuioTime(3,0), 0.5f, 0.0f, 0.0f);
		assertEquals(TuioContainer.TUIO_DECELERATING, tobj.getTuioState());
		tobj.update(new TuioTime(4,0), 0.5f, 0.0f, 0.0f);
		assertEquals(TuioContainer.TUIO_STOPPED, tobj.getTuioState());
		assertFalse(tobj.isMoving());
	}

	@Test
	public void container_update_dt0_no_nan() {
		TuioCursor tcur = new TuioCursor(new TuioTime(1,0), 1, 0, 0.0f, 0.0f);
		// same timestamp -> dt==0 must not produce NaN speeds
		tcur.update(new TuioTime(1,0), 0.5f, 0.5f);
		assertTrue(Float.isFinite(tcur.getXSpeed()));
		assertTrue(Float.isFinite(tcur.getMotionSpeed()));
		assertTrue(Float.isFinite(tcur.getMotionAccel()));
	}

	@Test
	public void container_update_explicit_speeds() {
		TuioBlob tblb = new TuioBlob(1, 0, 0.0f, 0.0f, 0.0f, 0.1f, 0.1f, 0.01f);
		tblb.update(new TuioTime(1,0), 0.3f, 0.4f, 0.0f, 0.1f, 0.1f, 0.01f, 0.6f, 0.8f, 0.0f, 0.5f, 0.0f);
		assertEquals(0.6, tblb.getXSpeed(), 1e-6);
		assertEquals(0.8, tblb.getYSpeed(), 1e-6);
		assertEquals(1.0, tblb.getMotionSpeed(), 1e-5);
		assertEquals(0.5, tblb.getMotionAccel(), 1e-6);
		assertEquals(TuioContainer.TUIO_ACCELERATING, tblb.getTuioState());
	}

	@Test
	public void container_stop_remove() {
		TuioObject tobj = new TuioObject(new TuioTime(0,0), 1, 1, 0.0f, 0.0f, 0.0f);
		tobj.update(new TuioTime(1,0), 0.5f, 0.0f, 0.0f);
		assertTrue(tobj.isMoving());
		tobj.stop(new TuioTime(2,0)); // performs a zero-motion update -> decelerating
		assertEquals(TuioContainer.TUIO_DECELERATING, tobj.getTuioState());
		tobj.update(new TuioTime(3,0), 0.5f, 0.0f, 0.0f);
		assertEquals(TuioContainer.TUIO_STOPPED, tobj.getTuioState());
		tobj.remove(new TuioTime(4,0));
		assertEquals(TuioContainer.TUIO_REMOVED, tobj.getTuioState());
		assertEquals(new TuioTime(4,0), tobj.getTuioTime());
	}

	@Test
	public void container_source() {
		TuioCursor tcur = new TuioCursor(1, 0, 0.0f, 0.0f);
		assertEquals(0, tcur.getTuioSourceID());
		tcur.setTuioSource(3, "reacTIVision", "192.168.0.1");
		assertEquals(3, tcur.getTuioSourceID());
		assertEquals("reacTIVision", tcur.getTuioSourceName());
		assertEquals("192.168.0.1", tcur.getTuioSourceAddress());
	}

	@Test
	public void container_copy_lastpoint_repoint() {
		// regression: lastPoint must point into the copy's path, not the source's
		TuioCursor orig = new TuioCursor(new TuioTime(0,0), 1, 0, 0.0f, 0.0f);
		orig.update(new TuioTime(1,0), 0.2f, 0.0f);
		orig.update(new TuioTime(2,0), 0.4f, 0.0f);
		assertEquals(3, orig.getPath().size());

		TuioCursor copy = new TuioCursor(orig);
		assertEquals(3, copy.getPath().size());
		// a subsequent update on the copy must stay consistent
		copy.update(new TuioTime(3,0), 0.6f, 0.0f);
		assertEquals(0.6, copy.getX(), 1e-6);
		assertEquals(4, copy.getPath().size());
		assertTrue(Float.isFinite(copy.getXSpeed()));
		// original untouched
		assertEquals(3, orig.getPath().size());
	}

	@Test
	public void container_predict_position() {
		TuioObject tobj = new TuioObject(new TuioTime(0,0), 1, 1, 0.0f, 0.0f, 0.0f);
		tobj.update(new TuioTime(1,0), 0.5f, 0.0f, 0.0f); // x_speed = 0.5/s, x_accel = 0.5/s^2
		TuioPoint pred = tobj.predictPosition();
		// x + v*dt + 0.5*a*dt^2 = 0.5 + 0.5*1 + 0.5*0.5*1 = 1.25
		assertEquals(1.25, pred.getX(), 1e-5);
		assertEquals(0.0, pred.getY(), 1e-6);
	}

	@Test
	public void container_path_capped() {
		TuioCursor tcur = new TuioCursor(new TuioTime(0,0), 1, 0, 0.0f, 0.0f);
		for (int i = 1; i <= TuioContainer.MAX_PATH_LENGTH + 10; i++)
			tcur.update(new TuioTime(i,0), (float)i/1000.0f, 0.0f);
		assertTrue(tcur.getPath().size() <= TuioContainer.MAX_PATH_LENGTH);
	}
}
