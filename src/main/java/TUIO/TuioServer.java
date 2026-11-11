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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import TUIO.osc.OscPacketBuilder;

/**
 * The TuioServer class manages the OSC senders and the state of
 * the TUIO entities on the server side and generates the OSC
 * bundles for the three TUIO profiles.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */
public class TuioServer extends TuioManager {

	// setMessage + fseqMessage size estimates
	static final int OBJ_MESSAGE_SIZE = 108;
	static final int CUR_MESSAGE_SIZE = 88;
	static final int BLB_MESSAGE_SIZE = 116;

	private final List<OscSender> senderList = new ArrayList<OscSender>();
	private OscPacketBuilder oscPacket;
	private OscPacketBuilder fullPacket;

	private int update_interval = 1;
	private boolean full_update = false;
	private boolean periodic_update = false;
	private boolean objectProfileEnabled = true;
	private boolean cursorProfileEnabled = true;
	private boolean blobProfileEnabled = true;
	private String source_name = "";

	private TuioTime objectUpdateTime;
	private TuioTime cursorUpdateTime;
	private TuioTime blobUpdateTime;

	/**
	 * The default constructor creates a TuioServer using the default
	 * UDP sender to localhost:3333.
	 */
	public TuioServer() {
		this(new UdpSender());
	}

	/**
	 * This constructor creates a TuioServer using the default
	 * UDP sender to the provided host and port.
	 *
	 * @param	host	the host to send to
	 * @param	port	the port to send to
	 */
	public TuioServer(String host, int port) {
		this(new UdpSender(host, port));
	}

	/**
	 * This constructor creates a TuioServer using the provided OscSender.
	 *
	 * @param	oscsend	the OscSender to use
	 */
	public TuioServer(OscSender oscsend) {
		initialize(oscsend);
	}

	private void initialize(OscSender oscsend) {
		senderList.add(oscsend);
		int size = oscsend.getBufferSize();
		oscPacket = new OscPacketBuilder(size);
		fullPacket = new OscPacketBuilder(size);

		objectUpdateTime = new TuioTime(getFrameTime());
		cursorUpdateTime = new TuioTime(getFrameTime());
		blobUpdateTime = new TuioTime(getFrameTime());

		if (cursorProfileEnabled) sendEmptyCursorBundle();
		if (objectProfileEnabled) sendEmptyObjectBundle();
		if (blobProfileEnabled) sendEmptyBlobBundle();
	}

	/**
	 * Adds an additional OscSender to the sender list.
	 *
	 * @param	sender	the OscSender to add
	 */
	public void addOscSender(OscSender sender) {
		// resize packets to smallest transport method
		int size = sender.getBufferSize();
		if (size < oscPacket.capacity()) {
			oscPacket = new OscPacketBuilder(size);
			fullPacket = new OscPacketBuilder(size);
		}
		senderList.add(sender);
	}

	private void deliverOscPacket(OscPacketBuilder packet) {
		for (OscSender sender : senderList) sender.sendOscPacket(packet);
	}

