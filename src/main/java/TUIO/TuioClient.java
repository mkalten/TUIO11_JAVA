/*
 TUIO Java library
 Copyright (c) 2005-2014 Martin Kaltenbrunner <martin@tuio.org>
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3.0 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library.
 */
package TUIO;

import com.illposed.osc.*;
import java.util.*;
import org.apache.commons.lang3.event.EventListenerSupport;

/**
 * The TuioClient class is the central TUIO protocol decoder component. It
 * provides a simple callback infrastructure using the {@link TuioListener}
 * interface. In order to receive and decode TUIO messages an instance of
 * TuioClient needs to be created. The TuioClient instance then generates TUIO
 * events which are broadcasted to all registered classes that implement the
 * {@link TuioListener} interface.<P>
 * <code>
 * TuioClient client = new TuioClient();<br>
 * client.addTuioListener(myTuioListener);<br>
 * client.connect();<br>
 * </code>
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.0
 */
public class TuioClient implements OSCListener {

    private int port = 3333;
    private OSCPortIn oscPort;
    private boolean connected = false;
    private final HashMap<Long, TuioObject> objectList = new HashMap<Long, TuioObject>();
    private ArrayList<Long> aliveObjectList = new ArrayList<Long>();
    private ArrayList<Long> newObjectList = new ArrayList<Long>();
    private final HashMap<Long, TuioCursor> cursorList = new HashMap<Long, TuioCursor>();
    private ArrayList<Long> aliveCursorList = new ArrayList<Long>();
    private ArrayList<Long> newCursorList = new ArrayList<Long>();
    private final HashMap<Long, TuioBlob> blobList = new HashMap<Long, TuioBlob>();
    private ArrayList<Long> aliveBlobList = new ArrayList<Long>();
    private ArrayList<Long> newBlobList = new ArrayList<Long>();

    private final ArrayList<TuioObject> frameObjects = new ArrayList<TuioObject>();
    private final ArrayList<TuioCursor> frameCursors = new ArrayList<TuioCursor>();
    private final ArrayList<TuioBlob> frameBlobs = new ArrayList<TuioBlob>();

    private final ArrayList<TuioCursor> freeCursorList = new ArrayList<TuioCursor>();
    private int maxCursorID = -1;
    private final ArrayList<TuioBlob> freeBlobList = new ArrayList<TuioBlob>();
    private int maxBlobID = -1;

    private long currentFrame = 0;
    private TuioTime currentTime;

    private final EventListenerSupport<TuioListener> listenerList
            = EventListenerSupport.create(TuioListener.class);

    /**
     * The default constructor creates a client that listens to the default TUIO
     * port 3333
     */
    public TuioClient() {
    }

    /**
     * This constructor creates a client that listens to the provided port
     *
     * @param port the listening port number
     */
    public TuioClient(int port) {
        this.port = port;
    }

    /**
     * The TuioClient starts listening to TUIO messages on the configured UDP
     * port All reveived TUIO messages are decoded and the resulting TUIO events
     * are broadcasted to all registered TuioListeners
     */
    public void connect() {

        TuioTime.initSession();
        currentTime = new TuioTime();
        currentTime.reset();

        try {
            oscPort = new OSCPortIn(port);
            oscPort.addListener("/tuio/2Dobj", this);
            oscPort.addListener("/tuio/2Dcur", this);
            oscPort.addListener("/tuio/2Dblb", this);
            oscPort.startListening();
            connected = true;
        } catch (Exception e) {
            System.out.println("TuioClient: failed to connect to port " + port);
            connected = false;
        }
    }

    /**
     * The TuioClient stops listening to TUIO messages on the configured UDP
     * port
     */
    public void disconnect() {
        oscPort.stopListening();
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        oscPort.close();
        connected = false;
    }

    /**
     * Returns true if this TuioClient is currently connected.
     *
     * @return	true if this TuioClient is currently connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Adds the provided TuioListener to the list of registered TUIO event
     * listeners
     *
     * @param listener the TuioListener to add
     */
    public void addTuioListener(TuioListener listener) {
        listenerList.addListener(listener);
    }

