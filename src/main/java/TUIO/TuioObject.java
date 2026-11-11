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

/**
 * The TuioObject class encapsulates /tuio/2Dobj TUIO objects.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */ 
public class TuioObject extends TuioContainer {
	
	/**
	 * The individual symbol ID number that is assigned to each TuioObject.
	 */ 
	protected int symbol_id;
	/**
	 * The rotation angle value.
	 */ 
	protected float angle;
	/**
	 * The rotation speed value.
	 */ 
	protected float rotation_speed;
	/**
	 * The rotation acceleration value.
	 */ 
	protected float rotation_accel;
	/**
	 * Defines the ROTATING state.
	 */
	public static final int TUIO_ROTATING = 5;
	/**
	 * Optional OneEuroFilter for the rotation angle smoothing
	 */
	protected OneEuroFilter angleFilter;
	/**
	 * Angle update threshold — small angle changes below this value are ignored
	 */
	protected float angleThreshold = 0.0f;
	
	/**
	 * This constructor takes a TuioTime argument and assigns it along with the provided 
	 * Session ID, Symbol ID, X and Y coordinate and angle to the newly created TuioObject.
	 *
	 * @param	ttime	the TuioTime to assign
	 * @param	si	the Session ID  to assign
	 * @param	sym	the Symbol ID  to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the angle to assign
	 */
	public TuioObject (TuioTime ttime, long si, int sym, float xp, float yp, float a) {
		super(ttime, si,xp,yp);
		symbol_id = sym;
		angle = a;
		rotation_speed = 0.0f;
		rotation_accel = 0.0f;
	}

	/**
	 * This constructor takes the provided Session ID, Symbol ID, X and Y coordinate 
	 * and angle, and assigs these values to the newly created TuioObject.
	 *
	 * @param	si	the Session ID  to assign
	 * @param	sym	the Symbol ID  to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the angle to assign
	 */	
	public TuioObject (long si, int sym, float xp, float yp, float a) {
		super(si,xp,yp);
		symbol_id = sym;
		angle = a;
		rotation_speed = 0.0f;
		rotation_accel = 0.0f;
	}

	/**
	 * This constructor takes the atttibutes of the provided TuioObject 
	 * and assigs these values to the newly created TuioObject.
	 *
	 * @param	tobj	the TuioObject to assign
	 */
	public TuioObject (TuioObject tobj) {
		super(tobj);
		symbol_id = tobj.symbol_id;
		angle = tobj.angle;
		rotation_speed = tobj.rotation_speed;
		rotation_accel = tobj.rotation_accel;
		angleFilter = tobj.angleFilter != null ? new OneEuroFilter(tobj.angleFilter) : null;
		angleThreshold = tobj.angleThreshold;
	}

	/**
	 * Takes a TuioTime argument and assigns it along with the provided 
	 * X and Y coordinate, angle, X and Y velocity, motion acceleration,
	 * rotation speed and rotation acceleration to the private TuioObject attributes.
	 *
	 * @param	ttime	the TuioTime to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the angle coordinate to assign
	 * @param	xs	the X velocity to assign
	 * @param	ys	the Y velocity to assign
	 * @param	rs	the rotation velocity to assign
	 * @param	ma	the motion acceleration to assign
	 * @param	ra	the rotation acceleration to assign
	 */
	public void update (TuioTime ttime, float xp, float yp, float a, float xs, float ys, float rs, float ma, float ra) {
		super.update(ttime,xp,yp,xs,ys,ma);
		angle = a;
		rotation_speed = rs;
		rotation_accel = ra;
		if ((rotation_accel!=0) && (state==TUIO_STOPPED)) state = TUIO_ROTATING;
	}

	/**
	 * Assigns the provided X and Y coordinate, angle, X and Y velocity, motion acceleration
	 * rotation velocity and rotation acceleration to the private TuioContainer attributes.
	 * The TuioTime time stamp remains unchanged.
	 *
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the angle coordinate to assign
	 * @param	xs	the X velocity to assign
	 * @param	ys	the Y velocity to assign
	 * @param	rs	the rotation velocity to assign
	 * @param	ma	the motion acceleration to assign
	 * @param	ra	the rotation acceleration to assign
	 */
	public void update (float xp, float yp, float a, float xs, float ys, float rs, float ma, float ra) {
		super.update(xp,yp,xs,ys,ma);
		angle = a;
		rotation_speed = rs;
		rotation_accel = ra;
		if ((rotation_accel!=0) && (state==TUIO_STOPPED)) state = TUIO_ROTATING;
	}
	
