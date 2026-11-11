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

import java.util.List;

import org.junit.jupiter.api.Test;

public class TuioDispatcherTest {

	/** minimal listener for counting registrations */
	private static class EmptyListener implements TuioListener {
		public void addTuioObject(TuioObject tobj) { }
		public void updateTuioObject(TuioObject tobj) { }
		public void removeTuioObject(TuioObject tobj) { }
		public void addTuioCursor(TuioCursor tcur) { }
		public void updateTuioCursor(TuioCursor tcur) { }
		public void removeTuioCursor(TuioCursor tcur) { }
		public void addTuioBlob(TuioBlob tblb) { }
		public void updateTuioBlob(TuioBlob tblb) { }
		public void removeTuioBlob(TuioBlob tblb) { }
		public void refresh(TuioTime ftime) { }
	}

	@Test
	public void dispatcher_listeners() {
		TuioDispatcher disp = new TuioDispatcher();
		EmptyListener l1 = new EmptyListener(), l2 = new EmptyListener();
		disp.addTuioListener(l1);
		disp.addTuioListener(l2);
		assertEquals(2, disp.listenerList.size());

		disp.removeTuioListener(l1);
		assertEquals(1, disp.listenerList.size());
		disp.removeAllTuioListeners();
		assertEquals(0, disp.listenerList.size());
	}

	@Test
	public void dispatcher_getters_counts() {
		TuioDispatcher disp = new TuioDispatcher();
		disp.objectList.add(new TuioObject(new TuioTime(0,0), 11, 5, 0.1f, 0.2f, 0.3f));
		disp.objectList.add(new TuioObject(new TuioTime(0,0), 12, 6, 0.3f, 0.4f, 0.5f));
		disp.cursorList.add(new TuioCursor(new TuioTime(0,0), 21, 0, 0.5f, 0.5f));
		disp.blobList.add(new TuioBlob(new TuioTime(0,0), 31, 0, 0.5f, 0.5f, 0.0f, 0.1f, 0.1f, 0.01f));

		assertEquals(2, disp.getTuioObjectCount());
		assertEquals(1, disp.getTuioCursorCount());
		assertEquals(1, disp.getTuioBlobCount());
		assertEquals(2, disp.getTuioObjects().size());
		assertEquals(2, disp.copyTuioObjects().size());
		assertEquals(1, disp.copyTuioCursors().size());
		assertEquals(1, disp.copyTuioBlobs().size());

		TuioObject found = disp.getTuioObject(12);
		assertNotNull(found);
		assertEquals(6, found.getSymbolID());
		assertNull(disp.getTuioObject(99));
		assertNotNull(disp.getTuioCursor(21));
		assertNotNull(disp.getTuioBlob(31));

		// copies are independent values
		List<TuioObject> copies = disp.copyTuioObjects();
		copies.get(0).setSymbolID(999);
		assertEquals(6, disp.getTuioObject(12).getSymbolID());
	}

	@Test
	public void dispatcher_lock_unlock() {
		TuioDispatcher disp = new TuioDispatcher();
		// balanced lock/unlock pairs must not deadlock
		disp.lockObjectList();
		disp.unlockObjectList();
		disp.lockCursorList();
		disp.unlockCursorList();
		disp.lockBlobList();
		disp.unlockBlobList();
	}
}
