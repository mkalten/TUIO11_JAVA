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
 * The TuioBlob class encapsulates /tuio/2Dblb TUIO blobs.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */ 
public class TuioBlob extends TuioContainer {
	
	/**
	 * The individual blob ID number that is assigned to each TuioBlob.
	 */ 
	protected int blob_id;
	/**
	 * The rotation angle value.
	 */ 
	protected float angle;
	/**
	 * The width value.
	 */ 
	float width;
	/**
	 * The height value.
	 */ 
	float height;
	/**
	 * The area value.
	 */ 
	float area;
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
	 * Optional OneEuroFilter for the width smoothing
	 */
	protected OneEuroFilter widthFilter;
	/**
	 * Optional OneEuroFilter for the height smoothing
	 */
	protected OneEuroFilter heightFilter;
	/**
	 * Angle update threshold — small angle changes below this value are ignored
	 */
	protected float angleThreshold = 0.0f;
	/**
	 * Size update threshold — small width/height changes below this value are ignored
	 */
	protected float sizeThreshold = 0.0f;
	
	/**
	 * This constructor takes a TuioTime argument and assigns it along with the provided 
	 * Session ID, X and Y coordinate, width, height and angle to the newly created TuioBlob.
	 *
	 * @param	ttime	the TuioTime to assign
	 * @param	si	the Session ID  to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the angle to assign
	 * @param	w	the width to assign
	 * @param	h	the height to assign
	 * @param	f	the area to assign
	 */
	TuioBlob (TuioTime ttime, long si, int bi, float xp, float yp, float a, float w, float h, float f) {
	super(ttime, si,xp,yp);
		blob_id = bi;
		angle = a;
		width = w;
		height = h;
		area = f;
		rotation_speed = 0.0f;
		rotation_accel = 0.0f;
	}

	/**
	 * This constructor takes the provided Session ID, Blob ID, X and Y coordinate 
	 * and angle, and assigs these values to the newly created TuioBlob.
	 *
	 * @param	si	the Session ID  to assign
	 * @param	blb	the Blob ID  to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the angle to assign
	 * @param	w	the width to assign
	 * @param	h	the height to assign
	 * @param	f	the area to assign
	 */	
	public TuioBlob (long si, int blb, float xp, float yp, float a, float w, float h, float f) {
		super(si,xp,yp);
		blob_id = blb;
		angle = a;
		width = w;
		height = h;
		area = f;
		rotation_speed = 0.0f;
		rotation_accel = 0.0f;
	}

	/**
	 * This constructor takes the atttibutes of the provided TuioBlob 
	 * and assigs these values to the newly created TuioBlob.
	 *
	 * @param	tblb	the TuioBlob to assign
	 */
	public TuioBlob (TuioBlob tblb) {
		super(tblb);
		blob_id = tblb.blob_id;
		angle = tblb.angle;
		width = tblb.width;
		height = tblb.height;
		area = tblb.area;
		rotation_speed = tblb.rotation_speed;
		rotation_accel = tblb.rotation_accel;
		angleFilter = tblb.angleFilter != null ? new OneEuroFilter(tblb.angleFilter) : null;
		widthFilter = tblb.widthFilter != null ? new OneEuroFilter(tblb.widthFilter) : null;
		heightFilter = tblb.heightFilter != null ? new OneEuroFilter(tblb.heightFilter) : null;
		angleThreshold = tblb.angleThreshold;
		sizeThreshold = tblb.sizeThreshold;
	}

