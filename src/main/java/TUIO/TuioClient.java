/**
 * TUIO Java Console Example Copyright (c) 2005-2014 Martin Kaltenbrunner
 * <martin@tuio.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * Modified by Matthew Aguirre <matta@artistech.com>
 */
package TUIO;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.event.EventListenerSupport;
import org.apache.commons.logging.LogFactory;

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
public class TuioClient {

    private int port = 3333;
    private OSCPortIn oscPort;
    private boolean connected = false;

    private final HashMap<Long, TuioObject> objectMap = new HashMap<Long, TuioObject>();
    private final HashMap<Long, TuioCursor> cursorMap = new HashMap<Long, TuioCursor>();
    private final HashMap<Long, TuioBlob> blobMap = new HashMap<Long, TuioBlob>();

    private long currentFrame = 0;
    private TuioTime currentTime;

    private final EventListenerSupport<TuioListener> listenerList
            = EventListenerSupport.create(TuioListener.class);

    /**
     * Listens only on the object channel.
     */
    private class ObjListener2D implements OSCListener {

        private final ArrayList<TuioObject> frameObjects = new ArrayList<TuioObject>();
        private final ArrayList<Long> aliveObjectList = new ArrayList<Long>();
        private final ArrayList<Long> newObjectList = new ArrayList<Long>();