	/**
	 * Sends full TUIO messages (a complete dump of the current state).
	 */
	public void sendFullMessages() {
		// prepare the cursor packet
		fullPacket.clear();
		fullPacket.beginBundle();
		if (!source_name.isEmpty())
			fullPacket.beginMessage("/tuio/2Dcur").push("source").push(source_name).endMessage();
		startFullAlive("/tuio/2Dcur");

		for (TuioCursor tcur : cursorList) {
			if ((fullPacket.capacity() - fullPacket.size()) < CUR_MESSAGE_SIZE) {
				fullPacket.beginMessage("/tuio/2Dcur").push("fseq").push(-1).endMessage();
				fullPacket.endBundle();
				deliverOscPacket(fullPacket);

				fullPacket.clear();
				fullPacket.beginBundle();
				if (!source_name.isEmpty())
					fullPacket.beginMessage("/tuio/2Dcur").push("source").push(source_name).endMessage();
				startFullAlive("/tuio/2Dcur");
			}
			float xpos = tcur.getX();
			float xvel = tcur.getXSpeed();
			if (invert_x) { xpos = 1 - xpos; xvel = -1 * xvel; }
			float ypos = tcur.getY();
			float yvel = tcur.getYSpeed();
			if (invert_y) { ypos = 1 - ypos; yvel = -1 * yvel; }

			fullPacket.beginMessage("/tuio/2Dcur").push("set");
			fullPacket.push((int) tcur.getSessionID()).push(xpos).push(ypos);
			fullPacket.push(xvel).push(yvel).push(tcur.getMotionAccel());
			fullPacket.endMessage();
		}
		fullPacket.beginMessage("/tuio/2Dcur").push("fseq").push(-1).endMessage();
		fullPacket.endBundle();
		deliverOscPacket(fullPacket);

		// prepare the object packet
		fullPacket.clear();
		fullPacket.beginBundle();
		if (!source_name.isEmpty())
			fullPacket.beginMessage("/tuio/2Dobj").push("source").push(source_name).endMessage();
		startFullAlive("/tuio/2Dobj");

		for (TuioObject tobj : objectList) {
			if ((fullPacket.capacity() - fullPacket.size()) < OBJ_MESSAGE_SIZE) {
				fullPacket.beginMessage("/tuio/2Dobj").push("fseq").push(-1).endMessage();
				fullPacket.endBundle();
				deliverOscPacket(fullPacket);

				fullPacket.clear();
				fullPacket.beginBundle();
				if (!source_name.isEmpty())
					fullPacket.beginMessage("/tuio/2Dobj").push("source").push(source_name).endMessage();
				startFullAlive("/tuio/2Dobj");
			}
			float xpos = tobj.getX();
			float xvel = tobj.getXSpeed();
			if (invert_x) { xpos = 1 - xpos; xvel = -1 * xvel; }
			float ypos = tobj.getY();
			float yvel = tobj.getYSpeed();
			if (invert_y) { ypos = 1 - ypos; yvel = -1 * yvel; }
			float angle = tobj.getAngle();
			float rvel = tobj.getRotationSpeed();
			if (invert_a) { angle = 2.0f * (float) Math.PI - angle; rvel = -1 * rvel; }

			fullPacket.beginMessage("/tuio/2Dobj").push("set");
			fullPacket.push((int) tobj.getSessionID()).push(tobj.getSymbolID()).push(xpos).push(ypos).push(angle);
			fullPacket.push(xvel).push(yvel).push(rvel).push(tobj.getMotionAccel()).push(tobj.getRotationAccel());
			fullPacket.endMessage();
		}
		fullPacket.beginMessage("/tuio/2Dobj").push("fseq").push(-1).endMessage();
		fullPacket.endBundle();
		deliverOscPacket(fullPacket);

		// prepare the blob packet
		fullPacket.clear();
		fullPacket.beginBundle();
		if (!source_name.isEmpty())
			fullPacket.beginMessage("/tuio/2Dblb").push("source").push(source_name).endMessage();
		startFullAlive("/tuio/2Dblb");

		for (TuioBlob tblb : blobList) {
			if ((fullPacket.capacity() - fullPacket.size()) < BLB_MESSAGE_SIZE) {
				fullPacket.beginMessage("/tuio/2Dblb").push("fseq").push(-1).endMessage();
				fullPacket.endBundle();
				deliverOscPacket(fullPacket);

				fullPacket.clear();
				fullPacket.beginBundle();
				if (!source_name.isEmpty())
					fullPacket.beginMessage("/tuio/2Dblb").push("source").push(source_name).endMessage();
				startFullAlive("/tuio/2Dblb");
			}
			float xpos = tblb.getX();
			float xvel = tblb.getXSpeed();
			if (invert_x) { xpos = 1 - xpos; xvel = -1 * xvel; }
			float ypos = tblb.getY();
			float yvel = tblb.getYSpeed();
			if (invert_y) { ypos = 1 - ypos; yvel = -1 * yvel; }
			float angle = tblb.getAngle();
			float rvel = tblb.getRotationSpeed();
			if (invert_a) { angle = 2.0f * (float) Math.PI - angle; rvel = -1 * rvel; }

			fullPacket.beginMessage("/tuio/2Dblb").push("set");
			fullPacket.push((int) tblb.getSessionID()).push(xpos).push(ypos).push(angle);
			fullPacket.push(tblb.getWidth()).push(tblb.getHeight()).push(tblb.getArea());
			fullPacket.push(xvel).push(yvel).push(rvel).push(tblb.getMotionAccel()).push(tblb.getRotationAccel());
			fullPacket.endMessage();
		}
		fullPacket.beginMessage("/tuio/2Dblb").push("fseq").push(-1).endMessage();
		fullPacket.endBundle();
		deliverOscPacket(fullPacket);
	}

