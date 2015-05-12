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
 * 
 * Modified by Matthew Aguirre <matta@artistech.com>
 */

package TUIO;

/**
 * The TuioListener interface provides a simple callback infrastructure which is
 * used by the {@link TuioClient} class to dispatch TUIO events to all
 * registered instances of classes that implement the TuioListener interface
 * defined here.<P>
 * Any class that implements the TuioListener interface is required to implement
 * all of the callback methods defined here. The {@link TuioClient} makes use of
 * these interface methods in order to dispatch TUIO events to all registered
 * TuioListener implementations.<P>
 * <code>
 * public class MyTuioListener implements TuioListener<br>
 * ...</code><p>
 * <code>
 * MyTuioListener listener = new MyTuioListener();<br>
 * TuioClient client = new TuioClient();<br>
 * client.addTuioListener(listener);<br>
 * client.start();<br>
 * </code>
 *
 * @author Martin Kaltenbrunner
 * @version 1.1.0
 */
public interface TuioListener {

    /**
     * This callback method is invoked by the TuioClient when a new TuioObject
     * is added to the session.
     *
     * @param tobj the TuioObject reference associated to the addTuioObject
     * event
     */
    public void addTuioObject(TuioObject tobj);

    /**
     * This callback method is invoked by the TuioClient when an existing
     * TuioObject is updated during the session.
     *
     * @param tobj the TuioObject reference associated to the updateTuioObject
     * event
     */
    public void updateTuioObject(TuioObject tobj);

    /**
     * This callback method is invoked by the TuioClient when an existing
     * TuioObject is removed from the session.
     *
     * @param tobj the TuioObject reference associated to the removeTuioObject
     * event
     */
    public void removeTuioObject(TuioObject tobj);

    /**
     * This callback method is invoked by the TuioClient when a new TuioCursor
     * is added to the session.
     *
     * @param tcur the TuioCursor reference associated to the addTuioCursor
     * event
     */
    public void addTuioCursor(TuioCursor tcur);

    /**
     * This callback method is invoked by the TuioClient when an existing
     * TuioCursor is updated during the session.
     *
     * @param tcur the TuioCursor reference associated to the updateTuioCursor
     * event
     */
    public void updateTuioCursor(TuioCursor tcur);

    /**
     * This callback method is invoked by the TuioClient when an existing
     * TuioCursor is removed from the session.
     *
     * @param tcur the TuioCursor reference associated to the removeTuioCursor
     * event
     */
    public void removeTuioCursor(TuioCursor tcur);

    /**
     * This callback method is invoked by the TuioClient when a new TuioBlob is
     * added to the session.
     *
     * @param tblb the TuioBlob reference associated to the addTuioBlob event
     */
    public void addTuioBlob(TuioBlob tblb);

    /**
     * This callback method is invoked by the TuioClient when an existing
     * TuioBlob is updated during the session.
     *
     * @param tblb the TuioBlob reference associated to the updateTuioBlob event
     */
    public void updateTuioBlob(TuioBlob tblb);

    /**
     * This callback method is invoked by the TuioClient when an existing
     * TuioBlob is removed from the session.
     *
     * @param tblb the TuioBlob reference associated to the removeTuioBlob event
     */
    public void removeTuioBlob(TuioBlob tblb);

    /**
     * This callback method is invoked by the TuioClient to mark the end of a
     * received TUIO message bundle.
     *
     * @param ftime the TuioTime associated to the current TUIO message bundle
     */
    public void refresh(TuioTime ftime);
}
