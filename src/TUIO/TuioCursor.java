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
 * The TuioCursor class encapsulates /tuio/2Dcur TUIO cursors.
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.6
 */ 
public class TuioCursor extends TuioContainer {

	/**
	 * The individual cursor ID number that is assigned to each TuioCursor.
	 */ 
	protected int cursor_id;
	
	/**
	 * This constructor takes a TuioTime argument and assigns it along  with the provided 
	 * Session ID, Cursor ID, X and Y coordinate to the newly created TuioCursor.
	 *
	 * @param	ttime	the TuioTime to assign
	 * @param	si	the Session ID  to assign
	 * @param	ci	the Cursor ID  to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 */
	public TuioCursor (TuioTime ttime, long si, int ci, float xp, float yp) {
		super(ttime, si,xp,yp);
		this.cursor_id = ci;
	}
	
	/**
	 * This constructor takes the provided Session ID, Cursor ID, X and Y coordinate 
	 * and assigs these values to the newly created TuioCursor.
	 *
	 * @param	si	the Session ID  to assign
	 * @param	ci	the Cursor ID  to assign
	 * @param	xp	the X coordinate to assign
	 * @param	yp	the Y coordinate to assign
	 */
	public TuioCursor (long si, int ci, float xp, float yp) {
		super(si,xp,yp);
		this.cursor_id = ci;
	}

	/**
	 * This constructor takes the atttibutes of the provided TuioCursor 
	 * and assigs these values to the newly created TuioCursor.
	 *
	 * @param	tcur	the TuioCursor to assign
	 */
	public TuioCursor (TuioCursor tcur) {
		super(tcur);
		this.cursor_id = tcur.getCursorID();
	}
	
	/**
	 * Returns the Cursor ID of this TuioCursor.
	 * @return	the Cursor ID of this TuioCursor
	 */
	public int getCursorID() {
		return cursor_id;
	}
	
}