        /**
         * The OSC callback method where all TUIO messages are received and
         * decoded and where the TUIO event call-backs are dispatched.
         *
         * @param date	the time stamp of the OSC bundle
         * @param message	the received OSC message
         */
        public void acceptMessage(Date date, OSCMessage oscm) {
            Object[] args = oscm.getArguments();
            String command = (String) args[0];
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

                if (TuioClient.this.objectMap.get(s_id) == null) {

                    TuioObject addObject = new TuioObject(s_id, c_id, xpos, ypos, angle);
                    frameObjects.add(addObject);

                } else {

                    TuioObject tobj = TuioClient.this.objectMap.get(s_id);
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
                    if (aliveObjectList.contains((Long) s_id)) {
                        aliveObjectList.remove((Long) s_id);
                    }
                }

                // remove the remaining objects
                for (Long aliveObjectList1 : aliveObjectList) {
                    TuioObject removeObject = TuioClient.this.objectMap.get(aliveObjectList1);
                    if (removeObject == null) {
                        continue;
                    }
                    removeObject.remove(TuioClient.this.currentTime);
                    frameObjects.add(removeObject);
                }

            } else if (command.equals("fseq")) {

                long fseq = ((Integer) args[1]).longValue();
                boolean lateFrame = false;

                if (fseq > 0) {
                    if (fseq > TuioClient.this.currentFrame) {
                        TuioClient.this.currentTime = TuioTime.getSessionTime();
                    }
                    if ((fseq >= TuioClient.this.currentFrame) || ((TuioClient.this.currentFrame - fseq) > 100)) {
                        TuioClient.this.currentFrame = fseq;
                    } else {
                        lateFrame = true;
                    }
                } else if (TuioTime.getSessionTime().subtract(TuioClient.this.currentTime).getTotalMilliseconds() > 100) {
                    TuioClient.this.currentTime = TuioTime.getSessionTime();
                }

                if (!lateFrame) {
                    for (TuioObject tobj : frameObjects) {
                        switch (tobj.getTuioState()) {
                            case TuioObject.TUIO_REMOVED:
                                TuioObject removeObject = tobj;
                                removeObject.remove(TuioClient.this.currentTime);
                                TuioClient.this.listenerList.fire().removeTuioObject(removeObject);
                                TuioClient.this.objectMap.remove((Long) removeObject.getSessionID());
                                break;

                            case TuioObject.TUIO_ADDED:
                                TuioObject addObject = new TuioObject(TuioClient.this.currentTime, tobj.getSessionID(), tobj.getSymbolID(), tobj.getX(), tobj.getY(), tobj.getAngle());
                                TuioClient.this.objectMap.put(addObject.getSessionID(), addObject);
                                TuioClient.this.listenerList.fire().addTuioObject(addObject);
                                break;

                            default:
                                TuioObject updateObject = TuioClient.this.objectMap.get(tobj.getSessionID());
                                if ((tobj.getX() != updateObject.getX() && tobj.getXSpeed() == 0) || (tobj.getY() != updateObject.getY() && tobj.getYSpeed() == 0)) {
                                    updateObject.update(TuioClient.this.currentTime, tobj.getX(), tobj.getY(), tobj.getAngle());
                                } else {
                                    updateObject.update(TuioClient.this.currentTime, tobj.getX(), tobj.getY(), tobj.getAngle(), tobj.getXSpeed(), tobj.getYSpeed(), tobj.getRotationSpeed(), tobj.getMotionAccel(), tobj.getRotationAccel());
                                }

                                TuioClient.this.listenerList.fire().updateTuioObject(updateObject);
                        }
                    }

                    TuioClient.this.listenerList.fire().refresh(new TuioTime(TuioClient.this.currentTime, fseq));

                    aliveObjectList.clear();
                    aliveObjectList.addAll(newObjectList);
                    newObjectList.clear();
                }
                frameObjects.clear();
            }
        }
    }

    /**
     * Listens only on the cursor channel.
     */
    private class CurListener2D implements OSCListener {

        private final ArrayList<TuioCursor> frameCursors = new ArrayList<TuioCursor>();
        private final ArrayList<Long> aliveCursorList = new ArrayList<Long>();
        private final ArrayList<Long> newCursorList = new ArrayList<Long>();
        private final ArrayList<TuioCursor> freeCursorList = new ArrayList<TuioCursor>();
        private int maxCursorID = -1;

        /**
         * The OSC callback method where all TUIO messages are received and
         * decoded and where the TUIO event call-backs are dispatched.
         *
         * @param date	the time stamp of the OSC bundle
         * @param message	the received OSC message
         */
        public void acceptMessage(Date date, OSCMessage oscm) {
            Object[] args = oscm.getArguments();
            String command = (String) args[0];
            if (command.equals("set")) {

                long s_id = ((Integer) args[1]).longValue();
                float xpos = ((Float) args[2]);
                float ypos = ((Float) args[3]);
                float xspeed = ((Float) args[4]);
                float yspeed = ((Float) args[5]);
                float maccel = ((Float) args[6]);

                if (TuioClient.this.cursorMap.get(s_id) == null) {

                    TuioCursor addCursor = new TuioCursor(s_id, -1, xpos, ypos);
                    frameCursors.add(addCursor);

                } else {

                    TuioCursor tcur = TuioClient.this.cursorMap.get(s_id);
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
                    if (aliveCursorList.contains((Long) s_id)) {
                        aliveCursorList.remove((Long) s_id);
                    }
                }

                // remove the remaining cursors
                for (Long aliveCursorList1 : aliveCursorList) {
                    TuioCursor removeCursor = TuioClient.this.cursorMap.get(aliveCursorList1);
                    if (removeCursor == null) {
                        continue;
                    }
                    removeCursor.remove(TuioClient.this.currentTime);
                    frameCursors.add(removeCursor);
                }

            } else if (command.equals("fseq")) {
                long fseq = ((Integer) args[1]).longValue();
                boolean lateFrame = false;

                if (fseq > 0) {
                    if (fseq > TuioClient.this.currentFrame) {
                        TuioClient.this.currentTime = TuioTime.getSessionTime();
                    }
                    if ((fseq >= TuioClient.this.currentFrame) || ((TuioClient.this.currentFrame - fseq) > 100)) {
                        TuioClient.this.currentFrame = fseq;
                    } else {
                        lateFrame = true;
                    }
                } else if (TuioTime.getSessionTime().subtract(TuioClient.this.currentTime).getTotalMilliseconds() > 100) {
                    TuioClient.this.currentTime = TuioTime.getSessionTime();
                }
                if (!lateFrame) {

                    for (TuioCursor tcur : frameCursors) {
                        switch (tcur.getTuioState()) {
                            case TuioCursor.TUIO_REMOVED:

                                TuioCursor removeCursor = tcur;
                                removeCursor.remove(TuioClient.this.currentTime);
                                TuioClient.this.listenerList.fire().removeTuioCursor(removeCursor);

                                TuioClient.this.cursorMap.remove((Long) removeCursor.getSessionID());

                                if (removeCursor.getCursorID() == maxCursorID) {
                                    maxCursorID = -1;
                                    if (TuioClient.this.cursorMap.size() > 0) {
                                        for (TuioCursor tc : TuioClient.this.cursorMap.values()) {
                                            int c_id = tc.getCursorID();
                                            if (c_id > maxCursorID) {
                                                maxCursorID = c_id;
                                            }
                                        }

                                        for (TuioCursor tc : new ArrayList<TuioCursor>(freeCursorList)) {
                                            int c_id = tc.getCursorID();
                                            if (c_id >= maxCursorID) {
                                                freeCursorList.remove(tc);
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

                                int c_id = TuioClient.this.cursorMap.size();
                                if ((TuioClient.this.cursorMap.size() <= maxCursorID) && (freeCursorList.size() > 0)) {
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

                                TuioCursor addCursor = new TuioCursor(TuioClient.this.currentTime, tcur.getSessionID(), c_id, tcur.getX(), tcur.getY());
                                TuioClient.this.cursorMap.put(addCursor.getSessionID(), addCursor);

                                TuioClient.this.listenerList.fire().addTuioCursor(addCursor);
                                break;

                            default:

                                TuioCursor updateCursor = TuioClient.this.cursorMap.get(tcur.getSessionID());
                                if ((tcur.getX() != updateCursor.getX() && tcur.getXSpeed() == 0) || (tcur.getY() != updateCursor.getY() && tcur.getYSpeed() == 0)) {
                                    updateCursor.update(TuioClient.this.currentTime, tcur.getX(), tcur.getY());
                                } else {
                                    updateCursor.update(TuioClient.this.currentTime, tcur.getX(), tcur.getY(), tcur.getXSpeed(), tcur.getYSpeed(), tcur.getMotionAccel());
                                }

                                TuioClient.this.listenerList.fire().updateTuioCursor(updateCursor);
                        }
                    }

                    TuioClient.this.listenerList.fire().refresh(new TuioTime(TuioClient.this.currentTime, fseq));

                    aliveCursorList.clear();
                    aliveCursorList.addAll(newCursorList);
                    newCursorList.clear();
                }

                frameCursors.clear();
            }
        }
    }

    /**
     * Listens only on the blob channel.
     */
    private class BlobListener2D implements OSCListener {

        private final ArrayList<TuioBlob> frameBlobs = new ArrayList<TuioBlob>();
        private final ArrayList<Long> aliveBlobList = new ArrayList<Long>();
        private final ArrayList<Long> newBlobList = new ArrayList<Long>();
        private final ArrayList<TuioBlob> freeBlobList = new ArrayList<TuioBlob>();
        private int maxBlobID = -1;

        /**
         * The OSC callback method where all TUIO messages are received and
         * decoded and where the TUIO event call-backs are dispatched.
         *
         * @param date	the time stamp of the OSC bundle
         * @param message	the received OSC message
         */
        public void acceptMessage(Date date, OSCMessage oscm) {
            Object[] args = oscm.getArguments();
            String command = (String) args[0];
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

                if (TuioClient.this.blobMap.get(s_id) == null) {

                    TuioBlob addBlob = new TuioBlob(s_id, -1, xpos, ypos, angle, width, height, area);
                    frameBlobs.add(addBlob);

                } else {

                    TuioBlob tblb = TuioClient.this.blobMap.get(s_id);
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
                    if (aliveBlobList.contains((Long) s_id)) {
                        aliveBlobList.remove((Long) s_id);
                    }
                }

                // remove the remaining blobs
                for (Long aliveBlobList1 : aliveBlobList) {
                    TuioBlob removeBlob = TuioClient.this.blobMap.get(aliveBlobList1);
                    if (removeBlob == null) {
                        continue;
                    }
                    removeBlob.remove(TuioClient.this.currentTime);
                    frameBlobs.add(removeBlob);
                }

            } else if (command.equals("fseq")) {
                long fseq = ((Integer) args[1]).longValue();
                boolean lateFrame = false;

                if (fseq > 0) {
                    if (fseq > TuioClient.this.currentFrame) {
                        TuioClient.this.currentTime = TuioTime.getSessionTime();
                    }
                    if ((fseq >= TuioClient.this.currentFrame) || ((TuioClient.this.currentFrame - fseq) > 100)) {
                        TuioClient.this.currentFrame = fseq;
                    } else {
                        lateFrame = true;
                    }
                } else if (TuioTime.getSessionTime().subtract(TuioClient.this.currentTime).getTotalMilliseconds() > 100) {
                    TuioClient.this.currentTime = TuioTime.getSessionTime();
                }
                if (!lateFrame) {

                    for (TuioBlob tblb : frameBlobs) {
                        switch (tblb.getTuioState()) {
                            case TuioBlob.TUIO_REMOVED:
                                TuioBlob removeBlob = tblb;
                                removeBlob.remove(TuioClient.this.currentTime);

                                TuioClient.this.listenerList.fire().removeTuioBlob(removeBlob);

                                TuioClient.this.blobMap.remove((Long) removeBlob.getSessionID());

                                if (removeBlob.getBlobID() == maxBlobID) {
                                    maxBlobID = -1;
                                    if (TuioClient.this.blobMap.size() > 0) {
                                        for (TuioBlob tb : TuioClient.this.blobMap.values()) {
                                            int b_id = tb.getBlobID();
                                            if (b_id > maxBlobID) {
                                                maxBlobID = b_id;
                                            }
                                        }

                                        for (TuioBlob tb : new ArrayList<TuioBlob>(freeBlobList)) {
                                            int b_id = tb.getBlobID();
                                            if (b_id >= maxBlobID) {
                                                freeBlobList.remove(tb);
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
                                int b_id = TuioClient.this.blobMap.size();
                                if ((TuioClient.this.blobMap.size() <= maxBlobID) && (freeBlobList.size() > 0)) {
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

                                TuioBlob addBlob = new TuioBlob(TuioClient.this.currentTime, tblb.getSessionID(), b_id, tblb.getX(), tblb.getY(), tblb.getAngle(), tblb.getWidth(), tblb.getHeight(), tblb.getArea());
                                TuioClient.this.blobMap.put(addBlob.getSessionID(), addBlob);

                                TuioClient.this.listenerList.fire().addTuioBlob(addBlob);
                                break;

                            default:
                                TuioBlob updateBlob = TuioClient.this.blobMap.get(tblb.getSessionID());
                                if ((tblb.getX() != updateBlob.getX() && tblb.getXSpeed() == 0) || (tblb.getY() != updateBlob.getY() && tblb.getYSpeed() == 0)) {
                                    updateBlob.update(TuioClient.this.currentTime, tblb.getX(), tblb.getY(), tblb.getAngle(), tblb.getWidth(), tblb.getHeight(), tblb.getArea());
                                } else {
                                    updateBlob.update(TuioClient.this.currentTime, tblb.getX(), tblb.getY(), tblb.getAngle(), tblb.getWidth(), tblb.getHeight(), tblb.getArea(), tblb.getXSpeed(), tblb.getYSpeed(), tblb.getRotationSpeed(), tblb.getMotionAccel(), tblb.getRotationAccel());
                                }

                                TuioClient.this.listenerList.fire().updateTuioBlob(updateBlob);
                        }
                    }

                    TuioClient.this.listenerList.fire().refresh(new TuioTime(TuioClient.this.currentTime, fseq));

                    aliveBlobList.clear();
                    aliveBlobList.addAll(newBlobList);
                    newBlobList.clear();
                }

                frameBlobs.clear();
            }
        }
    }

    /**
     * The default constructor creates a client that listens to the default TUIO
     * port 3333.
     */
    public TuioClient() {
    }

    /**
     * This constructor creates a client that listens to the provided port.
     *
     * @param port the listening port number
     */
    public TuioClient(int port) {
        this.port = port;
    }

    /**
     * The TuioClient starts listening to TUIO messages on the configured UDP
     * port All received TUIO messages are decoded and the resulting TUIO events
     * are broadcasted to all registered TuioListeners.
     */
    public void connect() {

        TuioTime.initSession();
        currentTime = new TuioTime();
        currentTime.reset();

        try {
            oscPort = new OSCPortIn(port);
            oscPort.addListener("/tuio/2Dobj", new ObjListener2D());
            oscPort.addListener("/tuio/2Dcur", new CurListener2D());
            oscPort.addListener("/tuio/2Dblb", new BlobListener2D());
            oscPort.startListening();
            connected = true;
        } catch (Exception e) {
            LogFactory.getLog(TuioClient.class).fatal(MessageFormat.format("Failed to connect to port: {0}", new Object[]{Integer.toString(port)}));
            connected = false;
        }
    }

    /**
     * The TuioClient stops listening to TUIO messages on the configured UDP
     * port.
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
     * listeners.
     *
     * @param listener the TuioListener to add
     */
    public void addTuioListener(TuioListener listener) {
        listenerList.addListener(listener);
    }

    /**
     * Removes the provided TuioListener from the list of registered TUIO event
     * listeners.
     *
     * @param listener the TuioListener to remove
     */
    public void removeTuioListener(TuioListener listener) {
        listenerList.removeListener(listener);
    }

    /**
     * Removes all TuioListener from the list of registered TUIO event
     * listeners.
     */
    public void removeAllTuioListeners() {
        TuioListener[] listeners = listenerList.getListeners();
        for (TuioListener l : listeners) {
            listenerList.removeListener(l);
        }
    }

    /**
     * Returns an ArrayList of all currently active TuioObjects.
     *
     * @return an ArrayList of all currently active TuioObjects
     */
    public List<TuioObject> getTuioObjectList() {
        return new ArrayList<TuioObject>(objectMap.values());
    }

    /**
     * Returns an ArrayList of all currently active TuioCursors.
     *
     * @return an ArrayList of all currently active TuioCursors
     */
    public List<TuioCursor> getTuioCursorList() {
        return new ArrayList<TuioCursor>(cursorMap.values());
    }

    /**
     * Returns an ArrayList of all currently active TuioBlobs.
     *
     * @return an ArrayList of all currently active TuioBlobs
     */
    public List<TuioBlob> getTuioBlobList() {
        return new ArrayList<TuioBlob>(blobMap.values());
    }

    /**
     * Returns the TuioObject corresponding to the provided Session ID or NULL
     * if the Session ID does not refer to an active TuioObject.
     *
     * @param	s_id	the Session ID of the required TuioObject
     * @return an active TuioObject corresponding to the provided Session ID or
     * NULL
     */
    public TuioObject getTuioObject(long s_id) {
        return objectMap.get(s_id);
    }

    /**
     * Returns the TuioCursor corresponding to the provided Session ID or NULL
     * if the Session ID does not refer to an active TuioCursor.
     *
     * @param	s_id	the Session ID of the required TuioCursor
     * @return an active TuioCursor corresponding to the provided Session ID or
     * NULL
     */
    public TuioCursor getTuioCursor(long s_id) {
        return cursorMap.get(s_id);
    }

    /**
     * Returns the TuioBlob corresponding to the provided Session ID or NULL if
     * the Session ID does not refer to an active TuioBlob.
     *
     * @param	s_id	the Session ID of the required TuioBlob
     * @return an active TuioBlob corresponding to the provided Session ID or
     * NULL
     */
    public TuioBlob getTuioBlob(long s_id) {
        return blobMap.get(s_id);
    }
}
