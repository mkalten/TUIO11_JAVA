TUIO JAVA LIBRARY AND EXAMPLES
------------------------------
Copyright (c) 2005-2014 Martin Kaltenbrunner <martin@tuio.org>
This software is part of reacTIVision, an open source fiducial
tracking and multi-touch framework based on computer vision. 
http://reactivision.sourceforge.net/

Modified
--------
Modified by Matthew Aguirre <matta@artistech.com>

1. Made the code a Maven project.  Import the OSC library from Maven.
2. Separated the OSC listener code into 3 different classes.
3. All classes are now serializable.

Demo Applications:
------------------
This package contains three demo applications which receive
TUIO messages from any TUIO enabled tracker or the Simulator.
All these applications show how to use the TUIO Java Library 
in a simple way.

TuioDemo graphically displays the object and cursor state,
TuioApplet is a version of TuioDemo for the web browser,
TuioDump prints the object and cursor state to the console.

You can use these demo applications for debugging purposes, 
or as a starting point for the development of your own Java 
applications implementing the TUIO protocol. Please refer to
the source code of the example and the following section.

Pressing F1 will toggle FullScreen mode with the TuioDemo,
pressing ESC or closing the Window will end the application.
Hitting the V key will print the TUIO events to the console.

Keep in mind to make your graphics scalable for the varying
screen and window resolutions. A reasonable TUIO application
will run in fullscreen mode, although the windowed mode might
be useful for debugging purposes or working with the Simulator.

Application Programming Interface:
----------------------------------
First you  need to create an instance of TuioClient. This class 
is listening to TUIO messages on the specified port and generates
higher level messages based on the object events.

Your application needs to implement the TuioListener interface,
and has to be added to the TuioClient in order to receive messages.

	"class MyApplication implements TuioListener"

A simple code snippet for setting up a TUIO session:

	MyApplication app = new MyApplication();
	TuioClient client = new TuioClient();
	client.addTuioListener(app);
	client.connect();

A TuioListener needs to implement the following methods:

* addTuioObject(TuioObject tobj):
  this is called when an object becomes visible
* updateTuioObject(TuioObject tobj):
  an object was moved on the table surface
* removeTuioObject(TuioObject tobj):
  an object was removed from the table

* addTuioCursor(TuioCursor tcur):
  this is called when a new cursor is detected
* updateTuioCursor(TuioCursor tcur):
  a cursor was moving on the table surface
* removeTuioCursor(TuioCursor tcur):
  a cursor was removed from the table

* addTuioBlob(TuioBlob tblob):
  this is called when a new blob is detected
* updateTuioBlob(TuioBlob tblob):
  a blob is moving on the table surface
* removeTuioBlob(TuioBlob tblob):
  a blob was removed from the table

* refresh(TuioTime frameTime):
  this method is called after each bundle,
  use it to repaint your screen for example
        
Each object or cursor is identified with a  unique session ID, that is maintained
over its lifetime. Additionally each object carries symbol ID that corresponds
to its attached fiducial marker number. The cursor ID of the cursor object is always
a number in the range of all currently detected cursor blobs.

The TuioObject, TuioCursor and TuioBlob references are updated automatically by the TuioClient
and are always referencing the same instance over the object lifetime.
All TuioObject, TuioCursor and TuioBlob attributes are encapsulated and can be
accessed with methods such as getX(), getY() and getAngle() and so on.
TuioObject, TuioCursor and TuioBlob also have some additional convenience methods
for the calculation of distances and angles between objects. The getPath()
method returns a Vector of TuioPoints representing the movement path of the object.

Alternatively the TuioClient class contains some methods for the polling
of the current object and cursor state. There are methods which return
either a list or individual object and cursor objects. The TuioObject,
TuioCursor and TuioBlob classes have been added as a container, which can be also used
by external classes. 

* getTuioObjectList() returns an ArrayList of all currently present TuioObjects
* getTuioCursorList() returns an ArrayList of all currently present TuioCursors
* getTuioBlobList() returns an ArrayList of all currently present TuioBlobs
* getTuioObject(long s_id) returns a TuioObject (or NULL if not present)
* getTuioCursor(long s_id) returns a TuioCursor (or NULL if not present)
* getTuioBlob(long s_id) returns a TuioBlob (or NULL if not present)

License:
--------
This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.
 
This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.
 
You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

References:
-----------
This application uses the JavaOSC OpenSound Control library (v 0.2).
https://github.com/hoijui/JavaOSC
for more information and the source code.