	/**
	 * Takes a TuioTime argument and assigns it along with the provided 
	 * X and Y coordinate and angle to the private TuioObject attributes.
	 * The speed and accleration values are calculated accordingly.
	 *
	 * @param	ttime	the TuioTime to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the angle coordinate to assign
	 */
	public void update (TuioTime ttime, float xp, float yp, float a) {
		lastPoint = path.peekLast();
		super.update(ttime,xp,yp);

		TuioTime diffTime = currentTime.subtract(lastPoint.getTuioTime());
		float dt = diffTime.getTotalMilliseconds()/1000.0f;
		float last_rotation_speed = rotation_speed;

		float da = a-angle;
		if (da > Math.PI) da -= 2*(float)Math.PI;
		else if (da < -Math.PI) da += 2*(float)Math.PI;

		float prev_angle = angle;
		if (angleFilter != null) angle = (float)angleFilter.filter(angle+da,dt);
		else angle = angle+da;
		if (Math.abs(angle-prev_angle)<angleThreshold) angle = prev_angle;

		if (angle > 2*Math.PI) angle-=2*(float)Math.PI;
		else if (angle < 0) angle+=2*(float)Math.PI;

		da = angle-prev_angle;
		if (da > Math.PI) da -= 2*(float)Math.PI;
		else if (da < -Math.PI) da += 2*(float)Math.PI;
		da = da/(2.0f*(float)Math.PI);

		if (dt>0) {
			rotation_speed = da/dt;
			rotation_accel = (rotation_speed - last_rotation_speed)/dt;
		}
		if ((rotation_accel!=0) && (state==TUIO_STOPPED)) state = TUIO_ROTATING;
	}
	
	/**
	 * Takes the atttibutes of the provided TuioObject 
	 * and assigs these values to this TuioObject.
	 * The TuioTime time stamp of this TuioContainer remains unchanged.
	 *
	 * @param	tobj	the TuioContainer to assign
	 */	
	public void update (TuioObject tobj) {
		super.update(tobj);
		angle = tobj.getAngle();
		rotation_speed = tobj.getRotationSpeed();
		rotation_accel = tobj.getRotationAccel();
		if ((rotation_accel!=0) && (state==TUIO_STOPPED)) state = TUIO_ROTATING;
	}
	
	/**
	 * This method is used to calculate the speed and acceleration values of a
	 * TuioObject with unchanged position and angle.
	 *
	 * @param	ttime	the TuioTime to assign
	 */
	public void stop (TuioTime ttime) {
		update(ttime,xpos,ypos, angle);
	}

	/**
	 * Returns the symbol ID of this TuioObject.
	 * @return	the symbol ID of this TuioObject
	 */
	public int getSymbolID() {
		return symbol_id;
	}

	/**
	 * Sets the Symbol ID of this TuioObject.
	 *
	 * @param	sym	the Symbol ID to assign
	 */
	public void setSymbolID(int sym) {
		symbol_id = sym;
	}
		
	/**
	 * Returns the rotation angle of this TuioObject.
	 * @return	the rotation angle of this TuioObject
	 */
	public float getAngle() {
		return angle;
	}

	/**
	 * Returns the rotation angle in degrees of this TuioObject.
	 * @return	the rotation angle in degrees of this TuioObject
	 */
	public float getAngleDegrees() {
		return angle/(float)Math.PI*180.0f;
	}
	
	/**
	 * Returns the rotation speed of this TuioObject.
	 * @return	the rotation speed of this TuioObject
	 */
	public float getRotationSpeed() {
		return rotation_speed;
	}
		
	/**
	 * Returns the rotation acceleration of this TuioObject.
	 * @return	the rotation acceleration of this TuioObject
	 */
	public float getRotationAccel() {
		return rotation_accel;
	}

	/**
	 * Returns true of this TuioObject is moving.
	 * @return	true of this TuioObject is moving
	 */
	public boolean isMoving() { 
		if ((state==TUIO_ACCELERATING) || (state==TUIO_DECELERATING) || (state==TUIO_ROTATING)) return true;
		else return false;
	}

	/**
	 * Adds an angle update threshold — angle changes smaller than
	 * this value are ignored on update.
	 *
	 * @param	thresh	the angle threshold to set
	 */
	public void addAngleThreshold(float thresh) {
		angleThreshold = thresh;
	}

	/**
	 * Removes the angle update threshold.
	 */
	public void removeAngleThreshold() {
		angleThreshold = 0.0f;
	}

	/**
	 * Adds a OneEuroFilter to the rotation angle of this TuioObject.
	 *
	 * @param	mcut	the min cutoff frequency parameter
	 * @param	beta	the beta parameter
	 */
	public void addAngleFilter(float mcut, float beta) {
		angleFilter = new OneEuroFilter(60.0, mcut, beta, 10.0);
	}

	/**
	 * Removes the OneEuroFilter from the rotation angle of this TuioObject.
	 */
	public void removeAngleFilter() {
		angleFilter = null;
	}

}