	/**
	 * Takes a TuioTime argument and assigns it along with the provided 
	 * X and Y coordinate, angle, X and Y velocity, motion acceleration,
	 * rotation speed and rotation acceleration to the private TuioBlob attributes.
	 *
	 * @param	ttime	the TuioTime to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the angle coordinate to assign
	 * @param	w	the width to assign
	 * @param	h	the height to assign
	 * @param	f	the area to assign
	 * @param	xs	the X velocity to assign
	 * @param	ys	the Y velocity to assign
	 * @param	rs	the rotation velocity to assign
	 * @param	ma	the motion acceleration to assign
	 * @param	ra	the rotation acceleration to assign
	 */
	public void update (TuioTime ttime, float xp, float yp, float a, float w, float h, float f, float xs, float ys, float rs, float ma, float ra) {
		super.update(ttime,xp,yp,xs,ys,ma);
		angle = a;
		width = w;
		height = h;
		area = f;
		rotation_speed = rs;
		rotation_accel = ra;
		if ((rotation_accel!=0) && (state==TUIO_STOPPED)) state = TUIO_ROTATING;
	}

	/**
	 * Takes a TuioTime argument and assigns it along with the provided 
	 * X and Y coordinate, angle, X and Y velocity, motion acceleration,
	 * rotation speed and rotation acceleration to the private TuioBlob attributes.
	 *
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the rotation angle to assign
	 * @param	w	the width to assign
	 * @param	h	the height to assign
	 * @param	f	the area to assign
	 * @param	xs	the X velocity to assign
	 * @param	ys	the Y velocity to assign
	 * @param	rs	the rotation velocity to assign
	 * @param	ma	the motion acceleration to assign
	 * @param	ra	the rotation acceleration to assign
	 */
	public void update (float xp, float yp, float a, float w, float h, float f, float xs, float ys, float rs, float ma, float ra) {
		super.update(xp,yp,xs,ys,ma);
		angle = a;
		width = w;
		height = h;
		area = f;
		rotation_speed = rs;
		rotation_accel = ra;
		if ((rotation_accel!=0) && (state==TUIO_STOPPED)) state = TUIO_ROTATING;
	}
	
	/**
	 * Takes a TuioTime argument and assigns it along with the provided 
	 * X and Y coordinate and angle to the private TuioBlob attributes.
	 * The speed and accleration values are calculated accordingly.
	 *
	 * @param	ttime	the TuioTime to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 * @param	a	the angle coordinate to assign
	 * @param	w	the width to assign
	 * @param	h	the height to assign
	 * @param	f	the area to assign
	 */
	public void update (TuioTime ttime, float xp, float yp, float a, float w, float h, float f) {
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

		if (widthFilter != null && heightFilter != null) {
			w = (float)widthFilter.filter(w,dt);
			h = (float)heightFilter.filter(h,dt);
		}

		float dw = Math.abs(width - w);
		float dh = Math.abs(height - h);
		if ((dw>sizeThreshold) || (dh>sizeThreshold)) {
			width = w;
			height = h;
		}

		area = f;

		if (dt>0) {
			rotation_speed = da/dt;
			rotation_accel = (rotation_speed - last_rotation_speed)/dt;
		}
		if ((rotation_accel!=0) && (state==TUIO_STOPPED)) state = TUIO_ROTATING;
	}
	
	/**
	 * Takes the atttibutes of the provided TuioBlob 
	 * and assigs these values to this TuioBlob.
	 * The TuioTime time stamp of this TuioContainer remains unchanged.
	 *
	 * @param	tblb	the TuioContainer to assign
	 */	
	public void update (TuioBlob tblb) {
		super.update(tblb);
		angle = tblb.getAngle();
		width = tblb.getWidth();
		height = tblb.getHeight();
		area = tblb.getArea();
		rotation_speed = tblb.getRotationSpeed();
		rotation_accel = tblb.getRotationAccel();
		if ((rotation_accel!=0) && (state==TUIO_STOPPED)) state = TUIO_ROTATING;
	}
	
	/**
	 * This method is used to calculate the speed and acceleration values of a
	 * TuioBlob with unchanged position and angle.
	 *
	 * @param	ttime	the TuioTime to assign
	 */
	public void stop (TuioTime ttime) {
		update(ttime,xpos,ypos,angle,width,height,area);
	}

	/**
	 * Returns the Blob ID of this TuioBlob.
	 * @return	the Blob ID of this TuioBlob
	 */
	public int getBlobID() {
		return blob_id;
	}