    /**
     * Removes the provided TuioListener from the list of registered TUIO event
     * listeners
     *
     * @param listener the TuioListener to remove
     */
    public void removeTuioListener(TuioListener listener) {
        listenerList.removeListener(listener);
    }

    /**
     * Removes all TuioListener from the list of registered TUIO event listeners
     */
    public void removeAllTuioListeners() {
        TuioListener[] listeners = listenerList.getListeners();
        for (TuioListener l : listeners) {
            listenerList.removeListener(l);
        }
    }

    /**
     * Returns a ArrayList of all currently active TuioObjects
     *
     * @return a ArrayList of all currently active TuioObjects
     * @deprecated use {@link #getTuioObjectList()} instead.
     */
    @Deprecated
    public ArrayList<TuioObject> getTuioObjects() {
        return new ArrayList<TuioObject>(objectList.values());
    }

    /**
     * Returns an ArrayList of all currently active TuioObjects
     *
     * @return an ArrayList of all currently active TuioObjects
     */
    public ArrayList<TuioObject> getTuioObjectList() {
        return new ArrayList<TuioObject>(objectList.values());
    }

    /**
     * Returns a ArrayList of all currently active TuioCursors
     *
     * @return a ArrayList of all currently active TuioCursors
     * @deprecated use {@link #getTuioCursorList()} instead.
     */
    @Deprecated
    public ArrayList<TuioCursor> getTuioCursors() {
        return new ArrayList<TuioCursor>(cursorList.values());
    }

    /**
     * Returns an ArrayList of all currently active TuioCursors
     *
     * @return an ArrayList of all currently active TuioCursors
     */
    public ArrayList<TuioCursor> getTuioCursorList() {
        return new ArrayList<TuioCursor>(cursorList.values());
    }

    /**
     * Returns a ArrayList of all currently active TuioBlobs
     *
     * @return a ArrayList of all currently active TuioBlobs
     * @deprecated use {@link #getTuioBlobList()} instead.
     */
    @Deprecated
    public ArrayList<TuioBlob> getTuioBlobs() {
        return new ArrayList<TuioBlob>(blobList.values());
    }

    /**
     * Returns an ArrayList of all currently active TuioBlobs
     *
     * @return an ArrayList of all currently active TuioBlobs
     */
    public ArrayList<TuioBlob> getTuioBlobList() {
        return new ArrayList<TuioBlob>(blobList.values());
    }

    /**
     * Returns the TuioObject corresponding to the provided Session ID or NULL
     * if the Session ID does not refer to an active TuioObject
     *
     * @param	s_id	the Session ID of the required TuioObject
     * @return an active TuioObject corresponding to the provided Session ID or
     * NULL
     */
    public TuioObject getTuioObject(long s_id) {
        return objectList.get(s_id);
    }

    /**
     * Returns the TuioCursor corresponding to the provided Session ID or NULL
     * if the Session ID does not refer to an active TuioCursor
     *
     * @param	s_id	the Session ID of the required TuioCursor
     * @return an active TuioCursor corresponding to the provided Session ID or
     * NULL
     */
    public TuioCursor getTuioCursor(long s_id) {
        return cursorList.get(s_id);
    }

    /**
     * Returns the TuioBlob corresponding to the provided Session ID or NULL if
     * the Session ID does not refer to an active TuioBlob
     *
     * @param	s_id	the Session ID of the required TuioBlob
     * @return an active TuioBlob corresponding to the provided Session ID or
     * NULL
     */
    public TuioBlob getTuioBlob(long s_id) {
        return blobList.get(s_id);
    }

