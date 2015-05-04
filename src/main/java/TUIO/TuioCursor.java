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

/**
 * The TuioCursor class encapsulates /tuio/2Dcur TUIO cursors.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.0
 */
public class TuioCursor extends TuioContainer {

    /**
     * The individual cursor ID number that is assigned to each TuioCursor.
     */
    protected int cursor_id;

    /**
     * Default constructor. Initialize everything to 0.
     */
    public TuioCursor() {
        super(0, 0, 0);
    }
    
    /**
     * This constructor takes a TuioTime argument and assigns it along with the
     * provided Session ID, Cursor ID, X and Y coordinate to the newly created
     * TuioCursor.
     *
     * @param	ttime	the TuioTime to assign
     * @param	si	the Session ID to assign
     * @param	ci	the Cursor ID to assign
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     */
    public TuioCursor(TuioTime ttime, long si, int ci, float xp, float yp) {
        super(ttime, si, xp, yp);
        this.cursor_id = ci;
    }

    /**
     * This constructor takes the provided Session ID, Cursor ID, X and Y
     * coordinate and assigns these values to the newly created TuioCursor.
     *
     * @param	si	the Session ID to assign
     * @param	ci	the Cursor ID to assign
     * @param	xp	the X coordinate to assign
     * @param	yp	the Y coordinate to assign
     */
    public TuioCursor(long si, int ci, float xp, float yp) {
        super(si, xp, yp);
        this.cursor_id = ci;
    }

    /**
     * This constructor takes the attributes of the provided TuioCursor and
     * assigns these values to the newly created TuioCursor.
     *
     * @param	tcur	the TuioCursor to assign
     */
    public TuioCursor(TuioCursor tcur) {
        super(tcur);
        this.cursor_id = tcur.getCursorID();
    }

    /**
     * Returns the Cursor ID of this TuioCursor.
     *
     * @return	the Cursor ID of this TuioCursor
     */
    public int getCursorID() {
        return cursor_id;
    }

    /**
     * Set the cursor id.  Will only set the value if it is equal-to 0.
     *
     * @param value
     */
    public void setCursorID(int value) {
        if (cursor_id == 0) {
            cursor_id = value;
        }
    }
}