	/**
	 * Sets the Blob ID of this TuioBlob.
	 *
	 * @param	bi	the Blob ID to assign
	 */
	public void setBlobID(int bi) {
		blob_id = bi;
	}
	
	/**
	 * Returns the width of this TuioBlob.
	 * @return	the width of this TuioBlob
	 */
	public float getWidth() {
		return width;
	}
	
	/**
	 * Returns the height of this TuioBlob.
	 * @return	the height of this TuioBlob
	 */
	public float getHeight() {
		return height;
	}	
	
	/**
	 * Returns the screen width of this TuioBlob.
	 *
	 * @param	w	the full screen width in pixels
	 * @return	the screen width of this TuioBlob
	 */
	public int getScreenWidth(int w) {
		return (int)(width*w);
	}	
	
	/**
	 * Returns the screen height of this TuioBlob.
	 *
	 * @param	h	the full screen height in pixels
	 * @return	the screen height of this TuioBlob
	 */
	public int getScreenHeight(int h) {
		return (int)(height*h);
	}	
	
	/**
	 * Returns the area of this TuioBlob.
	 * @return	the area of this TuioBlob
	 */
	public float getArea() {
		return area;
	}
	
	/**
	 * Returns the rotation angle of this TuioBlob.
	 * @return	the rotation angle of this TuioBlob
	 */
	public float getAngle() {
		return angle;
	}

	/**
	 * Returns the rotation angle in degrees of this TuioBlob.
	 * @return	the rotation angle in degrees of this TuioBlob
	 */
	public float getAngleDegrees() {
		return angle/(float)Math.PI*180.0f;
	}
	
	/**
	 * Returns the rotation speed of this TuioBlob.
	 * @return	the rotation speed of this TuioBlob
	 */
	public float getRotationSpeed() {
		return rotation_speed;
	}
		
	/**
	 * Returns the rotation acceleration of this TuioBlob.
	 * @return	the rotation acceleration of this TuioBlob
	 */
	public float getRotationAccel() {
		return rotation_accel;
	}

	/**
	 * Returns true of this TuioBlob is moving.
	 * @return	true of this TuioBlob is moving
	 */
	public boolean isMoving() { 
		if ((state==TUIO_ACCELERATING) || (state==TUIO_DECELERATING) || (state==TUIO_ROTATING)) return true;
		else return false;
	}

	/**
	 * Adds a OneEuroFilter to the rotation angle of this TuioBlob,
	 * smoothing small angle changes while minimizing lag.
	 *
	 * @param	mcut	the minimal cutoff frequency
	 * @param	beta	the speed coefficient
	 */
	public void addAngleFilter(float mcut, float beta) {
		angleFilter = new OneEuroFilter(60.0, mcut, beta, 10.0);
	}

	/**
	 * Removes the OneEuroFilter from the rotation angle of this TuioBlob.
	 */
	public void removeAngleFilter() {
		angleFilter = null;
	}

	/**
	 * Adds a OneEuroFilter to the width and height of this TuioBlob,
	 * smoothing small size changes while minimizing lag.
	 *
	 * @param	mcut	the minimal cutoff frequency
	 * @param	beta	the speed coefficient
	 */
	public void addSizeFilter(float mcut, float beta) {
		widthFilter = new OneEuroFilter(60.0, mcut, beta, 10.0);
		heightFilter = new OneEuroFilter(60.0, mcut, beta, 10.0);
	}

	/**
	 * Removes the OneEuroFilter from the width and height of this TuioBlob.
	 */
	public void removeSizeFilter() {
		widthFilter = null;
		heightFilter = null;
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
	 * Adds a size update threshold — width and height changes smaller than
	 * this value are ignored on update.
	 *
	 * @param	thresh	the size threshold to set
	 */
	public void addSizeThreshold(float thresh) {
		sizeThreshold = thresh;
	}

	/**
	 * Removes the size update threshold.
	 */
	public void removeSizeThreshold() {
		sizeThreshold = 0.0f;
	}

}
