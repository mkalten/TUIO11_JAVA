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
import java.util.List;

/**
 * The TuioManager class manages all TuioObjects, TuioCursors and TuioBlobs
 * on the TUIO server side, providing ADD, UPDATE and REMOVE methods
 * in analogy to the TuioClient's TuioListener interface.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public class TuioManager extends TuioDispatcher {

	private final List<TuioCursor> freeCursorList = new ArrayList<TuioCursor>();
	private final List<TuioCursor> freeCursorBuffer = new ArrayList<TuioCursor>();
	private final List<TuioBlob> freeBlobList = new ArrayList<TuioBlob>();
	private final List<TuioBlob> freeBlobBuffer = new ArrayList<TuioBlob>();

	private TuioTime currentFrameTime = TuioTime.getSystemTime();
	private int currentFrame = -1;
	private int maxCursorID = -1;
	private int maxBlobID = -1;
	private int sessionID = -1;

	protected boolean updateObject = false;
	protected boolean updateCursor = false;
	protected boolean updateBlob = false;
	private boolean verbose = false;

	protected boolean invert_x = false;
	protected boolean invert_y = false;
	protected boolean invert_a = false;

	// ---- TuioObject management ----

	/**
	 * Creates a new TuioObject based on the given arguments.
	 *
	 * @param	sym	the symbol ID
	 * @param	xp	the X coordinate
	 * @param	yp	the Y coordinate
	 * @param	a	the angle
	 * @return	the created TuioObject
	 */
	public TuioObject addTuioObject(int sym, float xp, float yp, float a) {
		sessionID++;
		TuioObject tobj = new TuioObject(currentFrameTime, sessionID, sym, xp, yp, a);
		objectList.add(tobj);
		updateObject = true;
		notifyAddObject(tobj);
		if (verbose)
			System.out.println("add obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ") " + tobj.getX() + " " + tobj.getY() + " " + tobj.getAngle());
		return tobj;
	}

	/**
	 * Adds an externally managed TuioObject to the internal list of active TuioObjects.
	 *
	 * @param	tobj	the TuioObject to add
	 */
	public void addExternalTuioObject(TuioObject tobj) {
		if (tobj == null) return;
		tobj.setSessionID(++sessionID);
		objectList.add(tobj);
		updateObject = true;
		notifyAddObject(tobj);
		if (verbose)
			System.out.println("add obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ") " + tobj.getX() + " " + tobj.getY() + " " + tobj.getAngle());
	}

	/**
	 * Updates the referenced TuioObject based on the given arguments.
	 *
	 * @param	tobj	the TuioObject to update
	 * @param	xp	the X coordinate
	 * @param	yp	the Y coordinate
	 * @param	a	the angle
	 */
	public void updateTuioObject(TuioObject tobj, float xp, float yp, float a) {
		if (tobj == null) return;
		if (tobj.getTuioTime().equals(currentFrameTime)) return;
		tobj.update(currentFrameTime, xp, yp, a);
		updateObject = true;
		if (tobj.isMoving()) {
			notifyUpdateObject(tobj);
			if (verbose)
				System.out.println("set obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ") " + tobj.getX() + " " + tobj.getY() + " " + tobj.getAngle()
						+ " " + tobj.getXSpeed() + " " + tobj.getYSpeed() + " " + tobj.getRotationSpeed() + " " + tobj.getMotionAccel() + " " + tobj.getRotationAccel());
		}
	}

	/**
	 * Updates an externally managed TuioObject.
	 *
	 * @param	tobj	the TuioObject to update
	 */
	public void updateExternalTuioObject(TuioObject tobj) {
		if (tobj == null) return;
		updateObject = true;
		if (tobj.isMoving()) {
			notifyUpdateObject(tobj);
			if (verbose)
				System.out.println("set obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ") " + tobj.getX() + " " + tobj.getY() + " " + tobj.getAngle()
						+ " " + tobj.getXSpeed() + " " + tobj.getYSpeed() + " " + tobj.getRotationSpeed() + " " + tobj.getMotionAccel() + " " + tobj.getRotationAccel());
		}
	}

	/**
	 * Removes the referenced TuioObject from the internal list of TuioObjects.
	 *
	 * @param	tobj	the TuioObject to remove
	 */
	public void removeTuioObject(TuioObject tobj) {
		if (tobj == null) return;
		objectList.remove(tobj);
		tobj.remove(currentFrameTime);
		updateObject = true;
		notifyRemoveObject(tobj);
		if (verbose)
			System.out.println("del obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ")");
	}

	/**
	 * Removes an externally managed TuioObject from the internal list of TuioObjects.
	 *
	 * @param	tobj	the TuioObject to remove
	 */
	public void removeExternalTuioObject(TuioObject tobj) {
		if (tobj == null) return;
		objectList.remove(tobj);
		updateObject = true;
		notifyRemoveObject(tobj);
		if (verbose)
			System.out.println("del obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ")");
	}

	// ---- TuioCursor management ----

	/**
	 * Creates a new TuioCursor based on the given arguments.
	 *
	 * @param	xp	the X coordinate
	 * @param	yp	the Y coordinate
	 * @return	the created TuioCursor
	 */
	public TuioCursor addTuioCursor(float xp, float yp) {
		sessionID++;

		int cursorID = cursorList.size();
		if (cursorList.size() <= maxCursorID) {
			if (!freeCursorList.isEmpty()) {
				TuioCursor closestCursor = null;
				for (TuioCursor iter : freeCursorList)
					if (closestCursor == null || iter.getDistance(xp, yp) < closestCursor.getDistance(xp, yp))
						closestCursor = iter;
				cursorID = closestCursor.getCursorID();
				freeCursorList.remove(closestCursor);
			} else {
				cursorID = maxCursorID + 1;
				maxCursorID = cursorID;
			}
		} else {
			maxCursorID = cursorID;
		}

		TuioCursor tcur = new TuioCursor(currentFrameTime, sessionID, cursorID, xp, yp);
		cursorList.add(tcur);
		updateCursor = true;
		notifyAddCursor(tcur);
		if (verbose)
			System.out.println("add cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ") " + tcur.getX() + " " + tcur.getY());
		return tcur;
	}

	/**
	 * Adds an externally managed TuioCursor.
	 *
	 * @param	tcur	the TuioCursor to add
	 */
	public void addExternalTuioCursor(TuioCursor tcur) {
		if (tcur == null) return;

		int cursorID = cursorList.size();
		if (cursorID <= maxCursorID) {
			if (!freeCursorList.isEmpty()) {
				TuioCursor closestCursor = null;
				for (TuioCursor iter : freeCursorList)
					if (closestCursor == null || iter.getDistance(tcur.getX(), tcur.getY()) < closestCursor.getDistance(tcur.getX(), tcur.getY()))
						closestCursor = iter;
				cursorID = closestCursor.getCursorID();
				freeCursorList.remove(closestCursor);
			} else {
				cursorID = maxCursorID + 1;
				maxCursorID = cursorID;
			}
		} else {
			maxCursorID = cursorID;
		}

		tcur.setSessionID(++sessionID);
		tcur.setCursorID(cursorID);
		cursorList.add(tcur);
		updateCursor = true;
		notifyAddCursor(tcur);
		if (verbose)
			System.out.println("add cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ") " + tcur.getX() + " " + tcur.getY());
	}

	/**
	 * Updates the referenced TuioCursor based on the given arguments.
	 *
	 * @param	tcur	the TuioCursor to update
	 * @param	xp	the X coordinate
	 * @param	yp	the Y coordinate
	 */
	public void updateTuioCursor(TuioCursor tcur, float xp, float yp) {
		if (tcur == null) return;
		tcur.update(currentFrameTime, xp, yp);
		updateCursor = true;
		if (tcur.isMoving()) {
			notifyUpdateCursor(tcur);
			if (verbose)
				System.out.println("set cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ") " + tcur.getX() + " " + tcur.getY()
						+ " " + tcur.getXSpeed() + " " + tcur.getYSpeed() + " " + tcur.getMotionAccel());
		}
	}

	/**
	 * Updates an externally managed TuioCursor.
	 *
	 * @param	tcur	the TuioCursor to update
	 */
	public void updateExternalTuioCursor(TuioCursor tcur) {
		if (tcur == null) return;
		updateCursor = true;
		if (tcur.isMoving()) {
			notifyUpdateCursor(tcur);
			if (verbose)
				System.out.println("set cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ") " + tcur.getX() + " " + tcur.getY()
						+ " " + tcur.getXSpeed() + " " + tcur.getYSpeed() + " " + tcur.getMotionAccel());
		}
	}

	/**
	 * Removes the referenced TuioCursor from the internal list of TuioCursors.
	 *
	 * @param	tcur	the TuioCursor to remove
	 */
	public void removeTuioCursor(TuioCursor tcur) {
		if (tcur == null) return;
		if (verbose)
			System.out.println("del cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ")");

		cursorList.remove(tcur);
		tcur.remove(currentFrameTime);
		updateCursor = true;
		notifyRemoveCursor(tcur);

		if (tcur.getCursorID() == maxCursorID) {
			maxCursorID = -1;

			if (!cursorList.isEmpty()) {
				for (TuioCursor clist : cursorList) {
					int cursorID = clist.getCursorID();
					if (cursorID > maxCursorID) maxCursorID = cursorID;
				}
				freeCursorBuffer.clear();
				for (TuioCursor freeCursor : freeCursorList)
					if (freeCursor.getCursorID() <= maxCursorID) freeCursorBuffer.add(freeCursor);
				freeCursorList.clear();
				freeCursorList.addAll(freeCursorBuffer);
			} else {
				freeCursorList.clear();
			}
		} else if (tcur.getCursorID() < maxCursorID) {
			freeCursorList.add(tcur);
		}
	}

	/**
	 * Removes an externally managed TuioCursor from the internal list of TuioCursors.
	 *
	 * @param	tcur	the TuioCursor to remove
	 */
	public void removeExternalTuioCursor(TuioCursor tcur) {
		if (tcur == null) return;
		if (verbose)
			System.out.println("del cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ")");
		cursorList.remove(tcur);
		updateCursor = true;
		notifyRemoveCursor(tcur);
	}

	// ---- TuioBlob management ----

	/**
	 * Creates a new TuioBlob based on the given arguments.
	 *
	 * @param	xp	the X coordinate
	 * @param	yp	the Y coordinate
	 * @param	angle	the angle
	 * @param	width	the width
	 * @param	height	the height
	 * @param	area	the area
	 * @return	the created TuioBlob
	 */
	public TuioBlob addTuioBlob(float xp, float yp, float angle, float width, float height, float area) {
		sessionID++;

		int blobID = blobList.size();
		if (blobList.size() <= maxBlobID) {
			if (!freeBlobList.isEmpty()) {
				TuioBlob closestBlob = null;
				for (TuioBlob iter : freeBlobList)
					if (closestBlob == null || iter.getDistance(xp, yp) < closestBlob.getDistance(xp, yp))
						closestBlob = iter;
				blobID = closestBlob.getBlobID();
				freeBlobList.remove(closestBlob);
			} else {
				blobID = maxBlobID + 1;
				maxBlobID = blobID;
			}
		} else {
			maxBlobID = blobID;
		}

		TuioBlob tblb = new TuioBlob(currentFrameTime, sessionID, blobID, xp, yp, angle, width, height, area);
		blobList.add(tblb);
		updateBlob = true;
		notifyAddBlob(tblb);
		if (verbose)
			System.out.println("add blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ") " + tblb.getX() + " " + tblb.getY() + " " + tblb.getAngle() + " " + tblb.getWidth() + " " + tblb.getHeight() + " " + tblb.getArea());
		return tblb;
	}

	/**
	 * Adds an externally managed TuioBlob.
	 *
	 * @param	tblb	the TuioBlob to add
	 */
	public void addExternalTuioBlob(TuioBlob tblb) {
		if (tblb == null) return;

		int blobID = blobList.size();
		if (blobID <= maxBlobID) {
			if (!freeBlobList.isEmpty()) {
				TuioBlob closestBlob = null;
				for (TuioBlob iter : freeBlobList)
					if (closestBlob == null || iter.getDistance(tblb.getX(), tblb.getY()) < closestBlob.getDistance(tblb.getX(), tblb.getY()))
						closestBlob = iter;
				blobID = closestBlob.getBlobID();
				freeBlobList.remove(closestBlob);
			} else {
				blobID = maxBlobID + 1;
				maxBlobID = blobID;
			}
		} else {
			maxBlobID = blobID;
		}

		tblb.setSessionID(++sessionID);
		tblb.setBlobID(blobID);
		blobList.add(tblb);
		updateBlob = true;
		notifyAddBlob(tblb);
		if (verbose)
			System.out.println("add blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ") " + tblb.getX() + " " + tblb.getY() + " " + tblb.getAngle() + " " + tblb.getWidth() + " " + tblb.getHeight() + " " + tblb.getArea());
	}

	/**
	 * Updates the referenced TuioBlob based on the given arguments.
	 *
	 * @param	tblb	the TuioBlob to update
	 * @param	xp	the X coordinate
	 * @param	yp	the Y coordinate
	 * @param	angle	the angle
	 * @param	width	the width
	 * @param	height	the height
	 * @param	area	the area
	 */
	public void updateTuioBlob(TuioBlob tblb, float xp, float yp, float angle, float width, float height, float area) {
		if (tblb == null) return;
		if (tblb.getTuioTime().equals(currentFrameTime)) return;
		tblb.update(currentFrameTime, xp, yp, angle, width, height, area);
		updateBlob = true;
		if (tblb.isMoving()) {
			notifyUpdateBlob(tblb);
			if (verbose)
				System.out.println("set blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ") " + tblb.getX() + " " + tblb.getY() + " " + tblb.getAngle() + " " + tblb.getWidth() + " " + tblb.getHeight() + " " + tblb.getArea()
						+ " " + tblb.getXSpeed() + " " + tblb.getYSpeed() + " " + tblb.getRotationSpeed() + " " + tblb.getMotionAccel() + " " + tblb.getRotationAccel());
		}
	}

	/**
	 * Updates an externally managed TuioBlob.
	 *
	 * @param	tblb	the TuioBlob to update
	 */
	public void updateExternalTuioBlob(TuioBlob tblb) {
		if (tblb == null) return;
		updateBlob = true;
		if (tblb.isMoving()) {
			notifyUpdateBlob(tblb);
			if (verbose)
				System.out.println("set blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ") " + tblb.getX() + " " + tblb.getY() + " " + tblb.getAngle() + " " + tblb.getWidth() + " " + tblb.getHeight() + " " + tblb.getArea()
						+ " " + tblb.getXSpeed() + " " + tblb.getYSpeed() + " " + tblb.getRotationSpeed() + " " + tblb.getMotionAccel() + " " + tblb.getRotationAccel());
		}
	}

	/**
	 * Removes the referenced TuioBlob from the internal list of TuioBlobs.
	 *
	 * @param	tblb	the TuioBlob to remove
	 */
	public void removeTuioBlob(TuioBlob tblb) {
		if (tblb == null) return;
		if (verbose)
			System.out.println("del blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ")");

		blobList.remove(tblb);
		tblb.remove(currentFrameTime);
		updateBlob = true;
		notifyRemoveBlob(tblb);

		if (tblb.getBlobID() == maxBlobID) {
			maxBlobID = -1;

			if (!blobList.isEmpty()) {
				for (TuioBlob clist : blobList) {
					int blobID = clist.getBlobID();
					if (blobID > maxBlobID) maxBlobID = blobID;
				}
				freeBlobBuffer.clear();
				for (TuioBlob freeBlob : freeBlobList)
					if (freeBlob.getBlobID() <= maxBlobID) freeBlobBuffer.add(freeBlob);
				freeBlobList.clear();
				freeBlobList.addAll(freeBlobBuffer);
			} else {
				freeBlobList.clear();
			}
		} else if (tblb.getBlobID() < maxBlobID) {
			freeBlobList.add(tblb);
		}
	}

	/**
	 * Removes an externally managed TuioBlob from the internal list of TuioBlobs.
	 *
	 * @param	tblb	the TuioBlob to remove
	 */
	public void removeExternalTuioBlob(TuioBlob tblb) {
		if (tblb == null) return;
		if (verbose)
			System.out.println("del blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ")");
		blobList.remove(tblb);
		updateBlob = true;
		notifyRemoveBlob(tblb);
	}

	// ---- frame handling ----

	/**
	 * Returns the next session ID.
	 *
	 * @return	the next session ID
	 */
	public int getSessionID() {
		return ++sessionID;
	}

	/**
	 * Returns the current frame ID.
	 *
	 * @return	the current frame ID
	 */
	public int getFrameID() {
		return currentFrame;
	}

	/**
	 * Returns the current frame time.
	 *
	 * @return	the current frame time
	 */
	public TuioTime getFrameTime() {
		return currentFrameTime;
	}

	/**
	 * Initializes a new frame with the provided time stamp.
	 *
	 * @param	ttime	the frame time
	 */
	public void initFrame(TuioTime ttime) {
		currentFrameTime = new TuioTime(ttime);
		currentFrame++;
	}

	/**
	 * Commits the current frame, calling refresh() on all listeners.
	 */
	public void commitFrame() {
		for (TuioListener listener : listenerList)
			listener.refresh(currentFrameTime);
	}

	// ---- closest/untouched helpers ----

	/**
	 * Returns the TuioObject closest to the provided coordinates or NULL.
	 *
	 * @param	xp	the X coordinate
	 * @param	yp	the Y coordinate
	 * @return	the closest TuioObject or null
	 */
	public TuioObject getClosestTuioObject(float xp, float yp) {
		TuioObject closest = null;
		float closestDistance = 1.0f;
		for (TuioObject tobj : objectList) {
			float distance = tobj.getDistance(xp, yp);
			if (distance < closestDistance) {
				closest = tobj;
				closestDistance = distance;
			}
		}
		return closest;
	}

	/**
	 * Returns the TuioCursor closest to the provided coordinates or NULL.
	 *
	 * @param	xp	the X coordinate
	 * @param	yp	the Y coordinate
	 * @return	the closest TuioCursor or null
	 */
	public TuioCursor getClosestTuioCursor(float xp, float yp) {
		TuioCursor closest = null;
		float closestDistance = 1.0f;
		for (TuioCursor tcur : cursorList) {
			float distance = tcur.getDistance(xp, yp);
			if (distance < closestDistance) {
				closest = tcur;
				closestDistance = distance;
			}
		}
		return closest;
	}

	/**
	 * Returns the TuioBlob closest to the provided coordinates or NULL.
	 *
	 * @param	xp	the X coordinate
	 * @param	yp	the Y coordinate
	 * @return	the closest TuioBlob or null
	 */
	public TuioBlob getClosestTuioBlob(float xp, float yp) {
		TuioBlob closest = null;
		float closestDistance = 1.0f;
		for (TuioBlob tblb : blobList) {
			float distance = tblb.getDistance(xp, yp);
			if (distance < closestDistance) {
				closest = tblb;
				closestDistance = distance;
			}
		}
		return closest;
	}

	/**
	 * Returns a List of all currently inactive TuioObjects.
	 *
	 * @return	a List of all currently inactive TuioObjects
	 */
	public List<TuioObject> getUntouchedObjects() {
		List<TuioObject> untouched = new ArrayList<TuioObject>();
		for (TuioObject tobj : objectList)
			if (!tobj.getTuioTime().equals(currentFrameTime)) untouched.add(tobj);
		return untouched;
	}

	/**
	 * Calculates speed and acceleration values for all currently inactive TuioObjects.
	 */
	public void stopUntouchedMovingObjects() {
		for (TuioObject tobj : objectList) {
			if (!tobj.getTuioTime().equals(currentFrameTime) && tobj.isMoving()) {
				tobj.stop(currentFrameTime);
				updateObject = true;
				if (verbose)
					System.out.println("set obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ") " + tobj.getX() + " " + tobj.getY() + " " + tobj.getAngle());
			}
		}
	}

	/**
	 * Removes all currently inactive TuioObjects from the internal list of TuioObjects.
	 */
	public void removeUntouchedStoppedObjects() {
		List<TuioObject> copy = new ArrayList<TuioObject>(objectList);
		for (TuioObject tobj : copy) {
			if (!tobj.getTuioTime().equals(currentFrameTime) && !tobj.isMoving())
				removeTuioObject(tobj);
		}
	}

	/**
	 * Removes all TuioObjects from the internal list of TuioObjects.
	 */
	public void resetTuioObjects() {
		List<TuioObject> copy = new ArrayList<TuioObject>(objectList);
		for (TuioObject tobj : copy) removeTuioObject(tobj);
	}

	/**
	 * Returns a List of all currently inactive TuioCursors.
	 *
	 * @return	a List of all currently inactive TuioCursors
	 */
	public List<TuioCursor> getUntouchedCursors() {
		List<TuioCursor> untouched = new ArrayList<TuioCursor>();
		for (TuioCursor tcur : cursorList)
			if (!tcur.getTuioTime().equals(currentFrameTime)) untouched.add(tcur);
		return untouched;
	}

	/**
	 * Calculates speed and acceleration values for all currently inactive TuioCursors.
	 */
	public void stopUntouchedMovingCursors() {
		for (TuioCursor tcur : cursorList) {
			if (!tcur.getTuioTime().equals(currentFrameTime) && tcur.isMoving()) {
				tcur.stop(currentFrameTime);
				updateCursor = true;
				if (verbose)
					System.out.println("set cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ") " + tcur.getX() + " " + tcur.getY());
			}
		}
	}

	/**
	 * Removes all currently inactive TuioCursors from the internal list of TuioCursors.
	 */
	public void removeUntouchedStoppedCursors() {
		if (cursorList.isEmpty()) return;
		List<TuioCursor> copy = new ArrayList<TuioCursor>(cursorList);
		for (TuioCursor tcur : copy) {
			if (!tcur.getTuioTime().equals(currentFrameTime) && !tcur.isMoving())
				removeTuioCursor(tcur);
		}
	}

	/**
	 * Removes all TuioCursors from the internal list of TuioCursors.
	 */
	public void resetTuioCursors() {
		List<TuioCursor> copy = new ArrayList<TuioCursor>(cursorList);
		for (TuioCursor tcur : copy) removeTuioCursor(tcur);
	}

	/**
	 * Returns a List of all currently inactive TuioBlobs.
	 *
	 * @return	a List of all currently inactive TuioBlobs
	 */
	public List<TuioBlob> getUntouchedBlobs() {
		List<TuioBlob> untouched = new ArrayList<TuioBlob>();
		for (TuioBlob tblb : blobList)
			if (!tblb.getTuioTime().equals(currentFrameTime)) untouched.add(tblb);
		return untouched;
	}

	/**
	 * Calculates speed and acceleration values for all currently inactive TuioBlobs.
	 */
	public void stopUntouchedMovingBlobs() {
		for (TuioBlob tblb : blobList) {
			if (!tblb.getTuioTime().equals(currentFrameTime) && tblb.isMoving()) {
				tblb.stop(currentFrameTime);
				updateBlob = true;
				if (verbose)
					System.out.println("set blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ") " + tblb.getX() + " " + tblb.getY());
			}
		}
	}

	/**
	 * Removes all currently inactive TuioBlobs from the internal list of TuioBlobs.
	 */
	public void removeUntouchedStoppedBlobs() {
		List<TuioBlob> copy = new ArrayList<TuioBlob>(blobList);
		for (TuioBlob tblb : copy) {
			if (!tblb.getTuioTime().equals(currentFrameTime) && !tblb.isMoving())
				removeTuioBlob(tblb);
		}
	}

	/**
	 * Removes all TuioBlobs from the internal list of TuioBlobs.
	 */
	public void resetTuioBlobs() {
		List<TuioBlob> copy = new ArrayList<TuioBlob>(blobList);
		for (TuioBlob tblb : copy) removeTuioBlob(tblb);
	}

	// ---- configuration ----

	/**
	 * Sets verbose output.
	 *
	 * @param	verbose	print verbose messages if set to true
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Returns true if verbose output is enabled.
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * Inverts the X, Y and angle values.
	 *
	 * @param	ix	invert the X coordinate
	 * @param	iy	invert the Y coordinate
	 * @param	ia	invert the angle
	 */
	public void setInversion(boolean ix, boolean iy, boolean ia) {
		invert_x = ix;
		invert_y = iy;
		invert_a = ia;
	}

	public void setInvertXpos(boolean ix) {
		invert_x = ix;
	}

	public void setInvertYpos(boolean iy) {
		invert_y = iy;
	}

	public void setInvertAngle(boolean ia) {
		invert_a = ia;
	}

	public boolean getInvertXpos() {
		return invert_x;
	}

	public boolean getInvertYpos() {
		return invert_y;
	}

	public boolean getInvertAngle() {
		return invert_a;
	}

	public boolean isUpdateObject() {
		return updateObject;
	}

	public boolean isUpdateCursor() {
		return updateCursor;
	}

	public boolean isUpdateBlob() {
		return updateBlob;
	}

	// ---- listener notification helpers ----

	private void notifyAddObject(TuioObject tobj) {
		for (TuioListener listener : listenerList) listener.addTuioObject(tobj);
	}

	private void notifyUpdateObject(TuioObject tobj) {
		for (TuioListener listener : listenerList) listener.updateTuioObject(tobj);
	}

	private void notifyRemoveObject(TuioObject tobj) {
		for (TuioListener listener : listenerList) listener.removeTuioObject(tobj);
	}

	private void notifyAddCursor(TuioCursor tcur) {
		for (TuioListener listener : listenerList) listener.addTuioCursor(tcur);
	}

	private void notifyUpdateCursor(TuioCursor tcur) {
		for (TuioListener listener : listenerList) listener.updateTuioCursor(tcur);
	}

	private void notifyRemoveCursor(TuioCursor tcur) {
		for (TuioListener listener : listenerList) listener.removeTuioCursor(tcur);
	}

	private void notifyAddBlob(TuioBlob tblb) {
		for (TuioListener listener : listenerList) listener.addTuioBlob(tblb);
	}

	private void notifyUpdateBlob(TuioBlob tblb) {
		for (TuioListener listener : listenerList) listener.updateTuioBlob(tblb);
	}

	private void notifyRemoveBlob(TuioBlob tblb) {
		for (TuioListener listener : listenerList) listener.removeTuioBlob(tblb);
	}
}