	private void startFullAlive(String profile) {
		fullPacket.beginMessage(profile).push("alive");
		if ("/tuio/2Dcur".equals(profile)) {
			for (TuioCursor tcur : cursorList) fullPacket.push((int) tcur.getSessionID());
		} else if ("/tuio/2Dobj".equals(profile)) {
			for (TuioObject tobj : objectList) fullPacket.push((int) tobj.getSessionID());
		} else {
			for (TuioBlob tblb : blobList) fullPacket.push((int) tblb.getSessionID());
		}
		fullPacket.endMessage();
	}

	/**
	 * Commits the current frame, generating and delivering all
	 * pending TUIO messages and calling refresh() on all listeners.
	 */
	public void commitFrame() {
		TuioTime currentFrameTime = getFrameTime();
		for (TuioListener listener : listenerList)
			listener.refresh(currentFrameTime);

		if (updateObject) {
			startObjectBundle();
			for (TuioObject tobj : objectList) {
				// start a new packet if we exceed the packet capacity
				if ((oscPacket.capacity() - oscPacket.size()) < OBJ_MESSAGE_SIZE) {
					sendObjectBundle(getFrameID());
					startObjectBundle();
				}
				if (full_update || tobj.getTuioTime().equals(currentFrameTime))
					addObjectMessage(tobj);
			}
			objectUpdateTime = new TuioTime(currentFrameTime);
			sendObjectBundle(getFrameID());
		} else if (objectProfileEnabled && periodic_update) {
			TuioTime timeCheck = currentFrameTime.subtract(objectUpdateTime);
			if (timeCheck.getSeconds() >= update_interval) {
				objectUpdateTime = new TuioTime(currentFrameTime);
				startObjectBundle();
				if (full_update) {
					for (TuioObject tobj : objectList) {
						if ((oscPacket.capacity() - oscPacket.size()) < OBJ_MESSAGE_SIZE) {
							sendObjectBundle(getFrameID());
							startObjectBundle();
						}
						addObjectMessage(tobj);
					}
				}
				sendObjectBundle(getFrameID());
			}
		}
		updateObject = false;

		if (updateCursor) {
			startCursorBundle();
			for (TuioCursor tcur : cursorList) {
				if ((oscPacket.capacity() - oscPacket.size()) < CUR_MESSAGE_SIZE) {
					sendCursorBundle(getFrameID());
					startCursorBundle();
				}
				if (full_update || tcur.getTuioTime().equals(currentFrameTime))
					addCursorMessage(tcur);
			}
			cursorUpdateTime = new TuioTime(currentFrameTime);
			sendCursorBundle(getFrameID());
		} else if (cursorProfileEnabled && periodic_update) {
			TuioTime timeCheck = currentFrameTime.subtract(cursorUpdateTime);
			if (timeCheck.getSeconds() >= update_interval) {
				cursorUpdateTime = new TuioTime(currentFrameTime);
				startCursorBundle();
				if (full_update) {
					for (TuioCursor tcur : cursorList) {
						if ((oscPacket.capacity() - oscPacket.size()) < CUR_MESSAGE_SIZE) {
							sendCursorBundle(getFrameID());
							startCursorBundle();
						}
						addCursorMessage(tcur);
					}
				}
				sendCursorBundle(getFrameID());
			}
		}
		updateCursor = false;

		if (updateBlob) {
			startBlobBundle();
			for (TuioBlob tblb : blobList) {
				if ((oscPacket.capacity() - oscPacket.size()) < BLB_MESSAGE_SIZE) {
					sendBlobBundle(getFrameID());
					startBlobBundle();
				}
				if (full_update || tblb.getTuioTime().equals(currentFrameTime))
					addBlobMessage(tblb);
			}
			blobUpdateTime = new TuioTime(currentFrameTime);
			sendBlobBundle(getFrameID());
		} else if (blobProfileEnabled && periodic_update) {
			TuioTime timeCheck = currentFrameTime.subtract(blobUpdateTime);
			if (timeCheck.getSeconds() >= update_interval) {
				blobUpdateTime = new TuioTime(currentFrameTime);
				startBlobBundle();
				if (full_update) {
					for (TuioBlob tblb : blobList) {
						if ((oscPacket.capacity() - oscPacket.size()) < BLB_MESSAGE_SIZE) {
							sendBlobBundle(getFrameID());
							startBlobBundle();
						}
						addBlobMessage(tblb);
					}
				}
				sendBlobBundle(getFrameID());
			}
		}
		updateBlob = false;
	}

