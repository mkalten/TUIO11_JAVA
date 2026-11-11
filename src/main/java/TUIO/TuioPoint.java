/*
 TUIO Java library
 Copyright (c) 2005-2016 Martin Kaltenbrunner <martin@tuio.org>
 
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
import java.io.Serializable;

/**
 * The TuioPoint class on the one hand is a simple container and utility class to handle TUIO positions in general, 
 * on the other hand the TuioPoint is the base class for the TuioCursor and TuioObject classes.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */ 
public class TuioPoint implements Serializable {
	
	/**
	 * X coordinate, representated as a floating point value in a range of 0..1  
	 */
	protected float xpos;
	/**
	 * Y coordinate, representated as a floating point value in a range of 0..1  
	 */
	protected float ypos;
	/**
	 * The time stamp of the last update represented as TuioTime (time since session start)
	 */
	protected TuioTime currentTime;
	/**
	 * The creation time of this TuioPoint represented as TuioTime (time since session start)
	 */
	protected TuioTime startTime;
	/**
	 * Optional OneEuroFilter for the X coordinate smoothing
	 */
	protected OneEuroFilter xposFilter;
	/**
	 * Optional OneEuroFilter for the Y coordinate smoothing
	 */
	protected OneEuroFilter yposFilter;
	/**
	 * Position update threshold — small changes below this value are ignored
	 */
	protected float posThreshold = 0.0f;
	
	/**
	 * The default constructor takes no arguments and sets   
	 * its coordinate attributes to zero and its time stamp to the current session time.
	 */
	public TuioPoint() {
		xpos = 0.0f;
		ypos = 0.0f;
		currentTime = TuioTime.getSessionTime();
		startTime = new TuioTime(currentTime);
	}
	
	/**
	 * This constructor takes two floating point coordinate arguments and sets   
	 * its coordinate attributes to these values and its time stamp to the current session time.
	 *
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 */
	public TuioPoint(float xp, float yp) {
		xpos = xp;
		ypos = yp;
		currentTime = TuioTime.getSessionTime();
		startTime = new TuioTime(currentTime);
	}

	/**
	 * This constructor takes a TuioPoint argument and sets its coordinate attributes 
	 * to the coordinates of the provided TuioPoint and its time stamp to the current session time.
	 *
	 * @param	tpoint	the TuioPoint to assign
	 */
	public TuioPoint(TuioPoint tpoint) {
		xpos = tpoint.getX();
		ypos = tpoint.getY();
		currentTime = new TuioTime(tpoint.getTuioTime());
		startTime = new TuioTime(tpoint.getStartTime());
		posThreshold = tpoint.posThreshold;
		xposFilter = tpoint.xposFilter != null ? new OneEuroFilter(tpoint.xposFilter) : null;
		yposFilter = tpoint.yposFilter != null ? new OneEuroFilter(tpoint.yposFilter) : null;
	}
	
	/**
	 * This constructor takes a TuioTime object and two floating point coordinate arguments and sets   
	 * its coordinate attributes to these values and its time stamp to the provided TUIO time object.
	 *
	 * @param	ttime	the TuioTime to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 */
	public TuioPoint(TuioTime ttime, float xp, float yp) {
		xpos = xp;
		ypos = yp;
		currentTime = new TuioTime(ttime);
		startTime = new TuioTime(currentTime);
	}
	
	/**
	 * Takes a TuioPoint argument and updates its coordinate attributes 
	 * to the coordinates of the provided TuioPoint and leaves its time stamp unchanged.
	 *
	 * @param	tpoint	the TuioPoint to assign
	 */
	public void update(TuioPoint tpoint) {
		xpos = tpoint.getX();
		ypos = tpoint.getY();
	}

	/**
	 * Takes two floating point coordinate arguments and updates its coordinate attributes 
	 * to the coordinates of the provided TuioPoint and leaves its time stamp unchanged.
	 *
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 */
	public void update(float xp, float yp) {
		xpos = xp;
		ypos = yp;
	}
	
	/**
	 * Takes a TuioTime object and two floating point coordinate arguments and updates its coordinate attributes 
	 * to the coordinates of the provided TuioPoint and its time stamp to the provided TUIO time object.
	 *
	 * @param	ttime	the TuioTime to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 */
	public void update(TuioTime ttime, float xp, float yp) {

		if (xposFilter != null && yposFilter != null) {
			TuioTime diffTime = ttime.subtract(currentTime);
			float dt = diffTime.getTotalMilliseconds()/1000.0f;
			xp = (float)xposFilter.filter(xp,dt);
			yp = (float)yposFilter.filter(yp,dt);
		}

		float dx = Math.abs(xpos - xp);
		float dy = Math.abs(ypos - yp);
		if ((dx>posThreshold) || (dy>posThreshold)) {
			xpos = xp;
			ypos = yp;
		}

		currentTime = new TuioTime(ttime);
	}
	
	/**
	 * Returns the X coordinate of this TuioPoint. 
	 * @return	the X coordinate of this TuioPoint
	 */
	public float getX() {
		return xpos;
	}
	
	/**
	 * Returns the Y coordinate of this TuioPoint. 
	 * @return	the Y coordinate of this TuioPoint
	 */
	public float getY() {
		return ypos;
	}
	
