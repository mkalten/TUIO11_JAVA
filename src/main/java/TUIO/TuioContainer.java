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

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * The abstract TuioContainer class defines common attributes that apply to both
 * subclasses {@link TuioObject} and {@link TuioCursor}.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.0
 */
abstract class TuioContainer extends TuioPoint {

    /**
     * The unique session ID number that is assigned to each TUIO object or
     * cursor.
     */
    protected long session_id;
    /**
     * The X-axis velocity value.
     */
    protected float x_speed;
    /**
     * The Y-axis velocity value.
     */
    protected float y_speed;
    /**
     * The motion speed value.
     */
    protected float motion_speed;
    /**
     * The motion acceleration value.
     */
    protected float motion_accel;
    /**
     * A Vector of TuioPoints containing all the previous positions of the TUIO
     * component.
     */
    protected LinkedBlockingDeque<TuioPoint> path;
    /**
     * Defines the maximum path length.
     */
    public static int MAX_PATH_LENGTH = 128;
    /**
     * Defines the ADDED state.
     */
    public static final int TUIO_ADDED = 0;
    /**
     * Defines the ACCELERATING state.
     */
    public static final int TUIO_ACCELERATING = 1;
    /**
     * Defines the DECELERATING state.
     */
    public static final int TUIO_DECELERATING = 2;
    /**
     * Defines the STOPPED state.
     */
    public static final int TUIO_STOPPED = 3;
    /**
     * Defines the REMOVED state.
     */
    public static final int TUIO_REMOVED = 4;
    /**
     * Reflects the current state of the TuioComponent
     */
    protected int state;

    /**
     * This constructor takes a TuioTime argument and assigns it along with the
     * provided Session ID, X and Y coordinate to the newly created
     * TuioContainer.
     *
     * @param	ttime	the TuioTime to assign
     * @param	si	the Session ID to assign
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     */
    TuioContainer(TuioTime ttime, long si, float xp, float yp) {
        super(ttime, xp, yp);

        session_id = si;
        x_speed = 0.0f;
        y_speed = 0.0f;
        motion_speed = 0.0f;
        motion_accel = 0.0f;

        path = new LinkedBlockingDeque<TuioPoint>();
        path.addLast(new TuioPoint(currentTime, xpos, ypos));
        state = TUIO_ADDED;
    }

    /**
     * This constructor takes the provided Session ID, X and Y coordinate and
     * assigns these values to the newly created TuioContainer.
     *
     * @param	si	the Session ID to assign
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     */
    TuioContainer(long si, float xp, float yp) {
        super(xp, yp);

        session_id = si;
        x_speed = 0.0f;
        y_speed = 0.0f;
        motion_speed = 0.0f;
        motion_accel = 0.0f;

        path = new LinkedBlockingDeque<TuioPoint>();
        path.addLast(new TuioPoint(currentTime, xpos, ypos));
        state = TUIO_ADDED;
    }

    /**
     * This constructor takes the attributes of the provided TuioContainer and
     * assigns these values to the newly created TuioContainer.
     *
     * @param	tcon	the TuioContainer to assign
     */
    TuioContainer(TuioContainer tcon) {
        super(tcon);

        session_id = tcon.getSessionID();
        x_speed = 0.0f;
        y_speed = 0.0f;
        motion_speed = 0.0f;
        motion_accel = 0.0f;

        path = new LinkedBlockingDeque<TuioPoint>();
        path.addLast(new TuioPoint(currentTime, xpos, ypos));
        state = TUIO_ADDED;
    }

    /**
     * Takes a TuioTime argument and assigns it along with the provided X and Y
     * coordinate to the private TuioContainer attributes. The speed and
     * acceleration values are calculated accordingly.
     *
     * @param	ttime	the TuioTime to assign
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     */
    @Override
    public void update(TuioTime ttime, float xp, float yp) {
        TuioPoint lastPoint = path.getLast();
        super.update(ttime, xp, yp);

        TuioTime diffTime = currentTime.subtract(lastPoint.getTuioTime());
        float dt = diffTime.getTotalMilliseconds() / 1000.0f;
        float dx = this.xpos - lastPoint.getX();
        float dy = this.ypos - lastPoint.getY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float last_motion_speed = this.motion_speed;

        this.x_speed = dx / dt;
        this.y_speed = dy / dt;
        this.motion_speed = dist / dt;
        this.motion_accel = (motion_speed - last_motion_speed) / dt;

        path.addLast(new TuioPoint(currentTime, xpos, ypos));
        if (path.size() > MAX_PATH_LENGTH) {
            path.removeFirst();
        }

        if (motion_accel > 0) {
            state = TUIO_ACCELERATING;
        } else if (motion_accel < 0) {
            state = TUIO_DECELERATING;
        } else {
            state = TUIO_STOPPED;
        }
    }

    /**
     * This method is used to calculate the speed and acceleration values of
     * TuioContainers with unchanged positions.
     *
     * @param	ttime	the TuioTime to assign
     */
    public void stop(TuioTime ttime) {
        update(ttime, xpos, ypos);
    }

    /**
     * Takes a TuioTime argument and assigns it along with the provided X and Y
     * coordinate, X and Y velocity and acceleration to the private
     * TuioContainer attributes.
     *
     * @param	ttime	the TuioTime to assign
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     * @param	xs	the X velocity to assign
     * @param	ys	the Y velocity to assign
     * @param	ma	the acceleration to assign
     */
    public void update(TuioTime ttime, float xp, float yp, float xs, float ys, float ma) {
        super.update(ttime, xp, yp);
        x_speed = xs;
        y_speed = ys;
        motion_speed = (float) Math.sqrt(x_speed * x_speed + y_speed * y_speed);
        motion_accel = ma;

        path.addLast(new TuioPoint(currentTime, xpos, ypos));
        if (path.size() > MAX_PATH_LENGTH) {
            path.removeFirst();
        }

        if (motion_accel > 0) {
            state = TUIO_ACCELERATING;
        } else if (motion_accel < 0) {
            state = TUIO_DECELERATING;
        } else {
            state = TUIO_STOPPED;
        }
    }