	// ---- configuration ----

	/**
	 * Enables the full update mode, including unchanged entities in each frame.
	 */
	public void enableFullUpdate() {
		full_update = true;
	}

	/**
	 * Disables the full update mode.
	 */
	public void disableFullUpdate() {
		full_update = false;
	}

	/**
	 * Returns true if the full update mode is enabled.
	 */
	public boolean fullUpdateEnabled() {
		return full_update;
	}

	/**
	 * Enables periodic alive/fseq messages with the provided interval.
	 *
	 * @param	interval	the update interval in seconds
	 */
	public void enablePeriodicMessages(int interval) {
		periodic_update = true;
		update_interval = interval;
	}

	public void enablePeriodicMessages() {
		enablePeriodicMessages(1);
	}

	/**
	 * Disables periodic messages.
	 */
	public void disablePeriodicMessages() {
		periodic_update = false;
	}

	/**
	 * Returns true if periodic messages are enabled.
	 */
	public boolean periodicMessagesEnabled() {
		return periodic_update;
	}

	/**
	 * Returns the update interval.
	 */
	public int getUpdateInterval() {
		return update_interval;
	}

	/**
	 * Sets the source name and address.
	 *
	 * @param	name	the source name
	 * @param	ip	the source address
	 */
	public void setSourceName(String name, String ip) {
		source_name = name + "@" + ip;
	}