    /**
     * The OSC callback method where all TUIO messages are received and decoded
     * and where the TUIO event callbacks are dispatched
     *
     * @param date	the time stamp of the OSC bundle
     * @param message	the received OSC message
     */
    public void acceptMessage(Date date, OSCMessage message) {

        Object[] args = message.getArguments();
        String command = (String) args[0];
        String address = message.getAddress();

        if (address.equals("/tuio/2Dobj")) {

            if (command.equals("set")) {

                long s_id = ((Integer) args[1]).longValue();
                int c_id = ((Integer) args[2]);
                float xpos = ((Float) args[3]);
                float ypos = ((Float) args[4]);
                float angle = ((Float) args[5]);
                float xspeed = ((Float) args[6]);
                float yspeed = ((Float) args[7]);
                float rspeed = ((Float) args[8]);
                float maccel = ((Float) args[9]);
                float raccel = ((Float) args[10]);

                if (objectList.get(s_id) == null) {

                    TuioObject addObject = new TuioObject(s_id, c_id, xpos, ypos, angle);
                    frameObjects.add(addObject);

                } else {

                    TuioObject tobj = objectList.get(s_id);
                    if (tobj == null) {
                        return;
                    }
                    if ((tobj.xpos != xpos) || (tobj.ypos != ypos) || (tobj.angle != angle) || (tobj.x_speed != xspeed) || (tobj.y_speed != yspeed) || (tobj.rotation_speed != rspeed) || (tobj.motion_accel != maccel) || (tobj.rotation_accel != raccel)) {

                        TuioObject updateObject = new TuioObject(s_id, c_id, xpos, ypos, angle);
                        updateObject.update(xpos, ypos, angle, xspeed, yspeed, rspeed, maccel, raccel);
                        frameObjects.add(updateObject);
                    }

                }

            } else if (command.equals("alive")) {

                newObjectList.clear();
                for (int i = 1; i < args.length; i++) {
                    // get the message content
                    long s_id = ((Integer) args[i]).longValue();
                    newObjectList.add(s_id);
                    // reduce the object list to the lost objects
                    if (aliveObjectList.contains(s_id)) {
                        aliveObjectList.remove(s_id);
                    }
                }

                // remove the remaining objects
                for (Long aliveObjectList1 : aliveObjectList) {
                    TuioObject removeObject = objectList.get(aliveObjectList1);
                    if (removeObject == null) {
                        continue;
                    }
                    removeObject.remove(currentTime);
                    frameObjects.add(removeObject);
                }

            } else if (command.equals("fseq")) {

                long fseq = ((Integer) args[1]).longValue();
                boolean lateFrame = false;

                if (fseq > 0) {
                    if (fseq > currentFrame) {
                        currentTime = TuioTime.getSessionTime();
                    }
                    if ((fseq >= currentFrame) || ((currentFrame - fseq) > 100)) {
                        currentFrame = fseq;
                    } else {
                        lateFrame = true;
                    }
                } else if (TuioTime.getSessionTime().subtract(currentTime).getTotalMilliseconds() > 100) {
                    currentTime = TuioTime.getSessionTime();
                }

                if (!lateFrame) {
                    for (TuioObject tobj : frameObjects) {
                        switch (tobj.getTuioState()) {
                            case TuioObject.TUIO_REMOVED:
                                TuioObject removeObject = tobj;
                                removeObject.remove(currentTime);
                                listenerList.fire().removeTuioObject(removeObject);
                                objectList.remove(removeObject.getSessionID());
                                break;

                            case TuioObject.TUIO_ADDED:
                                TuioObject addObject = new TuioObject(currentTime, tobj.getSessionID(), tobj.getSymbolID(), tobj.getX(), tobj.getY(), tobj.getAngle());
                                objectList.put(addObject.getSessionID(), addObject);
                                listenerList.fire().addTuioObject(addObject);
                                break;

                            default:
                                TuioObject updateObject = objectList.get(tobj.getSessionID());
                                if ((tobj.getX() != updateObject.getX() && tobj.getXSpeed() == 0) || (tobj.getY() != updateObject.getY() && tobj.getYSpeed() == 0)) {
                                    updateObject.update(currentTime, tobj.getX(), tobj.getY(), tobj.getAngle());
                                } else {
                                    updateObject.update(currentTime, tobj.getX(), tobj.getY(), tobj.getAngle(), tobj.getXSpeed(), tobj.getYSpeed(), tobj.getRotationSpeed(), tobj.getMotionAccel(), tobj.getRotationAccel());
                                }

                                listenerList.fire().updateTuioObject(updateObject);
                        }
                    }

                    listenerList.fire().refresh(new TuioTime(currentTime, fseq));

                    ArrayList<Long> buffer = aliveObjectList;
                    aliveObjectList = newObjectList;
                    // recycling the vector
                    newObjectList = buffer;
                }
                frameObjects.clear();
            }
        } else if (address.equals("/tuio/2Dcur")) {

            if (command.equals("set")) {

                long s_id = ((Integer) args[1]).longValue();
                float xpos = ((Float) args[2]);
                float ypos = ((Float) args[3]);
                float xspeed = ((Float) args[4]);
                float yspeed = ((Float) args[5]);
                float maccel = ((Float) args[6]);

                if (cursorList.get(s_id) == null) {

                    TuioCursor addCursor = new TuioCursor(s_id, -1, xpos, ypos);
                    frameCursors.add(addCursor);

                } else {

                    TuioCursor tcur = cursorList.get(s_id);
                    if (tcur == null) {
                        return;
                    }
                    if ((tcur.xpos != xpos) || (tcur.ypos != ypos) || (tcur.x_speed != xspeed) || (tcur.y_speed != yspeed) || (tcur.motion_accel != maccel)) {

                        TuioCursor updateCursor = new TuioCursor(s_id, tcur.getCursorID(), xpos, ypos);
                        updateCursor.update(xpos, ypos, xspeed, yspeed, maccel);
                        frameCursors.add(updateCursor);
                    }
                }

                //System.out.println("set cur " + s_id+" "+xpos+" "+ypos+" "+xspeed+" "+yspeed+" "+maccel);
            } else if (command.equals("alive")) {

                newCursorList.clear();
                for (int i = 1; i < args.length; i++) {
                    // get the message content
                    long s_id = ((Integer) args[i]).longValue();
                    newCursorList.add(s_id);
                    // reduce the cursor list to the lost cursors
                    if (aliveCursorList.contains(s_id)) {
                        aliveCursorList.remove(s_id);
                    }
                }

                // remove the remaining cursors
                for (Long aliveCursorList1 : aliveCursorList) {
                    TuioCursor removeCursor = cursorList.get(aliveCursorList1);
                    if (removeCursor == null) {
                        continue;
                    }
                    removeCursor.remove(currentTime);
                    frameCursors.add(removeCursor);
                }

            } else if (command.equals("fseq")) {
                long fseq = ((Integer) args[1]).longValue();
                boolean lateFrame = false;

                if (fseq > 0) {
                    if (fseq > currentFrame) {
                        currentTime = TuioTime.getSessionTime();
                    }
                    if ((fseq >= currentFrame) || ((currentFrame - fseq) > 100)) {
                        currentFrame = fseq;
                    } else {
                        lateFrame = true;
                    }
                } else if (TuioTime.getSessionTime().subtract(currentTime).getTotalMilliseconds() > 100) {
                    currentTime = TuioTime.getSessionTime();
                }
                if (!lateFrame) {

                    for (TuioCursor tcur : frameCursors) {
                        switch (tcur.getTuioState()) {
                            case TuioCursor.TUIO_REMOVED:

                                TuioCursor removeCursor = tcur;
                                removeCursor.remove(currentTime);
                                listenerList.fire().removeTuioCursor(removeCursor);

                                cursorList.remove(removeCursor.getSessionID());

                                if (removeCursor.getCursorID() == maxCursorID) {
                                    maxCursorID = -1;
                                    if (cursorList.size() > 0) {
                                        for (TuioCursor tc : cursorList.values()) {
                                            int c_id = tc.getCursorID();
                                            if (c_id > maxCursorID) {
                                                maxCursorID = c_id;
                                            }
                                        }

                                        for (TuioCursor tc : new ArrayList<TuioCursor>(cursorList.values())) {
                                            int c_id = tc.getCursorID();
                                            if (c_id >= maxCursorID) {
                                                freeCursorList.remove(c_id);
                                            }
                                        }
                                    } else {
                                        freeCursorList.clear();
                                    }
                                } else if (removeCursor.getCursorID() < maxCursorID) {
                                    freeCursorList.add(removeCursor);
                                }

                                break;

                            case TuioCursor.TUIO_ADDED:

                                int c_id = cursorList.size();
                                if ((cursorList.size() <= maxCursorID) && (freeCursorList.size() > 0)) {
                                    TuioCursor closestCursor = freeCursorList.get(0);
                                    for (TuioCursor testCursor : freeCursorList) {
                                        if (testCursor.getDistance(tcur) < closestCursor.getDistance(tcur)) {
                                            closestCursor = testCursor;
                                        }
                                    }
                                    c_id = closestCursor.getCursorID();
                                    freeCursorList.remove(closestCursor);
                                } else {
                                    maxCursorID = c_id;
                                }

                                TuioCursor addCursor = new TuioCursor(currentTime, tcur.getSessionID(), c_id, tcur.getX(), tcur.getY());
                                cursorList.put(addCursor.getSessionID(), addCursor);

                                listenerList.fire().addTuioCursor(addCursor);
                                break;

                            default:

                                TuioCursor updateCursor = cursorList.get(tcur.getSessionID());
                                if ((tcur.getX() != updateCursor.getX() && tcur.getXSpeed() == 0) || (tcur.getY() != updateCursor.getY() && tcur.getYSpeed() == 0)) {
                                    updateCursor.update(currentTime, tcur.getX(), tcur.getY());
                                } else {
                                    updateCursor.update(currentTime, tcur.getX(), tcur.getY(), tcur.getXSpeed(), tcur.getYSpeed(), tcur.getMotionAccel());
                                }

                                listenerList.fire().updateTuioCursor(updateCursor);
                        }
                    }

                    listenerList.fire().refresh(new TuioTime(currentTime, fseq));

                    ArrayList<Long> buffer = aliveCursorList;
                    aliveCursorList = newCursorList;
                    // recycling the vector
                    newCursorList = buffer;
                }

                frameCursors.clear();
            }

        } else if (address.equals("/tuio/2Dblb")) {

            if (command.equals("set")) {

                long s_id = ((Integer) args[1]).longValue();
                float xpos = ((Float) args[2]);
                float ypos = ((Float) args[3]);
                float angle = ((Float) args[4]);
                float width = ((Float) args[5]);
                float height = ((Float) args[6]);
                float area = ((Float) args[7]);
                float xspeed = ((Float) args[8]);
                float yspeed = ((Float) args[9]);
                float rspeed = ((Float) args[10]);
                float maccel = ((Float) args[11]);
                float raccel = ((Float) args[12]);

                if (blobList.get(s_id) == null) {

                    TuioBlob addBlob = new TuioBlob(s_id, -1, xpos, ypos, angle, width, height, area);
                    frameBlobs.add(addBlob);

                } else {

                    TuioBlob tblb = blobList.get(s_id);
                    if (tblb == null) {
                        return;
                    }
                    if ((tblb.xpos != xpos) || (tblb.ypos != ypos) || (tblb.x_speed != xspeed) || (tblb.y_speed != yspeed) || (tblb.motion_accel != maccel)) {

                        TuioBlob updateBlob = new TuioBlob(s_id, tblb.getBlobID(), xpos, ypos, angle, width, height, area);
                        updateBlob.update(xpos, ypos, angle, width, height, area, xspeed, yspeed, rspeed, maccel, raccel);
                        frameBlobs.add(updateBlob);
                    }
                }

                //System.out.println("set blb " + s_id+" "+xpos+" "+ypos+" "+xspeed+" "+yspeed+" "+maccel);
            } else if (command.equals("alive")) {

                newBlobList.clear();
                for (int i = 1; i < args.length; i++) {
                    // get the message content
                    long s_id = ((Integer) args[i]).longValue();
                    newBlobList.add(s_id);
                    // reduce the blob list to the lost blobs
                    if (aliveBlobList.contains(s_id)) {
                        aliveBlobList.remove(s_id);
                    }
                }

                // remove the remaining blobs
                for (Long aliveBlobList1 : aliveBlobList) {
                    TuioBlob removeBlob = blobList.get(aliveBlobList1);
                    if (removeBlob == null) {
                        continue;
                    }
                    removeBlob.remove(currentTime);
                    frameBlobs.add(removeBlob);
                }

            } else if (command.equals("fseq")) {
                long fseq = ((Integer) args[1]).longValue();
                boolean lateFrame = false;

                if (fseq > 0) {
                    if (fseq > currentFrame) {
                        currentTime = TuioTime.getSessionTime();
                    }
                    if ((fseq >= currentFrame) || ((currentFrame - fseq) > 100)) {
                        currentFrame = fseq;
                    } else {
                        lateFrame = true;
                    }
                } else if (TuioTime.getSessionTime().subtract(currentTime).getTotalMilliseconds() > 100) {
                    currentTime = TuioTime.getSessionTime();
                }
                if (!lateFrame) {

                    for (TuioBlob tblb : frameBlobs) {
                        switch (tblb.getTuioState()) {
                            case TuioBlob.TUIO_REMOVED:
                                TuioBlob removeBlob = tblb;
                                removeBlob.remove(currentTime);

                                listenerList.fire().removeTuioBlob(removeBlob);

                                blobList.remove(removeBlob.getSessionID());

                                if (removeBlob.getBlobID() == maxBlobID) {
                                    maxBlobID = -1;
                                    if (blobList.size() > 0) {
                                        for (TuioBlob tb : blobList.values()) {
                                            int b_id = tb.getBlobID();
                                            if (b_id > maxBlobID) {
                                                maxBlobID = b_id;
                                            }
                                        }

                                        for (TuioBlob tb : new ArrayList<TuioBlob>(freeBlobList)) {
                                            int b_id = tb.getBlobID();
                                            if (b_id >= maxBlobID) {
                                                freeBlobList.remove(b_id);
                                            }
                                        }
                                    } else {
                                        freeBlobList.clear();
                                    }
                                } else if (removeBlob.getBlobID() < maxBlobID) {
                                    freeBlobList.add(removeBlob);
                                }

                                break;

                            case TuioBlob.TUIO_ADDED:
                                int b_id = blobList.size();
                                if ((blobList.size() <= maxBlobID) && (freeBlobList.size() > 0)) {
                                    TuioBlob closestBlob = freeBlobList.get(0);
                                    for (TuioBlob testBlob : freeBlobList) {
                                        if (testBlob.getDistance(tblb) < closestBlob.getDistance(tblb)) {
                                            closestBlob = testBlob;
                                        }
                                    }
                                    b_id = closestBlob.getBlobID();
                                    freeBlobList.remove(closestBlob);
                                } else {
                                    maxBlobID = b_id;
                                }

                                TuioBlob addBlob = new TuioBlob(currentTime, tblb.getSessionID(), b_id, tblb.getX(), tblb.getY(), tblb.getAngle(), tblb.getWidth(), tblb.getHeight(), tblb.getArea());
                                blobList.put(addBlob.getSessionID(), addBlob);

                                listenerList.fire().addTuioBlob(addBlob);
                                break;

                            default:
                                TuioBlob updateBlob = blobList.get(tblb.getSessionID());
                                if ((tblb.getX() != updateBlob.getX() && tblb.getXSpeed() == 0) || (tblb.getY() != updateBlob.getY() && tblb.getYSpeed() == 0)) {
                                    updateBlob.update(currentTime, tblb.getX(), tblb.getY(), tblb.getAngle(), tblb.getWidth(), tblb.getHeight(), tblb.getArea());
                                } else {
                                    updateBlob.update(currentTime, tblb.getX(), tblb.getY(), tblb.getAngle(), tblb.getWidth(), tblb.getHeight(), tblb.getArea(), tblb.getXSpeed(), tblb.getYSpeed(), tblb.getRotationSpeed(), tblb.getMotionAccel(), tblb.getRotationAccel());
                                }

                                listenerList.fire().updateTuioBlob(updateBlob);
                        }
                    }

                    listenerList.fire().refresh(new TuioTime(currentTime, fseq));

                    ArrayList<Long> buffer = aliveBlobList;
                    aliveBlobList = newBlobList;
                    // recycling the vector
                    newBlobList = buffer;
                }

                frameBlobs.clear();
            }

        }
    }
}
