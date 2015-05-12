/**
 * TUIO Java Console Example
 * Copyright (c) 2005-2014 Martin Kaltenbrunner <martin@tuio.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package TUIO;

import java.io.Serializable;

/**
 * The TuioTime class is a simple structure that is used to represent the time
 * that has elapsed since the session start. The time is internally represented
 * as seconds and fractions of microseconds which should be more than sufficient
 * for gesture related timing requirements. Therefore at the beginning of a
 * typical TUIO session the static method initSession() will set the reference
 * time for the session. Another important static method getSessionTime will
 * return a TuioTime object representing the time elapsed since the session
 * start. The class also provides various additional convenience method, which
 * allow some simple time arithmetics.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.0
 */
public class TuioTime implements Serializable{

    /**
     * the time since session start in seconds
     */
    private long seconds = 0;
    /**
     * time fraction in microseconds
     */
    private long micro_seconds = 0;
    /**
     * the session start time in seconds
     */
    private static long start_seconds = 0;
    /**
     * start time fraction in microseconds
     */
    private static long start_micro_seconds = 0;
    /**
     * the associated frame ID
     */
    private long frame_id = 0;

    /**
     * The default constructor takes no arguments and sets the Seconds and
     * Microseconds attributes of the newly created TuioTime both to zero.
     */
    public TuioTime() {
        this.seconds = 0;
        this.micro_seconds = 0;
    }

    /**
     * This constructor takes the provided time represented in total
     * Milliseconds and assigns this value to the newly created TuioTime.
     *
     * @param msec the total time in Milliseconds
     */
    public TuioTime(long msec) {
        this.seconds = msec / 1000;
        this.micro_seconds = 1000 * (msec % 1000);
    }

    /**
     * This constructor takes the provided time represented in Seconds and
     * Microseconds and assigns these value to the newly created TuioTime.
     *
     * @param sec the total time in seconds
     * @param usec	the microseconds time component
     */
    public TuioTime(long sec, long usec) {
        this.seconds = sec;
        this.micro_seconds = usec;
    }

    /**
     * This constructor takes the provided TuioTime and assigns its Seconds and
     * Microseconds values to the newly created TuioTime.
     *
     * @param ttime the TuioTime used to copy
     */
    public TuioTime(TuioTime ttime) {
        this.seconds = ttime.getSeconds();
        this.micro_seconds = ttime.getMicroseconds();
    }

    /**
     * This constructor takes the provided TuioTime and assigns its Seconds and
     * Microseconds values to the newly created TuioTime.
     *
     * @param ttime the TuioTime used to copy
     * @param f_id
     */
    public TuioTime(TuioTime ttime, long f_id) {
        this.seconds = ttime.getSeconds();
        this.micro_seconds = ttime.getMicroseconds();
        this.frame_id = f_id;
    }

    /**
     * Sums the provided time value represented in total Microseconds to this
     * TuioTime.
     *
     * @param us	the total time to add in Microseconds
     * @return the sum of this TuioTime with the provided argument in
     * microseconds
     */
    public TuioTime add(long us) {
        long sec = seconds + us / 1000000;
        long usec = micro_seconds + us % 1000000;
        return new TuioTime(sec, usec);
    }

    /**
     * Sums the provided TuioTime to the private Seconds and Microseconds
     * attributes.
     *
     * @param ttime	the TuioTime to add
     * @return the sum of this TuioTime with the provided TuioTime argument
     */
    public TuioTime add(TuioTime ttime) {
        long sec = seconds + ttime.getSeconds();
        long usec = micro_seconds + ttime.getMicroseconds();
        sec += usec / 1000000;
        usec = usec % 1000000;
        return new TuioTime(sec, usec);
    }

    /**
     * Subtracts the provided time represented in Microseconds from the private
     * Seconds and Microseconds attributes.
     *
     * @param us	the total time to subtract in Microseconds
     * @return the subtraction result of this TuioTime minus the provided time
     * in Microseconds
     */
    public TuioTime subtract(long us) {
        long sec = seconds - us / 1000000;
        long usec = micro_seconds - us % 1000000;

        if (usec < 0) {
            usec += 1000000;
            sec--;
        }

        return new TuioTime(sec, usec);
    }

    /**
     * Subtracts the provided TuioTime from the private Seconds and Microseconds
     * attributes.
     *
     * @param ttime	the TuioTime to subtract
     * @return the subtraction result of this TuioTime minus the provided
     * TuioTime
     */
    public TuioTime subtract(TuioTime ttime) {
        long sec = seconds - ttime.getSeconds();
        long usec = micro_seconds - ttime.getMicroseconds();

        if (usec < 0) {
            usec += 1000000;
            sec--;
        }

        return new TuioTime(sec, usec);
    }

    /**
     * Takes a TuioTime argument and compares the provided TuioTime to the
     * private Seconds and Microseconds attributes.
     *
     * @param ttime	the TuioTime to compare
     * @return true if the two TuioTime have equal Seconds and Microseconds
     * attributes
     */
    public boolean equals(TuioTime ttime) {
        return (seconds == ttime.getSeconds()) && (micro_seconds == ttime.getMicroseconds());
    }

    /**
     * Resets the seconds and micro_seconds attributes to zero.
     */
    public void reset() {
        seconds = 0;
        micro_seconds = 0;
    }

    /**
     * Returns the TuioTime Seconds component.
     *
     * @return the TuioTime Seconds component
     */
    public long getSeconds() {
        return seconds;
    }
    
    /**
     * Set the seconds.
     *
     * @param value
     */
    public void setSeconds(long value) {
        seconds = value;
    }

    /**
     * Returns the TuioTime Microseconds component.
     *
     * @return the TuioTime Microseconds component
     */
    public long getMicroseconds() {
        return micro_seconds;
    }
    
    /**
     * Set the micro seconds.
     *
     * @param value
     */
    public void setMicroseconds(long value) {
        micro_seconds = value;
    }

    /**
     * Returns the total TuioTime in Milliseconds.
     *
     * @return the total TuioTime in Milliseconds
     */
    public long getTotalMilliseconds() {
        return seconds * 1000 + micro_seconds / 1000;
    }

    /**
     * This static method globally resets the TUIO session time.
     */
    public static void initSession() {
        TuioTime startTime = getSystemTime();
        start_seconds = startTime.getSeconds();
        start_micro_seconds = startTime.getMicroseconds();
    }

    /**
     * Returns the present TuioTime representing the time since session start.
     *
     * @return the present TuioTime representing the time since session start
     */
    public static TuioTime getSessionTime() {
        TuioTime sessionTime = getSystemTime().subtract(getStartTime());
        return sessionTime;

    }

    /**
     * Returns the absolute TuioTime representing the session start.
     *
     * @return the absolute TuioTime representing the session start
     */
    public static TuioTime getStartTime() {
        return new TuioTime(start_seconds, start_micro_seconds);
    }

    /**
     * Returns the absolute TuioTime representing the current system time.
     *
     * @return the absolute TuioTime representing the current system time
     */
    public static TuioTime getSystemTime() {
        long usec = System.nanoTime() / 1000;
        return new TuioTime(usec / 1000000, usec % 1000000);
    }

    /**
     * associates a Frame ID to this TuioTime.
     *
     * @param f_id	the Frame ID to associate
     */
    public void setFrameID(long f_id) {
        frame_id = f_id;
    }

    /**
     * Returns the Frame ID associated to this TuioTime.
     *
     * @return the Frame ID associated to this TuioTime
     */
    public long getFrameID() {
        return frame_id;
    }

}
