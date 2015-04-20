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
 */
package TUIO;

/**
 * The TuioBlob class encapsulates /tuio/2Dblb TUIO blobs.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.0
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
    protected float width;
    /**
     * The height value.
     */
    protected float height;
    /**
     * The area value.
     */
    protected float area;
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
     * This constructor takes a TuioTime argument and assigns it along with the
     * provided Session ID, X and Y coordinate, width, height and angle to the
     * newly created TuioBlob.
     *
     * @param	ttime	the TuioTime to assign
     * @param	si	the Session ID to assign
     * @param   bi
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     * @param	a	the angle to assign
     * @param	w	the width to assign
     * @param	h	the height to assign
     * @param	f	the area to assign
     */
    protected TuioBlob(TuioTime ttime, long si, int bi, float xp, float yp, float a, float w, float h, float f) {
        super(ttime, si, xp, yp);
        blob_id = bi;
        angle = a;
        width = w;
        height = h;
        area = f;
        rotation_speed = 0.0f;
        rotation_accel = 0.0f;
    }

    /**
     * This constructor takes the provided Session ID, Blob ID, X and Y
     * coordinate and angle, and assigns these values to the newly created
     * TuioBlob.
     *
     * @param	si	the Session ID to assign
     * @param	blb	the Blob ID to assign
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     * @param	a	the angle to assign
     * @param	w	the width to assign
     * @param	h	the height to assign
     * @param	f	the area to assign
     */
    public TuioBlob(long si, int blb, float xp, float yp, float a, float w, float h, float f) {
        super(si, xp, yp);
        blob_id = blb;
        angle = a;
        width = w;
        height = h;
        area = f;
        rotation_speed = 0.0f;
        rotation_accel = 0.0f;
    }

    /**
     * This constructor takes the attributes of the provided TuioBlob and assigns
     * these values to the newly created TuioBlob.
     *
     * @param	tblb	the TuioBlob to assign
     */
    public TuioBlob(TuioBlob tblb) {
        super(tblb);
        blob_id = tblb.getBlobID();
        angle = tblb.getAngle();
        width = tblb.getWidth();
        height = tblb.getHeight();
        area = tblb.getArea();
        rotation_speed = 0.0f;
        rotation_accel = 0.0f;
    }

    /**
     * Takes a TuioTime argument and assigns it along with the provided X and Y
     * coordinate, angle, X and Y velocity, motion acceleration, rotation speed
     * and rotation acceleration to the private TuioBlob attributes.
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
    public void update(TuioTime ttime, float xp, float yp, float a, float w, float h, float f, float xs, float ys, float rs, float ma, float ra) {
        super.update(ttime, xp, yp, xs, ys, ma);
        angle = a;
        width = w;
        height = h;
        area = f;
        rotation_speed = rs;
        rotation_accel = ra;
        if ((rotation_accel != 0) && (state != TUIO_STOPPED)) {
            state = TUIO_ROTATING;
        }
    }

    /**
     * Takes a TuioTime argument and assigns it along with the provided X and Y
     * coordinate, angle, X and Y velocity, motion acceleration, rotation speed
     * and rotation acceleration to the private TuioBlob attributes.
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
    public void update(float xp, float yp, float a, float w, float h, float f, float xs, float ys, float rs, float ma, float ra) {
        super.update(xp, yp, xs, ys, ma);
        angle = a;
        width = w;
        height = h;
        area = f;
        rotation_speed = rs;
        rotation_accel = ra;
        if ((rotation_accel != 0) && (state != TUIO_STOPPED)) {
            state = TUIO_ROTATING;
        }
    }

    /**
     * Takes a TuioTime argument and assigns it along with the provided X and Y
     * coordinate and angle to the private TuioBlob attributes. The speed and
     * acceleration values are calculated accordingly.
     *
     * @param	ttime	the TuioTime to assign
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     * @param	a	the angle coordinate to assign
     * @param	w	the width to assign
     * @param	h	the height to assign
     * @param	f	the area to assign
     */
    public void update(TuioTime ttime, float xp, float yp, float a, float w, float h, float f) {
        TuioPoint lastPoint = path.getLast();
        super.update(ttime, xp, yp);

        width = w;
        height = h;
        area = f;

        TuioTime diffTime = currentTime.subtract(lastPoint.getTuioTime());
        float dt = diffTime.getTotalMilliseconds() / 1000.0f;
        float last_angle = angle;
        float last_rotation_speed = rotation_speed;
        angle = a;

        float da = (this.angle - last_angle) / (2.0f * (float) Math.PI);
        if (da > 0.75f) {
            da -= 1.0f;
        } else if (da < -0.75f) {
            da += 1.0f;
        }

        rotation_speed = da / dt;
        rotation_accel = (rotation_speed - last_rotation_speed) / dt;
        if ((rotation_accel != 0) && (state != TUIO_STOPPED)) {
            state = TUIO_ROTATING;
        }
    }

    /**
     * Takes the attributes of the provided TuioBlob and assigns these values to
     * this TuioBlob. The TuioTime time stamp of this TuioContainer remains
     * unchanged.
     *
     * @param	tblb	the TuioContainer to assign
     */
    public void update(TuioBlob tblb) {
        super.update(tblb);
        angle = tblb.getAngle();
        width = tblb.getWidth();
        height = tblb.getHeight();
        area = tblb.getArea();
        rotation_speed = tblb.getRotationSpeed();
        rotation_accel = tblb.getRotationAccel();
        if ((rotation_accel != 0) && (state != TUIO_STOPPED)) {
            state = TUIO_ROTATING;
        }
    }

    /**
     * This method is used to calculate the speed and acceleration values of a
     * TuioBlob with unchanged position and angle.
     *
     * @param	ttime	the TuioTime to assign
     */
    @Override
    public void stop(TuioTime ttime) {
        update(ttime, xpos, ypos, angle, width, height, area);
    }

    /**
     * Returns the Blob ID of this TuioBlob.
     *
     * @return	the Blob ID of this TuioBlob
     */
    public int getBlobID() {
        return blob_id;
    }

    /**
     * Returns the width of this TuioBlob.
     *
     * @return	the width of this TuioBlob
     */
    public float getWidth() {
        return width;
    }

    /**
     * Returns the height of this TuioBlob.
     *
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
        return (int) (width * w);
    }

    /**
     * Returns the screen height of this TuioBlob.
     *
     * @param	h	the full screen height in pixels
     * @return	the screen height of this TuioBlob
     */
    public int getScreenHeight(int h) {
        return (int) (height * h);
    }

    /**
     * Returns the area of this TuioBlob.
     *
     * @return	the area of this TuioBlob
     */
    public float getArea() {
        return area;
    }

    /**
     * Returns the rotation angle of this TuioBlob.
     *
     * @return	the rotation angle of this TuioBlob
     */
    public float getAngle() {
        return angle;
    }

    /**
     * Returns the rotation angle in degrees of this TuioBlob.
     *
     * @return	the rotation angle in degrees of this TuioBlob
     */
    public float getAngleDegrees() {
        return angle / (float) Math.PI * 180.0f;
    }

    /**
     * Returns the rotation speed of this TuioBlob.
     *
     * @return	the rotation speed of this TuioBlob
     */
    public float getRotationSpeed() {
        return rotation_speed;
    }

    /**
     * Returns the rotation acceleration of this TuioBlob.
     *
     * @return	the rotation acceleration of this TuioBlob
     */
    public float getRotationAccel() {
        return rotation_accel;
    }

    /**
     * Returns true of this TuioBlob is moving.
     *
     * @return	true of this TuioBlob is moving
     */
    @Override
    public boolean isMoving() {
        return (state == TUIO_ACCELERATING) || (state == TUIO_DECELERATING) || (state == TUIO_ROTATING);
    }

}
