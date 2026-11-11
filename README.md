# TUIO Java Library

Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>

This software is part of reacTIVision, an open source fiducial tracking and multi-touch framework based on computer vision.
<https://github.com/mkalten/reacTIVision>

## Features

- TUIO 1.1 client **and** server (`TuioClient`, `TuioServer`, `TuioManager`)
- UDP and TCP transports for receiving and sending
- Multiple simultaneous TUIO sources
- Optional One Euro filtering for position, angle and blob size
- Self-contained OSC 1.0 codec (`TUIO.osc`) — no external dependencies
- Swing demo client applications and a simple touch simulator

## Building

The project is built with Maven 3 and targets Java 8:

```bash
mvn test      # run the test suite
mvn package   # build target/libTUIO.jar and target/TuioDemo.jar
```

`libTUIO.jar` contains the library only; `TuioDemo.jar` is an executable jar bundling the demo applications.

## Testing

```bash
mvn test      # unit and transport tests
```

For an end-to-end check, run the C++ `TuioAudit` tool (TUIO11_CPP) as a sender and watch the events in `TuioDemo` or `TuioDump`:

```bash
java -jar target/TuioDemo.jar &
TuioAudit -s mixed -v        # TuioAudit -l lists all scenarios
```

## Receiving TUIO

Create a `TuioClient` and register a `TuioListener`. The client listens for TUIO messages and generates higher-level object events:

```java
class MyApplication implements TuioListener { ... }

MyApplication app = new MyApplication();
TuioClient client = new TuioClient();       // UDP on port 3333
client.addTuioListener(app);
client.connect();
```

Other receivers can be injected for different transports:

```java
TuioClient tcp = new TuioClient(new TcpReceiver(3333));
```

A `TuioListener` implements the following callbacks:

| Method                       | Called when                                  |
|------------------------------|----------------------------------------------|
| `addTuioObject(tobj)`        | an object becomes visible                    |
| `updateTuioObject(tobj)`     | an object was moved                          |
| `removeTuioObject(tobj)`     | an object was removed                        |
| `addTuioCursor(tcur)`        | a new cursor is detected                     |
| `updateTuioCursor(tcur)`     | a cursor was moved                           |
| `removeTuioCursor(tcur)`     | a cursor was removed                         |
| `addTuioBlob(tblob)`         | a new blob is detected                       |
| `updateTuioBlob(tblob)`      | a blob was moved                             |
| `removeTuioBlob(tblob)`      | a blob was removed                           |
| `refresh(frameTime)`         | after each bundle (e.g. repaint the screen)  |

Each object or cursor is identified by a unique session ID maintained over its lifetime. Objects additionally carry a symbol ID corresponding to their fiducial marker number. `TuioObject`, `TuioCursor` and `TuioBlob` references are updated in place by the client, expose their state through getters such as `getX()`, `getY()` and `getAngle()`, provide distance/angle helpers, and track their movement path via `getPath()`.

The client also supports polling instead of callbacks:

- `getTuioObjectList()` / `getTuioCursorList()` / `getTuioBlobList()`
- `getTuioObject(s_id)` / `getTuioCursor(s_id)` / `getTuioBlob(s_id)`
- source-aware variants `getTuioObjects(source_id)` etc. for multi-source setups

## Sending TUIO

Sending works in analogy to the client:

```java
TuioServer server = new TuioServer("localhost", 3333);  // UDP
server.initFrame(TuioTime.getSessionTime());
TuioCursor cursor = server.addTuioCursor(0.5f, 0.5f);
server.commitFrame();
```

`TuioServer` can send via `UdpSender` and `TcpSender` (client or listening mode); additional senders can be registered with `addOscSender()`.

## Demo Applications

- **TuioDump** — prints all TUIO events to the console.
- **TuioDemo** — graphically displays the object and cursor state.
  `F1` toggles fullscreen, `V` prints TUIO events, `ESC` quits.
**SimpleSimulator** — generates TUIO messages with mouse input.
  Shift-drag keeps cursors alive, ctrl-drag moves a joint group;
  `h`/`v`/`r` invert axes/rotation, `f`/`p` toggle filtering and
  periodic/full-update mode.

## License

This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
