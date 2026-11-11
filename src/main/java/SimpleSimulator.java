/*
 * TUIO Java library
 * Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
*/

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import TUIO.*;

/**
 * A simple Swing port of the SimpleSimulator demo.
 * It shows the cursors managed by a TuioServer and sends
 * TUIO messages for them.
 */
public class SimpleSimulator extends JPanel {

	private static final int WINDOW_WIDTH = 640;
	private static final int WINDOW_HEIGHT = 480;

	private final TuioServer tuioServer;
	private final java.util.List<TuioCursor> stickyCursorList = new ArrayList<TuioCursor>();
	private final java.util.List<TuioCursor> jointCursorList = new ArrayList<TuioCursor>();
	private final java.util.List<TuioCursor> activeCursorList = new ArrayList<TuioCursor>();
	private TuioCursor mouseCursor = null;

	private boolean verbose = false;
	private boolean fullupdate = false;
	private boolean periodic = false;
	private boolean fullscreen = false;
	private boolean running = false;
	private boolean help = false;
	private TuioTime frameTime = TuioTime.getSessionTime();

	private JFrame frame;

	public SimpleSimulator(TuioServer server) {
		super(true);
		TuioTime.initSession();
		frameTime = TuioTime.getSessionTime();
		tuioServer = server;
		tuioServer.setSourceName("SimpleSimulator");
		tuioServer.enableObjectProfile(false);
		tuioServer.enableBlobProfile(false);

		setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
		setBackground(Color.WHITE);

		MouseInputListener mouseInput = new MouseInputListener();
		addMouseListener(mouseInput);
		addMouseMotionListener(mouseInput);

		frame = new JFrame("SimpleSimulator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(this);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.addKeyListener(new SimulatorKeyListener());
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				running = false;
			}
		});
		frame.setVisible(true);
	}

	private class SimulatorKeyListener extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ESCAPE:
				running = false;
				frame.dispose();
				System.exit(0);
				break;
			case KeyEvent.VK_F1:
				fullscreen = !fullscreen;
				frame.setExtendedState(fullscreen
						? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);
				break;
			case KeyEvent.VK_F:
				fullupdate = !tuioServer.fullUpdateEnabled();
				if (fullupdate) tuioServer.enableFullUpdate();
				else tuioServer.disableFullUpdate();
				break;
			case KeyEvent.VK_P:
				periodic = !tuioServer.periodicMessagesEnabled();
				if (periodic) tuioServer.enablePeriodicMessages();
				else tuioServer.disablePeriodicMessages();
				break;
			case KeyEvent.VK_V:
				verbose = !verbose;
				tuioServer.setVerbose(verbose);
				break;
			case KeyEvent.VK_H:
				help = !help;
				break;
			case KeyEvent.VK_R:
				tuioServer.resetTuioCursors();
				stickyCursorList.clear();
				jointCursorList.clear();
				activeCursorList.clear();
				mouseCursor = null;
				break;
			default:
				break;
			}
		}
	}

	private class MouseInputListener extends MouseAdapter {

		private float normX(MouseEvent e) {
			return (float) e.getX() / getWidth();
		}

		private float normY(MouseEvent e) {
			return (float) e.getY() / getHeight();
		}

		public void mousePressed(MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e)) return;
			float x = normX(e);
			float y = normY(e);

			TuioCursor match = null;
			float distance = 0.01f;
			for (TuioCursor tcur : stickyCursorList) {
				float test = tcur.getDistance(x, y);
				if (test < distance && test < 8.0f / getWidth()) {
					distance = test;
					match = tcur;
				}
			}

			if (e.isShiftDown()) {
				if (match != null) {
					jointCursorList.remove(match);
					stickyCursorList.remove(match);
					activeCursorList.remove(match);
					tuioServer.removeTuioCursor(match);
				} else {
					TuioCursor cursor = tuioServer.addTuioCursor(x, y);
					stickyCursorList.add(cursor);
					activeCursorList.add(cursor);
				}
			} else if (e.isControlDown()) {
				if (match != null) {
					if (jointCursorList.contains(match)) jointCursorList.remove(match);
					else jointCursorList.add(match);
				}
			} else {
				mouseCursor = (match != null) ? match : tuioServer.addTuioCursor(x, y);
				if (!activeCursorList.contains(mouseCursor))
					activeCursorList.add(mouseCursor);
			}
		}

		public void mouseDragged(MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e)) return;
			if (mouseCursor == null) return;
			float x = normX(e);
			float y = normY(e);

			if (mouseCursor.getTuioTime().equals(frameTime)) return;

			if (jointCursorList.contains(mouseCursor)) {
				float dx = x - mouseCursor.getX();
				float dy = y - mouseCursor.getY();
				for (TuioCursor jointCursor : jointCursorList)
					tuioServer.updateTuioCursor(jointCursor,
							jointCursor.getX() + dx, jointCursor.getY() + dy);
			} else {
				tuioServer.updateTuioCursor(mouseCursor, x, y);
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e)) return;
			if (mouseCursor == null) return;

			activeCursorList.remove(mouseCursor);
			if (!stickyCursorList.contains(mouseCursor))
				tuioServer.removeTuioCursor(mouseCursor);
			mouseCursor = null;
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int width = getWidth();
		int height = getHeight();

		for (TuioCursor tcur : tuioServer.getTuioCursors()) {
			java.util.List<TuioPoint> path = tcur.getPath();
			if (path.size() > 0) {
				// draw the path
				g2.setColor(Color.BLUE);
				TuioPoint lastPoint = path.get(0);
				for (TuioPoint point : path) {
					g2.drawLine(lastPoint.getScreenX(width), lastPoint.getScreenY(height),
							point.getScreenX(width), point.getScreenY(height));
					lastPoint = point;
				}

				// draw the finger tip
				if (jointCursorList.contains(tcur)) g2.setColor(Color.DARK_GRAY);
				else g2.setColor(Color.LIGHT_GRAY);
				int r = height / 50;
				g2.fillOval(lastPoint.getScreenX(width) - r / 2,
						lastPoint.getScreenY(height) - r / 2, r, r);

				g2.setColor(Color.BLACK);
				g2.drawString("" + tcur.getCursorID(),
						tcur.getScreenX(width), tcur.getScreenY(height));
			}
		}

		if (help) {
			g2.setColor(Color.BLUE);
			String[] lines = {
				"h - toggle help display",
				verbose ? "v - disable verbose mode" : "v - enable verbose mode",
				"r - reset session",
				tuioServer.fullUpdateEnabled() ? "f - disable full update" : "f - enable full update",
				tuioServer.periodicMessagesEnabled() ? "p - disable periodic messages" : "p - enable periodic messages",
				"SHIFT click - create persistent cursor",
				"CTRL click - add to cursor group",
				fullscreen ? "F1 - exit fullscreen mode" : "F1 - enter fullscreen mode",
				"ESC - Quit"
			};
			for (int i = 0; i < lines.length; i++)
				g2.drawString(lines[i], 10, 20 + i * 15);
		}
	}

	public void run() {
		running = true;
		javax.swing.Timer timer = new javax.swing.Timer(20, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!running) { ((javax.swing.Timer) e.getSource()).stop(); return; }
				frameTime = TuioTime.getSessionTime();
				tuioServer.initFrame(frameTime);
				tuioServer.stopUntouchedMovingCursors();
				tuioServer.commitFrame();
				repaint();
			}
		});
		timer.start();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				TuioServer server = new TuioServer(); // default is UDP port 3333 on localhost
				server.addOscSender(new TcpSender(3333));
				SimpleSimulator app = new SimpleSimulator(server);
				app.run();
			}
		});
	}
}