	/**
	 * Sets the source name, resolving the local host address.
	 *
	 * @param	name	the source name
	 */
	public void setSourceName(String name) {
		String addr = "localhost";
		try {
			addr = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {}
		source_name = name + "@" + addr;
		System.out.println("tuio/src " + source_name);
	}

	public void enableObjectProfile(boolean flag) {
		objectProfileEnabled = flag;
	}

	public void enableCursorProfile(boolean flag) {
		cursorProfileEnabled = flag;
	}

	public void enableBlobProfile(boolean flag) {
		blobProfileEnabled = flag;
	}

	public boolean hasObjectProfile() {
		return objectProfileEnabled;
	}

	public boolean hasCursorProfile() {
		return cursorProfileEnabled;
	}

	public boolean hasBlobProfile() {
		return blobProfileEnabled;
	}

	/**
	 * Closes all registered senders.
	 */
	public void close() {
		for (OscSender sender : senderList) sender.close();
	}

	// ---- bundle generation ----

	private void sendEmptyCursorBundle() {
		oscPacket.clear();
		oscPacket.beginBundle();
		if (!source_name.isEmpty())
			oscPacket.beginMessage("/tuio/2Dcur").push("source").push(source_name).endMessage();
		oscPacket.beginMessage("/tuio/2Dcur").push("alive").endMessage();
		oscPacket.beginMessage("/tuio/2Dcur").push("fseq").push(-1).endMessage();
		oscPacket.endBundle();
		deliverOscPacket(oscPacket);
	}

	private void startCursorBundle() {
		oscPacket.clear();
		oscPacket.beginBundle();
		if (!source_name.isEmpty())
			oscPacket.beginMessage("/tuio/2Dcur").push("source").push(source_name).endMessage();
		oscPacket.beginMessage("/tuio/2Dcur").push("alive");
		for (TuioCursor tcur : cursorList)
			oscPacket.push((int) tcur.getSessionID());
		oscPacket.endMessage();
	}

	private void addCursorMessage(TuioCursor tcur) {
		float xpos = tcur.getX();
		float xvel = tcur.getXSpeed();
		if (invert_x) { xpos = 1 - xpos; xvel = -1 * xvel; }
		float ypos = tcur.getY();
		float yvel = tcur.getYSpeed();
		if (invert_y) { ypos = 1 - ypos; yvel = -1 * yvel; }

		oscPacket.beginMessage("/tuio/2Dcur").push("set");
		oscPacket.push((int) tcur.getSessionID()).push(xpos).push(ypos);
		oscPacket.push(xvel).push(yvel).push(tcur.getMotionAccel());
		oscPacket.endMessage();
	}

	private void sendCursorBundle(int fseq) {
		oscPacket.beginMessage("/tuio/2Dcur").push("fseq").push(fseq).endMessage();
		oscPacket.endBundle();
		deliverOscPacket(oscPacket);
	}

	private void sendEmptyObjectBundle() {
		oscPacket.clear();
		oscPacket.beginBundle();
		if (!source_name.isEmpty())
			oscPacket.beginMessage("/tuio/2Dobj").push("source").push(source_name).endMessage();
		oscPacket.beginMessage("/tuio/2Dobj").push("alive").endMessage();
		oscPacket.beginMessage("/tuio/2Dobj").push("fseq").push(-1).endMessage();
		oscPacket.endBundle();
		deliverOscPacket(oscPacket);
	}

	private void startObjectBundle() {
		oscPacket.clear();
		oscPacket.beginBundle();
		if (!source_name.isEmpty())
			oscPacket.beginMessage("/tuio/2Dobj").push("source").push(source_name).endMessage();
		oscPacket.beginMessage("/tuio/2Dobj").push("alive");
		for (TuioObject tobj : objectList)
			oscPacket.push((int) tobj.getSessionID());
		oscPacket.endMessage();
	}

	private void addObjectMessage(TuioObject tobj) {
		float xpos = tobj.getX();
		float xvel = tobj.getXSpeed();
		if (invert_x) { xpos = 1 - xpos; xvel = -1 * xvel; }
		float ypos = tobj.getY();
		float yvel = tobj.getYSpeed();
		if (invert_y) { ypos = 1 - ypos; yvel = -1 * yvel; }
		float angle = tobj.getAngle();
		float rvel = tobj.getRotationSpeed();
		if (invert_a) { angle = 2.0f * (float) Math.PI - angle; rvel = -1 * rvel; }

		oscPacket.beginMessage("/tuio/2Dobj").push("set");
		oscPacket.push((int) tobj.getSessionID()).push(tobj.getSymbolID()).push(xpos).push(ypos).push(angle);
		oscPacket.push(xvel).push(yvel).push(rvel).push(tobj.getMotionAccel()).push(tobj.getRotationAccel());
		oscPacket.endMessage();
	}

	private void sendObjectBundle(int fseq) {
		oscPacket.beginMessage("/tuio/2Dobj").push("fseq").push(fseq).endMessage();
		oscPacket.endBundle();
		deliverOscPacket(oscPacket);
	}

	private void sendEmptyBlobBundle() {
		oscPacket.clear();
		oscPacket.beginBundle();
		if (!source_name.isEmpty())
			oscPacket.beginMessage("/tuio/2Dblb").push("source").push(source_name).endMessage();
		oscPacket.beginMessage("/tuio/2Dblb").push("alive").endMessage();
		oscPacket.beginMessage("/tuio/2Dblb").push("fseq").push(-1).endMessage();
		oscPacket.endBundle();
		deliverOscPacket(oscPacket);
	}

	private void startBlobBundle() {
		oscPacket.clear();
		oscPacket.beginBundle();
		if (!source_name.isEmpty())
			oscPacket.beginMessage("/tuio/2Dblb").push("source").push(source_name).endMessage();
		oscPacket.beginMessage("/tuio/2Dblb").push("alive");
		for (TuioBlob tblb : blobList)
			oscPacket.push((int) tblb.getSessionID());
		oscPacket.endMessage();
	}

	private void addBlobMessage(TuioBlob tblb) {
		float xpos = tblb.getX();
		float xvel = tblb.getXSpeed();
		if (invert_x) { xpos = 1 - xpos; xvel = -1 * xvel; }
		float ypos = tblb.getY();
		float yvel = tblb.getYSpeed();
		if (invert_y) { ypos = 1 - ypos; yvel = -1 * yvel; }
		float angle = tblb.getAngle();
		float rvel = tblb.getRotationSpeed();
		if (invert_a) { angle = 2.0f * (float) Math.PI - angle; rvel = -1 * rvel; }

		oscPacket.beginMessage("/tuio/2Dblb").push("set");
		oscPacket.push((int) tblb.getSessionID()).push(xpos).push(ypos).push(angle);
		oscPacket.push(tblb.getWidth()).push(tblb.getHeight()).push(tblb.getArea());
		oscPacket.push(xvel).push(yvel).push(rvel).push(tblb.getMotionAccel()).push(tblb.getRotationAccel());
		oscPacket.endMessage();
	}

	private void sendBlobBundle(int fseq) {
		oscPacket.beginMessage("/tuio/2Dblb").push("fseq").push(fseq).endMessage();
		oscPacket.endBundle();
		deliverOscPacket(oscPacket);
	}
}