    /**
     * Assigns the provided X and Y coordinate, X and Y velocity and
     * acceleration to the private TuioContainer attributes. The TuioTime time
     * stamp remains unchanged.
     *
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     * @param	xs	the X velocity to assign
     * @param	ys	the Y velocity to assign
     * @param	ma	the acceleration to assign
     */
    public void update(float xp, float yp, float xs, float ys, float ma) {
        super.update(xp, yp);
        x_speed = xs;
        y_speed = ys;
        motion_speed = (float) Math.sqrt(x_speed * x_speed + y_speed * y_speed);
        motion_accel = ma;

        path.addLast(new TuioPoint(currentTime, xpos, ypos));
        if (path.size() > MAX_PATH_LENGTH) {
            path.removeFirst();
        }

        if (motion_accel > 0) {
            state = TUIO_ACCELERATING;
        } else if (motion_accel < 0) {
            state = TUIO_DECELERATING;
        } else {
            state = TUIO_STOPPED;
        }
    }

    /**
     * Takes the attributes of the provided TuioContainer and assigns these
     * values to this TuioContainer. The TuioTime time stamp of this
     * TuioContainer remains unchanged.
     *
     * @param	tcon	the TuioContainer to assign
     */
    public void update(TuioContainer tcon) {
        super.update(tcon);
        x_speed = tcon.getXSpeed();
        y_speed = tcon.getYSpeed();
        motion_speed = tcon.getMotionSpeed();
        motion_accel = tcon.getMotionAccel();

        path.addLast(new TuioPoint(currentTime, xpos, ypos));
        if (path.size() > MAX_PATH_LENGTH) {
            path.removeFirst();
        }

        if (motion_accel > 0) {
            state = TUIO_ACCELERATING;
        } else if (motion_accel < 0) {
            state = TUIO_DECELERATING;
        } else {
            state = TUIO_STOPPED;
        }
    }

    /**
     * Assigns the REMOVE state to this TuioContainer and sets its TuioTime time
     * stamp to the provided TuioTime argument.
     *
     * @param	ttime	the TuioTime to assign
     */
    public void remove(TuioTime ttime) {
        currentTime = new TuioTime(ttime);
        state = TUIO_REMOVED;
    }

    /**
     * Returns the Session ID of this TuioContainer.
     *
     * @return	the Session ID of this TuioContainer
     */
    public long getSessionID() {
        return session_id;
    }

    /**
     * Set the Session ID.  Only if the id is currently equal-to 0.
     *
     * @param value
     */
    public void setSessionID(long value) {
        if (session_id == 0) {
            session_id = value;
        }
    }

    /**
     * Returns the X velocity of this TuioContainer.
     *
     * @return	the X velocity of this TuioContainer
     */
    public float getXSpeed() {
        return x_speed;
    }

    /**
     * Set the X Speed.
     *
     * @param value
     */
    public void setXSpeed(float value) {
        x_speed = value;
    }

    /**
     * Returns the Y velocity of this TuioContainer.
     *
     * @return	the Y velocity of this TuioContainer
     */
    public float getYSpeed() {
        return y_speed;
    }

    /**
     * Set the Y Speed.
     *
     * @param value
     */
    public void setYSpeed(float value) {
        y_speed = value;
    }

    /**
     * Returns the position of this TuioContainer.
     *
     * @return	the position of this TuioContainer
     */
    public TuioPoint getPosition() {
        return new TuioPoint(xpos, ypos);
    }

    /**
     * Set the Position.
     *
     * @param value
     */
    public void setPosition(TuioPoint value) {
        xpos = value.xpos;
        ypos = value.ypos;
    }

    /**
     * Returns the path of this TuioContainer.
     *
     * @return	the path of this TuioContainer
     */
    public ArrayList<TuioPoint> getPath() {
        return new ArrayList<TuioPoint>(path);
    }

    /**
     * Set the Path.
     *
     * @param value
     */
    public void setPath(ArrayList<TuioPoint> value) {
        path.clear();
        path.addAll(value);
    }

    /**
     * Sets the maximum path length parameter the maximum path length
     */
    public static void setMaxPathLength(int length) {
        MAX_PATH_LENGTH = length;
    }

    /**
     * Returns the motion speed of this TuioContainer.
     *
     * @return	the motion speed of this TuioContainer
     */
    public float getMotionSpeed() {
        return motion_speed;
    }

    /**
     * Set the Motion Speed.
     *
     * @param value
     */
    public void setMotionSpeed(float value) {
        motion_speed = value;
    }

    /**
     * Returns the motion acceleration of this TuioContainer.
     *
     * @return	the motion acceleration of this TuioContainer
     */
    public float getMotionAccel() {
        return motion_accel;
    }

    /**
     * Set the Motion Acceleration.
     *
     * @param value
     */
    public void setMotionAccel(float value) {
        motion_accel = value;
    }

    /**
     * Returns the TUIO state of this TuioContainer.
     *
     * @return	the TUIO state of this TuioContainer
     */
    public int getTuioState() {
        return state;
    }

    /**
     * Set the TUIO State.
     *
     * @param value
     */
    public void setTuioState(int value) {
        state = value;
    }

    /**
     * Returns true of this TuioContainer is moving.
     *
     * @return	true of this TuioContainer is moving
     */
    public boolean isMoving() {
        return (state == TUIO_ACCELERATING) || (state == TUIO_DECELERATING);
    }

}
