/*
 * TUIO Java library
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

package TUIO;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The TuioDispatcher class manages the lists of active TuioObjects,
 * TuioCursors and TuioBlobs and the registered TuioListener callbacks.
 * All list access is guarded by per-type locks.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public class TuioDispatcher {

	protected final List<TuioListener> listenerList = new CopyOnWriteArrayList<TuioListener>();

	protected final List<TuioObject> objectList = new LinkedList<TuioObject>();
	protected final List<TuioCursor> cursorList = new LinkedList<TuioCursor>();
	protected final List<TuioBlob> blobList = new LinkedList<TuioBlob>();

	protected final ReentrantLock objectLock = new ReentrantLock();
	protected final ReentrantLock cursorLock = new ReentrantLock();
	protected final ReentrantLock blobLock = new ReentrantLock();

	/**
	 * Adds the provided TuioListener to the list of registered TUIO event listeners
	 *
	 * @param	listener	the TuioListener to add
	 */
	public void addTuioListener(TuioListener listener) {
		listenerList.add(listener);
	}

	/**
	 * Removes the provided TuioListener from the list of registered TUIO event listeners
	 *
	 * @param	listener	the TuioListener to remove
	 */
	public void removeTuioListener(TuioListener listener) {
		listenerList.remove(listener);
	}

	/**
	 * Removes all TuioListener from the list of registered TUIO event listeners
	 */
	public void removeAllTuioListeners() {
		listenerList.clear();
	}

	/**
	 * Returns a List of all currently active TuioObjects
	 *
	 * @return	a List of all currently active TuioObjects
	 */
	public List<TuioObject> getTuioObjects() {
		objectLock.lock();
		try {
			return new ArrayList<TuioObject>(objectList);
		} finally {
			objectLock.unlock();
		}
	}

	/**
	 * Returns the number of all currently active TuioObjects
	 *
	 * @return	the number of all currently active TuioObjects
	 */
	public int getTuioObjectCount() {
		objectLock.lock();
		try {
			return objectList.size();
		} finally {
			objectLock.unlock();
		}
	}

	/**
	 * Returns the TuioObject with the provided Session ID
	 *
	 * @param	s_id	the Session ID of the TuioObject to look up
	 * @return	the TuioObject or null if no such Session ID exists
	 */
	public TuioObject getTuioObject(long s_id) {
		objectLock.lock();
		try {
			for (TuioObject tobj : objectList)
				if (tobj.getSessionID()==s_id) return tobj;
			return null;
		} finally {
			objectLock.unlock();
		}
	}

	/**
	 * Returns a List with a copy of currently active TuioObjects
	 *
	 * @return	a List with a copy of all currently active TuioObjects
	 */
	public List<TuioObject> copyTuioObjects() {
		objectLock.lock();
		try {
			List<TuioObject> buffer = new ArrayList<TuioObject>();
			for (TuioObject tobj : objectList) buffer.add(new TuioObject(tobj));
			return buffer;
		} finally {
			objectLock.unlock();
		}
	}

	/**
	 * Returns a List of all currently active TuioCursors
	 *
	 * @return	a List of all currently active TuioCursors
	 */
	public List<TuioCursor> getTuioCursors() {
		cursorLock.lock();
		try {
			return new ArrayList<TuioCursor>(cursorList);
		} finally {
			cursorLock.unlock();
		}
	}

	/**
	 * Returns the number of all currently active TuioCursors
	 *
	 * @return	the number of all currently active TuioCursors
	 */
	public int getTuioCursorCount() {
		cursorLock.lock();
		try {
			return cursorList.size();
		} finally {
			cursorLock.unlock();
		}
	}

	/**
	 * Returns the TuioCursor with the provided Session ID
	 *
	 * @param	s_id	the Session ID of the TuioCursor to look up
	 * @return	the TuioCursor or null if no such Session ID exists
	 */
	public TuioCursor getTuioCursor(long s_id) {
		cursorLock.lock();
		try {
			for (TuioCursor tcur : cursorList)
				if (tcur.getSessionID()==s_id) return tcur;
			return null;
		} finally {
			cursorLock.unlock();
		}
	}

	/**
	 * Returns a List with a copy of currently active TuioCursors
	 *
	 * @return	a List with a copy of all currently active TuioCursors
	 */
	public List<TuioCursor> copyTuioCursors() {
		cursorLock.lock();
		try {
			List<TuioCursor> buffer = new ArrayList<TuioCursor>();
			for (TuioCursor tcur : cursorList) buffer.add(new TuioCursor(tcur));
			return buffer;
		} finally {
			cursorLock.unlock();
		}
	}

	/**
	 * Returns a List of all currently active TuioBlobs
	 *
	 * @return	a List of all currently active TuioBlobs
	 */
	public List<TuioBlob> getTuioBlobs() {
		blobLock.lock();
		try {
			return new ArrayList<TuioBlob>(blobList);
		} finally {
			blobLock.unlock();
		}
	}

	/**
	 * Returns the number of all currently active TuioBlobs
	 *
	 * @return	the number of all currently active TuioBlobs
	 */
	public int getTuioBlobCount() {
		blobLock.lock();
		try {
			return blobList.size();
		} finally {
			blobLock.unlock();
		}
	}

	/**
	 * Returns the TuioBlob with the provided Session ID
	 *
	 * @param	s_id	the Session ID of the TuioBlob to look up
	 * @return	the TuioBlob or null if no such Session ID exists
	 */
	public TuioBlob getTuioBlob(long s_id) {
		blobLock.lock();
		try {
			for (TuioBlob tblb : blobList)
				if (tblb.getSessionID()==s_id) return tblb;
			return null;
		} finally {
			blobLock.unlock();
		}
	}

	/**
	 * Returns a List with a copy of currently active TuioBlobs
	 *
	 * @return	a List with a copy of all currently active TuioBlobs
	 */
	public List<TuioBlob> copyTuioBlobs() {
		blobLock.lock();
		try {
			List<TuioBlob> buffer = new ArrayList<TuioBlob>();
			for (TuioBlob tblb : blobList) buffer.add(new TuioBlob(tblb));
			return buffer;
		} finally {
			blobLock.unlock();
		}
	}

	/**
	 * Locks the TuioObject list in order to avoid updates during access
	 */
	public void lockObjectList() {
		objectLock.lock();
	}

	/**
	 * Unlocks the TuioObject list
	 */
	public void unlockObjectList() {
		objectLock.unlock();
	}

	/**
	 * Locks the TuioCursor list in order to avoid updates during access
	 */
	public void lockCursorList() {
		cursorLock.lock();
	}

	/**
	 * Unlocks the TuioCursor list
	 */
	public void unlockCursorList() {
		cursorLock.unlock();
	}

	/**
	 * Locks the TuioBlob list in order to avoid updates during access
	 */
	public void lockBlobList() {
		blobLock.lock();
	}

	/**
	 * Unlocks the TuioBlob list
	 */
	public void unlockBlobList() {
		blobLock.unlock();
	}
}
