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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import TUIO.osc.OscMessage;

/**
 * The TuioClient class provides the main interface to a running
 * TUIO server. It is a port of the modernized C++ TuioClient with
 * support for multiple TUIO sources, UDP and TCP transports and
 * the full set of C++ client-side fixes.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public class TuioClient extends TuioDispatcher {

	private final List<TuioObject> frameObjects = new LinkedList<TuioObject>();
	private final List<Integer> aliveObjectList = new LinkedList<Integer>();
	private final List<TuioCursor> frameCursors = new LinkedList<TuioCursor>();
	private final List<Integer> aliveCursorList = new LinkedList<Integer>();
	private final List<TuioBlob> frameBlobs = new LinkedList<TuioBlob>();
	private final List<Integer> aliveBlobList = new LinkedList<Integer>();

	private int currentFrame = -1;
	private TuioTime currentTime = new TuioTime();

	private final List<TuioCursor> freeCursorList = new LinkedList<TuioCursor>();
	private final List<TuioCursor> freeCursorBuffer = new LinkedList<TuioCursor>();
	private final Map<Integer,Integer> maxCursorID = new HashMap<Integer,Integer>();

	private final List<TuioBlob> freeBlobList = new LinkedList<TuioBlob>();
	private final List<TuioBlob> freeBlobBuffer = new LinkedList<TuioBlob>();
	private final Map<Integer,Integer> maxBlobID = new HashMap<Integer,Integer>();

	private final Map<String,Integer> sourceList = new HashMap<String,Integer>();
	private int source_id = 0;
	private String source_name = "";
	private String source_addr = "";

	private final OscReceiver receiver;

	/**
	 * The default constructor creates a TuioClient listening for TUIO
	 * messages via UDP on the default port 3333.
	 */
	public TuioClient() {
		this(3333);
	}

	/**
	 * This constructor creates a TuioClient listening for TUIO messages
	 * via UDP on the provided port.
	 *
	 * @param	port	the UDP port to listen to
	 */
	public TuioClient(int port) {
		this(new UdpReceiver(port));
	}

	/**
	 * This constructor creates a TuioClient using the provided OscReceiver
	 * (UDP or TCP).
	 *
	 * @param	oscreceiver	the OscReceiver to use
	 */
	public TuioClient(OscReceiver oscreceiver) {
		receiver = oscreceiver;
		initialize();
	}

	private void initialize() {
		receiver.addTuioClient(this);
		maxCursorID.put(source_id, -1);
		maxBlobID.put(source_id, -1);
	}

	/**
	 * The TuioClient connects and starts receiving TUIO messages.
	 * The receiver runs in a dedicated background thread.
	 */
	public void connect() {
		connect(false);
	}

	/**
	 * The TuioClient connects and starts receiving TUIO messages.
	 * With the default parameter the OSC receiver runs in a dedicated background thread,
	 * when set to true the method blocks in the current thread until disconnect() is invoked.
	 *
	 * @param	lock	blocks in the current thread if set to true, runs in the background if set to false (default)
	 */
	public void connect(boolean lock) {
		TuioTime.initSession();
		currentTime.reset();
		currentFrame = -1;
		receiver.connect(lock);
	}

	/**
	 * The TuioClient disconnects and stops receiving TUIO messages.
	 */
	public void disconnect() {
		receiver.disconnect();

		aliveObjectList.clear();
		aliveCursorList.clear();
		aliveBlobList.clear();
		processBundleBegin();

		objectLock.lock();
		objectList.clear();
		objectLock.unlock();
		cursorLock.lock();
		cursorList.clear();
		cursorLock.unlock();
		blobLock.lock();
		blobList.clear();
		blobLock.unlock();

		freeCursorList.clear();
		freeCursorBuffer.clear();
		maxCursorID.clear();
		freeBlobList.clear();
		freeBlobBuffer.clear();
		maxBlobID.clear();

		sourceList.clear();
		source_id = 0;
		source_name = "";
		source_addr = "";
	}

	/**
	 * Returns true if the TuioClient is currently connected.
	 *
	 * @return	true if the TuioClient is currently connected
	 */
	public boolean isConnected() {
		return receiver.isConnected();
	}

	// ---- source-filtered accessors (C++ API) ----

	/**
	 * Returns a List of all currently active TuioObjects of the provided source
	 *
	 * @param	source_id	the source ID
	 * @return	a List of all currently active TuioObjects of the source
	 */
	public List<TuioObject> getTuioObjects(int source_id) {
		objectLock.lock();
		try {
			List<TuioObject> buffer = new ArrayList<TuioObject>();
			for (TuioObject tobj : objectList)
				if (tobj.getTuioSourceID() == source_id) buffer.add(tobj);
			return buffer;
		} finally {
			objectLock.unlock();
		}
	}

	/**
	 * Returns a List with a copy of the currently active TuioObjects of the provided source
	 *
	 * @param	source_id	the source ID
	 * @return	a List with a copy of the currently active TuioObjects of the source
	 */
	public List<TuioObject> copyTuioObjects(int source_id) {
		objectLock.lock();
		try {
			List<TuioObject> buffer = new ArrayList<TuioObject>();
			for (TuioObject tobj : objectList)
				if (tobj.getTuioSourceID() == source_id) buffer.add(new TuioObject(tobj));
			return buffer;
		} finally {
			objectLock.unlock();
		}
	}

	/**
	 * Returns the TuioObject corresponding to the provided Session ID
	 * or NULL if the Session ID does not refer to an active TuioObject
	 *
	 * @param	s_id	the Session ID of the required TuioObject
	 * @return	the TuioObject or null
	 */
	public TuioObject getTuioObject(long s_id) {
		return getTuioObject(0, s_id);
	}

	/**
	 * Returns the TuioObject corresponding to the provided source and Session ID
	 * or NULL if the Session ID does not refer to an active TuioObject
	 *
	 * @param	src_id	the source ID
	 * @param	s_id	the Session ID of the required TuioObject
	 * @return	the TuioObject or null
	 */
	public TuioObject getTuioObject(int src_id, long s_id) {
		objectLock.lock();
		try {
			for (TuioObject tobj : objectList)
				if (tobj.getTuioSourceID() == src_id && tobj.getSessionID() == s_id)
					return tobj;
			return null;
		} finally {
			objectLock.unlock();
		}
	}

	public List<TuioCursor> getTuioCursors(int source_id) {
		cursorLock.lock();
		try {
			List<TuioCursor> buffer = new ArrayList<TuioCursor>();
			for (TuioCursor tcur : cursorList)
				if (tcur.getTuioSourceID() == source_id) buffer.add(tcur);
			return buffer;
		} finally {
			cursorLock.unlock();
		}
	}

	public List<TuioCursor> copyTuioCursors(int source_id) {
		cursorLock.lock();
		try {
			List<TuioCursor> buffer = new ArrayList<TuioCursor>();
			for (TuioCursor tcur : cursorList)
				if (tcur.getTuioSourceID() == source_id) buffer.add(new TuioCursor(tcur));
			return buffer;
		} finally {
			cursorLock.unlock();
		}
	}

	public TuioCursor getTuioCursor(long s_id) {
		return getTuioCursor(0, s_id);
	}

	public TuioCursor getTuioCursor(int src_id, long s_id) {
		cursorLock.lock();
		try {
			for (TuioCursor tcur : cursorList)
				if (tcur.getTuioSourceID() == src_id && tcur.getSessionID() == s_id)
					return tcur;
			return null;
		} finally {
			cursorLock.unlock();
		}
	}

	public List<TuioBlob> getTuioBlobs(int source_id) {
		blobLock.lock();
		try {
			List<TuioBlob> buffer = new ArrayList<TuioBlob>();
			for (TuioBlob tblb : blobList)
				if (tblb.getTuioSourceID() == source_id) buffer.add(tblb);
			return buffer;
		} finally {
			blobLock.unlock();
		}
	}

	public List<TuioBlob> copyTuioBlobs(int source_id) {
		blobLock.lock();
		try {
			List<TuioBlob> buffer = new ArrayList<TuioBlob>();
			for (TuioBlob tblb : blobList)
				if (tblb.getTuioSourceID() == source_id) buffer.add(new TuioBlob(tblb));
			return buffer;
		} finally {
			blobLock.unlock();
		}
	}

	public TuioBlob getTuioBlob(long s_id) {
		return getTuioBlob(0, s_id);
	}

	public TuioBlob getTuioBlob(int src_id, long s_id) {
		blobLock.lock();
		try {
			for (TuioBlob tblb : blobList)
				if (tblb.getTuioSourceID() == src_id && tblb.getSessionID() == s_id)
					return tblb;
			return null;
		} finally {
			blobLock.unlock();
		}
	}

	// ---- deprecated legacy accessors (kept for backward compatibility) ----

	/**
	 * Returns a Vector of all currently active TuioObjects
	 *
	 * @return	a Vector of all currently active TuioObjects
	 * @deprecated use {@link #getTuioObjects()} instead.
	 */
	@Deprecated
	public Vector<TuioObject> getTuioObjects() {
		return new Vector<TuioObject>(super.getTuioObjects());
	}

	/**
	 * Returns an ArrayList of all currently active TuioObjects
	 *
	 * @return	a ArrayList of all currently active TuioObjects
	 */
	public ArrayList<TuioObject> getTuioObjectList() {
		return new ArrayList<TuioObject>(super.getTuioObjects());
	}

	/**
	 * Returns a Vector of all currently active TuioCursors
	 *
	 * @return	a Vector of all currently active TuioCursors
	 * @deprecated use {@link #getTuioCursors()} instead.
	 */
	@Deprecated
	public Vector<TuioCursor> getTuioCursors() {
		return new Vector<TuioCursor>(super.getTuioCursors());
	}

	/**
	 * Returns an ArrayList of all currently active TuioCursors
	 *
	 * @return	a ArrayList of all currently active TuioCursors
	 */
	public ArrayList<TuioCursor> getTuioCursorList() {
		return new ArrayList<TuioCursor>(super.getTuioCursors());
	}

	/**
	 * Returns a Vector of all currently active TuioBlobs
	 *
	 * @return	a Vector of all currently active TuioBlobs
	 * @deprecated use {@link #getTuioBlobs()} instead.
	 */
	@Deprecated
	public Vector<TuioBlob> getTuioBlobs() {
		return new Vector<TuioBlob>(super.getTuioBlobs());
	}

	/**
	 * Returns an ArrayList of all currently active TuioBlobs
	 *
	 * @return	a ArrayList of all currently active TuioBlobs
	 */
	public ArrayList<TuioBlob> getTuioBlobList() {
		return new ArrayList<TuioBlob>(super.getTuioBlobs());
	}

	// ---- OSC message processing ----

	/**
	 * Resets all pending frame state. Called by the OscReceiver at the
	 * beginning of each received OSC packet.
	 */
	public void processBundleBegin() {
		frameObjects.clear();
		frameCursors.clear();
		frameBlobs.clear();
	}

	/**
	 * The OSC message callback where all TUIO messages are received,
	 * decoded and the TUIO event callbacks are dispatched.
	 *
	 * @param	message	the received OSC message
	 */
	public void processOSC(OscMessage message) {
		try {
			String address = message.getAddress();
			List<Object> args = message.getArguments();

			if ("/tuio/2Dobj".equals(address)) {
				processObjectMessage(args);
			} else if ("/tuio/2Dcur".equals(address)) {
				processCursorMessage(args);
			} else if ("/tuio/2Dblb".equals(address)) {
				processBlobMessage(args);
			}
		} catch (RuntimeException e) {
			System.err.println("error parsing TUIO message: " + message.getAddress() + " - " + e.getMessage());
		}
	}

	// ---- argument helpers ----

	private static String argString(List<Object> args, int index) {
		Object arg = args.get(index);
		return arg == null ? "" : String.valueOf(arg);
	}

	private static int argInt(List<Object> args, int index) {
		return ((Number) args.get(index)).intValue();
	}

	private static float argFloat(List<Object> args, int index) {
		return ((Number) args.get(index)).floatValue();
	}

	private int parseSource(List<Object> args) {
		String source_str = argString(args, 1);
		Integer found = sourceList.get(source_str);
		if (found == null) {
			source_id = sourceList.size();
			sourceList.put(source_str, source_id);
			maxCursorID.put(source_id, -1);
			maxBlobID.put(source_id, -1);
		} else {
			source_id = found;
		}

		int atpos = source_str.indexOf('@');
		if (atpos >= 0) {
			source_name = source_str.substring(0, atpos);
			source_addr = source_str.substring(atpos + 1);
			if (source_addr.isEmpty()) source_addr = "localhost";
		} else {
			source_name = source_str;
			source_addr = "localhost";
		}
		return source_id;
	}

	private void notifyObjects(java.util.function.Consumer<TuioListener> callback) {
		for (TuioListener listener : listenerList) {
			try { callback.accept(listener); } catch (RuntimeException e) {}
		}
	}

	private void notifyCursors(java.util.function.Consumer<TuioListener> callback) {
		for (TuioListener listener : listenerList) {
			try { callback.accept(listener); } catch (RuntimeException e) {}
		}
	}

	private void notifyBlobs(java.util.function.Consumer<TuioListener> callback) {
		for (TuioListener listener : listenerList) {
			try { callback.accept(listener); } catch (RuntimeException e) {}
		}
	}

	private void notifyRefresh() {
		for (TuioListener listener : listenerList) {
			try { listener.refresh(currentTime); } catch (RuntimeException e) {}
		}
	}

	// ---- /tuio/2Dobj ----

	private void processObjectMessage(List<Object> args) {
		String cmd = argString(args, 0);

		if ("source".equals(cmd)) {
			parseSource(args);
		} else if ("set".equals(cmd)) {
			int s_id = argInt(args, 1);
			int c_id = argInt(args, 2);
			float xpos = argFloat(args, 3);
			float ypos = argFloat(args, 4);
			float angle = argFloat(args, 5);
			float xspeed = argFloat(args, 6);
			float yspeed = argFloat(args, 7);
			float rspeed = argFloat(args, 8);
			float maccel = argFloat(args, 9);
			float raccel = argFloat(args, 10);

			objectLock.lock();
			try {
				TuioObject tobj = null;
				for (TuioObject iter : objectList)
					if (iter.getSessionID() == s_id && iter.getTuioSourceID() == source_id) { tobj = iter; break; }

				if (tobj == null) {
					frameObjects.add(new TuioObject(s_id, c_id, xpos, ypos, angle));
				} else if (tobj.getX() != xpos || tobj.getY() != ypos || tobj.getAngle() != angle
						|| tobj.getXSpeed() != xspeed || tobj.getYSpeed() != yspeed
						|| tobj.getRotationSpeed() != rspeed || tobj.getMotionAccel() != maccel
						|| tobj.getRotationAccel() != raccel) {
					TuioObject updateObject = new TuioObject(s_id, tobj.getSymbolID(), xpos, ypos, angle);
					updateObject.update(xpos, ypos, angle, xspeed, yspeed, rspeed, maccel, raccel);
					frameObjects.add(updateObject);
				}
			} finally {
				objectLock.unlock();
			}
		} else if ("alive".equals(cmd)) {
			aliveObjectList.clear();
			for (int i = 1; i < args.size(); i++)
				aliveObjectList.add(argInt(args, i));
		} else if ("fseq".equals(cmd)) {
			processObjectFseq(argInt(args, 1));
		}
	}

	private void processObjectFseq(int fseq) {
		boolean lateFrame = false;
		if (fseq > 0) {
			if (fseq > currentFrame) currentTime = TuioTime.getSessionTime();
			if (fseq >= currentFrame || (currentFrame - fseq) > 100) currentFrame = fseq;
			else lateFrame = true;
		} else if (TuioTime.getSessionTime().getTotalMilliseconds() - currentTime.getTotalMilliseconds() > 100) {
			currentTime = TuioTime.getSessionTime();
		}

		if (lateFrame) {
			frameObjects.clear();
			return;
		}

		// find the removed objects first
		objectLock.lock();
		try {
			for (TuioObject tobj : objectList) {
				if (tobj.getTuioSourceID() == source_id
						&& !aliveObjectList.contains((int) tobj.getSessionID())) {
					tobj.remove(currentTime);
					frameObjects.add(new TuioObject(tobj));
				}
			}
		} finally {
			objectLock.unlock();
		}

		for (TuioObject tobj : frameObjects) {
			switch (tobj.getTuioState()) {
			case TuioContainer.TUIO_REMOVED: {
				TuioObject frameObject = tobj;
				frameObject.remove(currentTime);
				notifyObjects(l -> l.removeTuioObject(frameObject));

				objectLock.lock();
				try {
					objectList.removeIf(o -> o.getTuioSourceID() == source_id
							&& o.getSessionID() == frameObject.getSessionID());
				} finally {
					objectLock.unlock();
				}
				break;
			}
			case TuioContainer.TUIO_ADDED: {
				TuioObject frameObject;
				objectLock.lock();
				try {
					frameObject = new TuioObject(currentTime, tobj.getSessionID(),
							tobj.getSymbolID(), tobj.getX(), tobj.getY(), tobj.getAngle());
					if (!source_name.isEmpty())
						frameObject.setTuioSource(source_id, source_name, source_addr);
					objectList.add(frameObject);
				} finally {
					objectLock.unlock();
				}
				notifyObjects(l -> l.addTuioObject(frameObject));
				break;
			}
			default: {
				objectLock.lock();
				TuioObject frameObject = null;
				try {
					for (TuioObject iter : objectList) {
						if (iter.getTuioSourceID() == source_id
								&& iter.getSessionID() == tobj.getSessionID()) {
							frameObject = iter;
							break;
						}
					}
					if (frameObject == null || frameObject.getTuioState() == TuioContainer.TUIO_REMOVED) {
						// object was removed meanwhile
					} else if (frameObject == tobj) {
						// already handled
					} else {
						if ((tobj.getX() != frameObject.getX() && tobj.getXSpeed() == 0)
								|| (tobj.getY() != frameObject.getY() && tobj.getYSpeed() == 0))
							frameObject.update(currentTime, tobj.getX(), tobj.getY(), tobj.getAngle());
						else
							frameObject.update(currentTime, tobj.getX(), tobj.getY(), tobj.getAngle(),
									tobj.getXSpeed(), tobj.getYSpeed(), tobj.getRotationSpeed(),
									tobj.getMotionAccel(), tobj.getRotationAccel());
					}
				} finally {
					objectLock.unlock();
				}
				if (frameObject != null && frameObject != tobj
						&& frameObject.getTuioState() != TuioContainer.TUIO_REMOVED) {
					final TuioObject fo = frameObject;
					notifyObjects(l -> l.updateTuioObject(fo));
				}
				break;
			}
			}
		}

		notifyRefresh();
		frameObjects.clear();
	}

	// ---- /tuio/2Dcur ----

	private void processCursorMessage(List<Object> args) {
		String cmd = argString(args, 0);

		if ("source".equals(cmd)) {
			parseSource(args);
		} else if ("set".equals(cmd)) {
			int s_id = argInt(args, 1);
			float xpos = argFloat(args, 2);
			float ypos = argFloat(args, 3);
			float xspeed = argFloat(args, 4);
			float yspeed = argFloat(args, 5);
			float maccel = argFloat(args, 6);

			cursorLock.lock();
			try {
				TuioCursor tcur = null;
				for (TuioCursor iter : cursorList)
					if (iter.getSessionID() == s_id && iter.getTuioSourceID() == source_id) { tcur = iter; break; }

				if (tcur == null) {
					frameCursors.add(new TuioCursor(s_id, -1, xpos, ypos));
				} else if (tcur.getX() != xpos || tcur.getY() != ypos
						|| tcur.getXSpeed() != xspeed || tcur.getYSpeed() != yspeed
						|| tcur.getMotionAccel() != maccel) {
					TuioCursor updateCursor = new TuioCursor(s_id, tcur.getCursorID(), xpos, ypos);
					updateCursor.update(xpos, ypos, xspeed, yspeed, maccel);
					frameCursors.add(updateCursor);
				}
			} finally {
				cursorLock.unlock();
			}
		} else if ("alive".equals(cmd)) {
			aliveCursorList.clear();
			for (int i = 1; i < args.size(); i++)
				aliveCursorList.add(argInt(args, i));
		} else if ("fseq".equals(cmd)) {
			processCursorFseq(argInt(args, 1));
		}
	}

	private void processCursorFseq(int fseq) {
		boolean lateFrame = false;
		if (fseq > 0) {
			if (fseq > currentFrame) currentTime = TuioTime.getSessionTime();
			if (fseq >= currentFrame || (currentFrame - fseq) > 100) currentFrame = fseq;
			else lateFrame = true;
		} else if (TuioTime.getSessionTime().getTotalMilliseconds() - currentTime.getTotalMilliseconds() > 100) {
			currentTime = TuioTime.getSessionTime();
		}

		if (lateFrame) {
			frameCursors.clear();
			return;
		}

		cursorLock.lock();
		try {
			for (TuioCursor tcur : cursorList) {
				if (tcur.getTuioSourceID() == source_id
						&& !aliveCursorList.contains((int) tcur.getSessionID())) {
					tcur.remove(currentTime);
					frameCursors.add(new TuioCursor(tcur));
				}
			}
		} finally {
			cursorLock.unlock();
		}

		for (TuioCursor tcur : frameCursors) {
			switch (tcur.getTuioState()) {
			case TuioContainer.TUIO_REMOVED: {
				TuioCursor frameCursor = tcur;
				frameCursor.remove(currentTime);
				notifyCursors(l -> l.removeTuioCursor(frameCursor));

				cursorLock.lock();
				try {
					cursorList.removeIf(c -> c.getTuioSourceID() == source_id
							&& c.getSessionID() == frameCursor.getSessionID());

					if (frameCursor.getCursorID() == maxCursorID.get(source_id)) {
						maxCursorID.put(source_id, -1);

						if (!cursorList.isEmpty()) {
							int c_id = 0;
							for (TuioCursor clist : cursorList) {
								if (clist.getTuioSourceID() == source_id) {
									c_id = clist.getCursorID();
									if (c_id > maxCursorID.get(source_id)) maxCursorID.put(source_id, c_id);
								}
							}
							freeCursorBuffer.clear();
							for (TuioCursor freeCursor : freeCursorList) {
								if (freeCursor.getTuioSourceID() == source_id) {
									if (freeCursor.getCursorID() > maxCursorID.get(source_id)) continue; // dropped
									freeCursorBuffer.add(freeCursor);
								} else {
									freeCursorBuffer.add(freeCursor);
								}
							}
							freeCursorList.clear();
							freeCursorList.addAll(freeCursorBuffer);
						} else {
							freeCursorBuffer.clear();
							for (TuioCursor freeCursor : freeCursorList) {
								if (freeCursor.getTuioSourceID() != source_id)
									freeCursorBuffer.add(freeCursor);
							}
							freeCursorList.clear();
							freeCursorList.addAll(freeCursorBuffer);
						}
					} else if (frameCursor.getCursorID() < maxCursorID.get(source_id)) {
						freeCursorList.add(frameCursor);
					}
				} finally {
					cursorLock.unlock();
				}
				break;
			}
			case TuioContainer.TUIO_ADDED: {
				int c_id = 0;
				int free_size = 0;
				TuioCursor frameCursor;
				cursorLock.lock();
				try {
					for (TuioCursor iter : cursorList)
						if (iter.getTuioSourceID() == source_id) c_id++;
					for (TuioCursor iter : freeCursorList)
						if (iter.getTuioSourceID() == source_id) free_size++;

					if (free_size <= maxCursorID.get(source_id) && free_size > 0) {
						TuioCursor closestCursor = null;
						for (TuioCursor iter : freeCursorList) {
							if (iter.getTuioSourceID() == source_id
									&& (closestCursor == null
									|| iter.getDistance(tcur) < closestCursor.getDistance(tcur)))
								closestCursor = iter;
						}
						if (closestCursor != null) {
							c_id = closestCursor.getCursorID();
							freeCursorList.remove(closestCursor);
						}
					} else {
						maxCursorID.put(source_id, c_id);
					}

					frameCursor = new TuioCursor(currentTime, tcur.getSessionID(),
							c_id, tcur.getX(), tcur.getY());
					if (!source_name.isEmpty())
						frameCursor.setTuioSource(source_id, source_name, source_addr);
					cursorList.add(frameCursor);
				} finally {
					cursorLock.unlock();
				}

				notifyCursors(l -> l.addTuioCursor(frameCursor));
				break;
			}
			default: {
				cursorLock.lock();
				TuioCursor frameCursor = null;
				boolean valid = false;
				try {
					for (TuioCursor iter : cursorList) {
						if (iter.getTuioSourceID() == source_id
								&& iter.getSessionID() == tcur.getSessionID()) {
							frameCursor = iter;
							break;
						}
					}
					if (frameCursor != null && frameCursor != tcur
							&& frameCursor.getTuioState() != TuioContainer.TUIO_REMOVED) {
						valid = true;
						if ((tcur.getX() != frameCursor.getX() && tcur.getXSpeed() == 0)
								|| (tcur.getY() != frameCursor.getY() && tcur.getYSpeed() == 0))
							frameCursor.update(currentTime, tcur.getX(), tcur.getY());
						else
							frameCursor.update(currentTime, tcur.getX(), tcur.getY(),
									tcur.getXSpeed(), tcur.getYSpeed(), tcur.getMotionAccel());
					}
				} finally {
					cursorLock.unlock();
				}
				if (valid) {
					final TuioCursor fc = frameCursor;
					notifyCursors(l -> l.updateTuioCursor(fc));
				}
				break;
			}
			}
		}

		notifyRefresh();
		frameCursors.clear();
	}

	// ---- /tuio/2Dblb ----

	private void processBlobMessage(List<Object> args) {
		String cmd = argString(args, 0);

		if ("source".equals(cmd)) {
			parseSource(args);
		} else if ("set".equals(cmd)) {
			int s_id = argInt(args, 1);
			float xpos = argFloat(args, 2);
			float ypos = argFloat(args, 3);
			float angle = argFloat(args, 4);
			float width = argFloat(args, 5);
			float height = argFloat(args, 6);
			float area = argFloat(args, 7);
			float xspeed = argFloat(args, 8);
			float yspeed = argFloat(args, 9);
			float rspeed = argFloat(args, 10);
			float maccel = argFloat(args, 11);
			float raccel = argFloat(args, 12);

			blobLock.lock();
			try {
				TuioBlob tblb = null;
				for (TuioBlob iter : blobList)
					if (iter.getSessionID() == s_id && iter.getTuioSourceID() == source_id) { tblb = iter; break; }

				if (tblb == null) {
					frameBlobs.add(new TuioBlob(s_id, -1, xpos, ypos, angle, width, height, area));
				} else if (tblb.getX() != xpos || tblb.getY() != ypos || tblb.getAngle() != angle
						|| tblb.getWidth() != width || tblb.getHeight() != height || tblb.getArea() != area
						|| tblb.getXSpeed() != xspeed || tblb.getYSpeed() != yspeed
						|| tblb.getMotionAccel() != maccel) {
					TuioBlob updateBlob = new TuioBlob(s_id, tblb.getBlobID(), xpos, ypos, angle, width, height, area);
					updateBlob.update(xpos, ypos, angle, width, height, area, xspeed, yspeed, rspeed, maccel, raccel);
					frameBlobs.add(updateBlob);
				}
			} finally {
				blobLock.unlock();
			}
		} else if ("alive".equals(cmd)) {
			aliveBlobList.clear();
			for (int i = 1; i < args.size(); i++)
				aliveBlobList.add(argInt(args, i));
		} else if ("fseq".equals(cmd)) {
			processBlobFseq(argInt(args, 1));
		}
	}

	private void processBlobFseq(int fseq) {
		boolean lateFrame = false;
		if (fseq > 0) {
			if (fseq > currentFrame) currentTime = TuioTime.getSessionTime();
			if (fseq >= currentFrame || (currentFrame - fseq) > 100) currentFrame = fseq;
			else lateFrame = true;
		} else if (TuioTime.getSessionTime().getTotalMilliseconds() - currentTime.getTotalMilliseconds() > 100) {
			currentTime = TuioTime.getSessionTime();
		}

		if (lateFrame) {
			frameBlobs.clear();
			return;
		}

		blobLock.lock();
		try {
			for (TuioBlob tblb : blobList) {
				if (tblb.getTuioSourceID() == source_id
						&& !aliveBlobList.contains((int) tblb.getSessionID())) {
					tblb.remove(currentTime);
					frameBlobs.add(new TuioBlob(tblb));
				}
			}
		} finally {
			blobLock.unlock();
		}

		for (TuioBlob tblb : frameBlobs) {
			switch (tblb.getTuioState()) {
			case TuioContainer.TUIO_REMOVED: {
				TuioBlob frameBlob = tblb;
				frameBlob.remove(currentTime);
				notifyBlobs(l -> l.removeTuioBlob(frameBlob));

				blobLock.lock();
				try {
					blobList.removeIf(b -> b.getTuioSourceID() == source_id
							&& b.getSessionID() == frameBlob.getSessionID());

					if (frameBlob.getBlobID() == maxBlobID.get(source_id)) {
						maxBlobID.put(source_id, -1);

						if (!blobList.isEmpty()) {
							int b_id = 0;
							for (TuioBlob clist : blobList) {
								if (clist.getTuioSourceID() == source_id) {
									b_id = clist.getBlobID();
									if (b_id > maxBlobID.get(source_id)) maxBlobID.put(source_id, b_id);
								}
							}
							freeBlobBuffer.clear();
							for (TuioBlob freeBlob : freeBlobList) {
								if (freeBlob.getTuioSourceID() == source_id) {
									if (freeBlob.getBlobID() > maxBlobID.get(source_id)) continue;
									freeBlobBuffer.add(freeBlob);
								} else {
									freeBlobBuffer.add(freeBlob);
								}
							}
							freeBlobList.clear();
							freeBlobList.addAll(freeBlobBuffer);
						} else {
							freeBlobBuffer.clear();
							for (TuioBlob freeBlob : freeBlobList) {
								if (freeBlob.getTuioSourceID() != source_id)
									freeBlobBuffer.add(freeBlob);
							}
							freeBlobList.clear();
							freeBlobList.addAll(freeBlobBuffer);
						}
					} else if (frameBlob.getBlobID() < maxBlobID.get(source_id)) {
						freeBlobList.add(frameBlob);
					}
				} finally {
					blobLock.unlock();
				}
				break;
			}
			case TuioContainer.TUIO_ADDED: {
				int b_id = 0;
				int free_size = 0;
				TuioBlob frameBlob;
				blobLock.lock();
				try {
					for (TuioBlob iter : blobList)
						if (iter.getTuioSourceID() == source_id) b_id++;
					for (TuioBlob iter : freeBlobList)
						if (iter.getTuioSourceID() == source_id) free_size++;

					if (free_size <= maxBlobID.get(source_id) && free_size > 0) {
						TuioBlob closestBlob = null;
						for (TuioBlob iter : freeBlobList) {
							if (iter.getTuioSourceID() == source_id
									&& (closestBlob == null
									|| iter.getDistance(tblb) < closestBlob.getDistance(tblb)))
								closestBlob = iter;
						}
						if (closestBlob != null) {
							b_id = closestBlob.getBlobID();
							freeBlobList.remove(closestBlob);
						}
					} else {
						maxBlobID.put(source_id, b_id);
					}

					frameBlob = new TuioBlob(currentTime, tblb.getSessionID(), b_id,
							tblb.getX(), tblb.getY(), tblb.getAngle(),
							tblb.getWidth(), tblb.getHeight(), tblb.getArea());
					if (!source_name.isEmpty())
						frameBlob.setTuioSource(source_id, source_name, source_addr);
					blobList.add(frameBlob);
				} finally {
					blobLock.unlock();
				}

				notifyBlobs(l -> l.addTuioBlob(frameBlob));
				break;
			}
			default: {
				blobLock.lock();
				TuioBlob frameBlob = null;
				boolean valid = false;
				try {
					for (TuioBlob iter : blobList) {
						if (iter.getTuioSourceID() == source_id
								&& iter.getSessionID() == tblb.getSessionID()) {
							frameBlob = iter;
							break;
						}
					}
					if (frameBlob != null && frameBlob != tblb
							&& frameBlob.getTuioState() != TuioContainer.TUIO_REMOVED) {
						valid = true;
						if ((tblb.getX() != frameBlob.getX() && tblb.getXSpeed() == 0)
								|| (tblb.getY() != frameBlob.getY() && tblb.getYSpeed() == 0)
								|| (tblb.getAngle() != frameBlob.getAngle() && tblb.getRotationSpeed() == 0))
							frameBlob.update(currentTime, tblb.getX(), tblb.getY(), tblb.getAngle(),
									tblb.getWidth(), tblb.getHeight(), tblb.getArea());
						else
							frameBlob.update(currentTime, tblb.getX(), tblb.getY(), tblb.getAngle(),
									tblb.getWidth(), tblb.getHeight(), tblb.getArea(),
									tblb.getXSpeed(), tblb.getYSpeed(), tblb.getRotationSpeed(),
									tblb.getMotionAccel(), tblb.getRotationAccel());
					}
				} finally {
					blobLock.unlock();
				}
				if (valid) {
					final TuioBlob fb = frameBlob;
					notifyBlobs(l -> l.updateTuioBlob(fb));
				}
				break;
			}
			}
		}

		notifyRefresh();
		frameBlobs.clear();
	}
}
