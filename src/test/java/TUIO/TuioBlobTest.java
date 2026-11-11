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

public class TuioBlobTest {

	@Test
	public void tuioblob_fields() {
		TuioBlob tblb = new TuioBlob(new TuioTime(0,0), 7, 4, 0.5f, 0.6f, 0.3f, 0.2f, 0.1f, 0.02f);
		assertEquals(7, tblb.getSessionID());
		assertEquals(4, tblb.getBlobID());
		assertEquals(0.2, tblb.getWidth(), 1e-6);
		assertEquals(0.1, tblb.getHeight(), 1e-6);
		assertEquals(0.02, tblb.getArea(), 1e-6);
		assertEquals(0.3, tblb.getAngle(), 1e-6);

		tblb.setBlobID(11);
		assertEquals(11, tblb.getBlobID());
		assertEquals(200, tblb.getScreenWidth(1000));
		assertEquals(100, tblb.getScreenHeight(1000));
	}

	@Test
	public void tuioblob_update() {
		TuioBlob tblb = new TuioBlob(1, 0, 0.0f, 0.0f, 0.0f, 0.1f, 0.1f, 0.01f);
		tblb.update(new TuioTime(1,0), 0.3f, 0.4f, 0.5f, 0.2f, 0.2f, 0.04f);
		assertEquals(0.3, tblb.getX(), 1e-6);
		assertEquals(0.5, tblb.getAngle(), 1e-4);
		assertEquals(0.2, tblb.getWidth(), 1e-6);
		assertEquals(0.04, tblb.getArea(), 1e-6);
		assertNotEquals(0.0f, tblb.getRotationSpeed());
	}

	@Test
	public void tuioblob_update_full() {
		TuioBlob tblb = new TuioBlob(1, 0, 0.0f, 0.0f, 0.0f, 0.1f, 0.1f, 0.01f);
		tblb.update(0.2f, 0.3f, 0.4f, 0.15f, 0.15f, 0.0225f, 1.0f, 0.5f, 0.25f, 0.5f, 0.1f);
		assertEquals(1.0, tblb.getXSpeed(), 1e-6);
		assertEquals(0.25, tblb.getRotationSpeed(), 1e-6);
		assertEquals(0.15, tblb.getWidth(), 1e-6);
	}

	@Test
	public void tuioblob_thresholds_filters() {
		TuioBlob tblb = new TuioBlob(1, 0, 0.0f, 0.0f, 0.0f, 0.1f, 0.1f, 0.01f);

		tblb.addAngleThreshold(0.2f);
		tblb.addSizeThreshold(0.2f);
		tblb.update(new TuioTime(1,0), 0.0f, 0.0f, 0.1f, 0.12f, 0.1f, 0.01f);
		assertEquals(0.0, tblb.getAngle(), 1e-6);   // below angle threshold
		assertEquals(0.1, tblb.getWidth(), 1e-6);   // below size threshold
		tblb.removeAngleThreshold();
		tblb.removeSizeThreshold();

		tblb.addAngleFilter(1.0f, 0.1f);
		tblb.addSizeFilter(1.0f, 0.1f);
		tblb.update(new TuioTime(2,0), 0.0f, 0.0f, 1.0f, 0.3f, 0.3f, 0.09f);
		assertTrue(Float.isFinite(tblb.getAngle()));
		assertTrue(Float.isFinite(tblb.getWidth()));

		// deep copy keeps independent filters
		TuioBlob copy = new TuioBlob(tblb);
		copy.update(new TuioTime(3,0), 0.0f, 0.0f, 5.0f, 0.9f, 0.9f, 0.8f);
		tblb.update(new TuioTime(3,0), 0.0f, 0.0f, 0.6f, 0.4f, 0.4f, 0.16f);
		assertNotEquals(tblb.getAngle(), copy.getAngle());
		assertTrue(Float.isFinite(tblb.getWidth()));

		tblb.removeAngleFilter();
		tblb.removeSizeFilter();
	}

	@Test
	public void tuioblob_copy() {
		TuioBlob tblb = new TuioBlob(2, 3, 0.4f, 0.5f, 0.6f, 0.1f, 0.2f, 0.02f);
		tblb.update(new TuioTime(1,0), 0.5f, 0.5f, 0.7f, 0.2f, 0.2f, 0.04f);
		TuioBlob copy = new TuioBlob(tblb);
		assertEquals(3, copy.getBlobID());
		assertEquals(tblb.getAngle(), copy.getAngle(), 1e-6);
		assertEquals(tblb.getPath().size(), copy.getPath().size());
	}
}
