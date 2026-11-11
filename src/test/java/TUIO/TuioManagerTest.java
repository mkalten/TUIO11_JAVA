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

public class TuioManagerTest {

	/** TuioListener that records all callbacks. */
	private static class RecordingListener implements TuioListener {
		int addObj, updObj, delObj, addCur, updCur, delCur, addBlb, updBlb, delBlb;
		int lastSymbolID = -1, lastCursorID = -1, lastBlobID = -1, lastState = -1;

		public void addTuioObject(TuioObject tobj) { addObj++; lastSymbolID = tobj.getSymbolID(); }
		public void updateTuioObject(TuioObject tobj) { updObj++; lastSymbolID = tobj.getSymbolID(); }
		public void removeTuioObject(TuioObject tobj) { delObj++; lastState = tobj.getTuioState(); }
		public void addTuioCursor(TuioCursor tcur) { addCur++; lastCursorID = tcur.getCursorID(); }
		public void updateTuioCursor(TuioCursor tcur) { updCur++; lastCursorID = tcur.getCursorID(); }
		public void removeTuioCursor(TuioCursor tcur) { delCur++; lastState = tcur.getTuioState(); }
		public void addTuioBlob(TuioBlob tblb) { addBlb++; lastBlobID = tblb.getBlobID(); }
		public void updateTuioBlob(TuioBlob tblb) { updBlb++; lastBlobID = tblb.getBlobID(); }
		public void removeTuioBlob(TuioBlob tblb) { delBlb++; lastState = tblb.getTuioState(); }
		public void refresh(TuioTime ftime) { }
	}

	@Test
	public void manager_add_update_remove_object() {
		TuioManager manager = new TuioManager();
		RecordingListener listener = new RecordingListener();
		manager.addTuioListener(listener);

		TuioObject tobj = manager.addTuioObject(5, 0.1f, 0.2f, 0.3f);
		assertNotNull(tobj);
		assertEquals(TuioContainer.TUIO_ADDED, tobj.getTuioState());
		assertEquals(5, tobj.getSymbolID());
		assertEquals(1, listener.addObj);
		assertEquals(5, listener.lastSymbolID);
		assertEquals(0, tobj.getSessionID()); // sessionID starts at -1

		// a new frame is needed so the update has a time delta and reports moving
		manager.initFrame(TuioTime.getSystemTime().add(500000));
		manager.updateTuioObject(tobj, 0.4f, 0.5f, 0.6f);
		assertEquals(1, listener.updObj);

		manager.removeTuioObject(tobj);
		assertEquals(1, listener.delObj);
		assertEquals(TuioContainer.TUIO_REMOVED, listener.lastState);
		assertEquals(0, manager.getTuioObjects().size());

		manager.removeTuioListener(listener);
	}

	@Test
	public void manager_cursor_blob_add_remove() {
		TuioManager manager = new TuioManager();
		RecordingListener listener = new RecordingListener();
		manager.addTuioListener(listener);

		TuioCursor tcur = manager.addTuioCursor(0.3f, 0.4f);
		TuioBlob tblb = manager.addTuioBlob(0.5f, 0.6f, 0.7f, 0.1f, 0.1f, 0.01f);
		assertEquals(1, listener.addCur);
		assertEquals(1, listener.addBlb);
		assertEquals(0, listener.lastCursorID);
		assertEquals(0, listener.lastBlobID);
		assertEquals(0, tcur.getSessionID());
		assertEquals(1, tblb.getSessionID()); // shared sessionID counter

		manager.initFrame(TuioTime.getSystemTime().add(500000));
		manager.updateTuioCursor(tcur, 0.5f, 0.6f);
		manager.updateTuioBlob(tblb, 0.7f, 0.6f, 0.8f, 0.2f, 0.2f, 0.04f);
		assertEquals(1, listener.updCur);
		assertEquals(1, listener.updBlb);

		manager.removeTuioCursor(tcur);
		manager.removeTuioBlob(tblb);
		assertEquals(1, listener.delCur);
		assertEquals(1, listener.delBlb);

		manager.removeAllTuioListeners();
	}

