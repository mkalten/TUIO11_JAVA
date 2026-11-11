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

public class TuioCursorTest {

	@Test
	public void tuiocursor_id() {
		TuioCursor tcur = new TuioCursor(new TuioTime(0,0), 5, 3, 0.2f, 0.4f);
		assertEquals(3, tcur.getCursorID());
		tcur.setCursorID(9);
		assertEquals(9, tcur.getCursorID());
	}

	@Test
	public void tuiocursor_update() {
		TuioCursor tcur = new TuioCursor(new TuioTime(0,0), 5, 0, 0.0f, 0.0f);
		tcur.update(new TuioTime(1,0), 0.6f, 0.8f);
		assertEquals(0.6, tcur.getX(), 1e-6);
		assertEquals(0.8, tcur.getY(), 1e-6);
		assertEquals(1.0, tcur.getMotionSpeed(), 1e-4); // 0.6^2+0.8^2 = 1.0 over 1s
		assertEquals(TuioContainer.TUIO_ACCELERATING, tcur.getTuioState());
	}

	@Test
	public void tuiocursor_copy() {
		TuioCursor tcur = new TuioCursor(new TuioTime(0,0), 5, 1, 0.1f, 0.2f);
		tcur.update(new TuioTime(1,0), 0.3f, 0.4f);

		// copy ctor preserves state and path
		TuioCursor ref = new TuioCursor(tcur);
		assertEquals(5, ref.getSessionID());
		assertEquals(tcur.getTuioState(), ref.getTuioState());
		assertEquals(0.3, ref.getX(), 1e-6);
		assertEquals(tcur.getPath().size(), ref.getPath().size());
	}
}