	/**
	 * Returns the distance to the provided coordinates 
	 *
	 * @param	xp	the X coordinate of the distant point
	 * @param	yp	the Y coordinate of the distant point
	 * @return	the distance to the provided coordinates
	 */
	public float getDistance(float xp, float yp) {
		float dx = xpos-xp;
		float dy = ypos-yp;
		return (float)Math.sqrt(dx*dx+dy*dy);
	}

	/**
	 * Returns the distance to the provided TuioPoint 
	 *
	 * @param	tpoint	the distant TuioPoint
	 * @return	the distance to the provided TuioPoint
	 */
	public float getDistance(TuioPoint tpoint) {
		return getDistance(tpoint.getX(),tpoint.getY());
	}

	/**
	 * Takes two floating point coordinate arguments and the screen dimensions
	 * and returns the distance to the provided coordinates in screen space.
	 *
	 * @param	xp	the X coordinate to check
	 * @param	yp	the Y coordinate to check
	 * @param	w	the screen width
	 * @param	h	the screen height
	 * @return	the distance to the provided coordinates in screen space
	 */
	public float getScreenDistance(float xp, float yp, int w, int h) {
		float dx = w*xpos - w*xp;
		float dy = h*ypos - h*yp;
		return (float)Math.sqrt(dx*dx+dy*dy);
	}

	/**
	 * Returns the angle to the provided coordinates 
	 *
	 * @param	xp	the X coordinate of the distant point
	 * @param	yp	the Y coordinate of the distant point
	 * @return	the angle to the provided coordinates
	 */
	public float getAngle(float xp, float yp) {
		
		float side = xpos-xp;
		float height = ypos-yp;
		float distance = getDistance(xp,yp);
		if (distance==0.0f) return 0.0f;

		float angle = (float)(Math.asin(side/distance)+Math.PI/2);
		if (height<0) angle = 2.0f*(float)Math.PI-angle;
		
		return angle;
	}
	
	/**
	 * Returns the angle to the provided TuioPoint 
	 *
	 * @param	tpoint	the distant TuioPoint
	 * @return	the angle to the provided TuioPoint
	 */
	public float getAngle(TuioPoint tpoint) {						
		return getAngle(tpoint.getX(),tpoint.getY());
	}

	/**
	 * Returns the angle in degrees to the provided coordinates 
	 *
	 * @param	xp	the X coordinate of the distant point
	 * @param	yp	the Y coordinate of the distant point
	 * @return	the angle in degrees to the provided TuioPoint
	 */
	public float getAngleDegrees(float xp, float yp) {		
		return (getAngle(xp,yp)/(float)Math.PI)*180.0f;
	}
	
	/**
	 * Returns the angle in degrees to the provided TuioPoint 
	 *
	 * @param	tpoint	the distant TuioPoint
	 * @return	the angle in degrees to the provided TuioPoint
	 */
	public float getAngleDegrees(TuioPoint tpoint) {		
		return (getAngle(tpoint)/(float)Math.PI)*180.0f;
	}
	
	/**
	 * Returns the X coordinate in pixels relative to the provided screen width. 
	 *
	 * @param	width	the screen width
	 * @return	the X coordinate of this TuioPoint in pixels relative to the provided screen width
	 */
	public int getScreenX(int width) {
		return (int)Math.round(xpos*width);
	}
	
	/**
	 * Returns the Y coordinate in pixels relative to the provided screen height. 
	 *
	 * @param	height	the screen height
	 * @return	the Y coordinate of this TuioPoint in pixels relative to the provided screen height
	 */
	public int getScreenY(int height) {
		return (int)Math.round(ypos*height);
	}
	
	/**
	 * Returns the time stamp of this TuioPoint as TuioTime. 
	 *
	 * @return	the time stamp of this TuioPoint as TuioTime
	 */
	public TuioTime getTuioTime() {
		return new TuioTime(currentTime);
	}
	
	/**
	 * Returns the start time of this TuioPoint as TuioTime. 
	 *
	 * @return	the start time of this TuioPoint as TuioTime
	 */
	public TuioTime getStartTime() {
		return new TuioTime(startTime);
	}

	/**
	 * Adds a OneEuroFilter to the X and Y coordinate of this TuioPoint,
	 * smoothing small position changes while minimizing lag.
	 *
	 * @param	mcut	the minimal cutoff frequency
	 * @param	beta	the speed coefficient
	 */
	public void addPositionFilter(float mcut, float beta) {
		xposFilter = new OneEuroFilter(60.0, mcut, beta, 10.0);
		yposFilter = new OneEuroFilter(60.0, mcut, beta, 10.0);
	}

	/**
	 * Removes the OneEuroFilter from the X and Y coordinate of this TuioPoint.
	 */
	public void removePositionFilter() {
		xposFilter = null;
		yposFilter = null;
	}

	/**
	 * Sets the position update threshold — position changes smaller than
	 * this value are ignored on update.
	 *
	 * @param	threshold	the position threshold
	 */
	public void setPosThreshold(float threshold) {
		posThreshold = threshold;
	}

	/**
	 * Adds a position update threshold — position changes smaller than
	 * this value are ignored on update.
	 *
	 * @param	thresh	the position threshold to set
	 */
	public void addPositionThreshold(float thresh) {
		posThreshold = thresh;
	}

	/**
	 * Removes the position update threshold.
	 */
	public void removePositionThreshold() {
		posThreshold = 0.0f;
	}
}