	@Test
	public void manager_cursor_id_reuse() {
		TuioManager manager = new TuioManager();
		TuioCursor c1 = manager.addTuioCursor(0.1f, 0.1f);
		assertEquals(0, c1.getCursorID());
		TuioCursor c2 = manager.addTuioCursor(0.9f, 0.9f);
		assertEquals(1, c2.getCursorID());

		// removing a cursor frees the ID for reuse
		manager.removeTuioCursor(c1);
		TuioCursor c3 = manager.addTuioCursor(0.2f, 0.2f);
		assertEquals(0, c3.getCursorID()); // reused freed ID (nearest free position)
	}

	@Test
	public void manager_external_containers() {
		TuioManager manager = new TuioManager();
		RecordingListener listener = new RecordingListener();
		manager.addTuioListener(listener);

		// external cursor gets session + cursor ID assigned by the manager
		TuioCursor ext = new TuioCursor(new TuioTime(0,0), 999, -1, 0.5f, 0.5f);
		manager.addExternalTuioCursor(ext);
		assertEquals(1, listener.addCur);
		assertTrue(ext.getCursorID() >= 0);
		assertEquals(0, ext.getSessionID());

		// external removal does not delete the container
		manager.removeExternalTuioCursor(ext);
		assertEquals(1, listener.delCur);

		TuioBlob extb = new TuioBlob(new TuioTime(0,0), 999, -1, 0.5f, 0.5f, 0.0f, 0.1f, 0.1f, 0.01f);
		manager.addExternalTuioBlob(extb);
		assertTrue(extb.getBlobID() >= 0);
		manager.removeExternalTuioBlob(extb);

		manager.removeAllTuioListeners();
	}

	@Test
	public void manager_untouched_helpers() {
		TuioManager manager = new TuioManager();
		TuioTime t0 = TuioTime.getSystemTime();

		manager.initFrame(t0.add(500000));
		TuioObject tobj = manager.addTuioObject(1, 0.5f, 0.5f, 0.0f);
		manager.commitFrame();

		// new frame without touching the object -> untouched
		manager.initFrame(t0.add(1000000));
		assertEquals(1, manager.getUntouchedObjects().size());

		// update marks it touched for this frame
		manager.updateTuioObject(tobj, 0.6f, 0.5f, 0.0f);
		assertEquals(0, manager.getUntouchedObjects().size());

		// untouched moving objects can be stopped and removed — each stage
		// needs its own frame since stop() marks the object as touched
		manager.initFrame(t0.add(1500000));
		manager.stopUntouchedMovingObjects();          // decelerating
		manager.initFrame(t0.add(2000000));
		manager.stopUntouchedMovingObjects();          // stopped
		manager.initFrame(t0.add(2500000));
		manager.removeUntouchedStoppedObjects();
		assertEquals(0, manager.getTuioObjects().size());
	}

	@Test
	public void manager_closest_inversion_verbose() {
		TuioManager manager = new TuioManager();
		manager.addTuioCursor(0.2f, 0.2f);
		manager.addTuioCursor(0.8f, 0.8f);

		TuioCursor closest = manager.getClosestTuioCursor(0.25f, 0.25f);
		assertNotNull(closest);
		assertEquals(0.2, closest.getX(), 1e-6);

		manager.resetTuioCursors();
		assertEquals(0, manager.getTuioCursors().size());

		// the invert flags are stored but never applied to the coordinates
		manager.setInversion(true, true, false);
		TuioCursor inv = manager.addTuioCursor(0.3f, 0.4f);
		assertEquals(0.3, inv.getX(), 1e-6); // documents the no-op
		assertEquals(0.4, inv.getY(), 1e-6);
		manager.setInversion(false, false, false);

		manager.setVerbose(true);
		assertTrue(manager.isVerbose());
		manager.setVerbose(false);
	}

	@Test
	public void manager_frame_session_ids() {
		TuioManager manager = new TuioManager();
		manager.initFrame(new TuioTime(10,0));
		assertEquals(0, manager.getFrameID());
		manager.commitFrame();
		manager.initFrame(new TuioTime(11,0));
		assertEquals(1, manager.getFrameID());

		// the session ID advances with each added container
		int s0 = manager.getSessionID();
		manager.addTuioObject(1, 0.5f, 0.5f, 0.0f);
		assertTrue(manager.getSessionID() > s0);
	}
}
