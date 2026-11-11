/*
 TUIO Java GUI Demo
 Copyright (c) 2005-2026 Martin Kaltenbrunner <martin@tuio.org>

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files
 (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge,
 publish, distribute, sublicense, and/or sell copies of the Software,
 and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent;
import javax.swing.JFrame;

import TUIO.TuioBlob;
import TUIO.TuioClient;
import TUIO.TuioCursor;
import TUIO.TuioListener;
import TUIO.TuioObject;
import TUIO.TuioPoint;
import TUIO.TuioTime;

public class TuioDemo extends JComponent implements TuioListener {

	private static final int WINDOW_WIDTH  = 640;
	private static final int WINDOW_HEIGHT = 480;

	private static final int FINGER_SIZE = 18;
	private static final int OBJECT_SIZE = 60;
	private static final int TABLE_SIZE  = 760;

	private final Map<Long,TuioObject> objectList = new ConcurrentHashMap<Long,TuioObject>();
	private final Map<Long,TuioCursor> cursorList = new ConcurrentHashMap<Long,TuioCursor>();
	private final Map<Long,TuioBlob>   blobList   = new ConcurrentHashMap<Long,TuioBlob>();

	private final Color bgrColor = new Color(0, 0, 64);
	private final Color curColor = new Color(192, 0, 192);
	private final Color objColor = new Color(64, 0, 0);
	private final Color blbColor = new Color(64, 64, 64);

	private boolean verbose = false;
	private boolean fullscreen = false;

	private JFrame frame;
	private final GraphicsDevice device;
	private final Cursor invisibleCursor;

	public TuioDemo() {
		device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "invisible cursor");
		setupWindow();
		showWindow();
	}

	public void setupWindow() {

		frame = new JFrame();
		frame.add(this);

		frame.setTitle("TuioDemo");
		frame.setResizable(false);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});

		frame.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent evt) {
				if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
					System.exit(0);
				} else if (evt.getKeyCode() == KeyEvent.VK_F1) {
					destroyWindow();
					setupWindow();
					fullscreen = !fullscreen;
					showWindow();
				} else if (evt.getKeyCode() == KeyEvent.VK_V) {
					verbose = !verbose;
				}
			}
		});
	}

	public void destroyWindow() {
		frame.setVisible(false);
		if (fullscreen) device.setFullScreenWindow(null);
		frame = null;
	}

	public void showWindow() {

		if (fullscreen) {
			int width  = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
			int height = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
			setSize(width, height);

			frame.setSize(width, height);
			frame.setUndecorated(true);
			device.setFullScreenWindow(frame);
			frame.setCursor(invisibleCursor);
		} else {
			setSize(WINDOW_WIDTH, WINDOW_HEIGHT);

			frame.pack();
			Insets insets = frame.getInsets();
			frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT + insets.top);
			frame.setCursor(Cursor.getDefaultCursor());
		}

		frame.setVisible(true);
		frame.repaint();
	}

	public void addTuioObject(TuioObject tobj) {
		objectList.put(tobj.getSessionID(), tobj);
		if (verbose)
			System.out.println("add obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ") "
					+ tobj.getX() + " " + tobj.getY() + " " + tobj.getAngle());
	}

	public void updateTuioObject(TuioObject tobj) {
		if (verbose)
			System.out.println("set obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ") "
					+ tobj.getX() + " " + tobj.getY() + " " + tobj.getAngle() + " " + tobj.getMotionSpeed()
					+ " " + tobj.getRotationSpeed() + " " + tobj.getMotionAccel() + " " + tobj.getRotationAccel());
	}

	public void removeTuioObject(TuioObject tobj) {
		objectList.remove(tobj.getSessionID());
		if (verbose)
			System.out.println("del obj " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ")");
	}

	public void addTuioCursor(TuioCursor tcur) {
		cursorList.put(tcur.getSessionID(), tcur);
		if (verbose)
			System.out.println("add cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ") "
					+ tcur.getX() + " " + tcur.getY());
	}

	public void updateTuioCursor(TuioCursor tcur) {
		if (verbose)
			System.out.println("set cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ") "
					+ tcur.getX() + " " + tcur.getY() + " " + tcur.getMotionSpeed() + " " + tcur.getMotionAccel());
	}

	public void removeTuioCursor(TuioCursor tcur) {
		cursorList.remove(tcur.getSessionID());
		if (verbose)
			System.out.println("del cur " + tcur.getCursorID() + " (" + tcur.getSessionID() + ")");
	}

	public void addTuioBlob(TuioBlob tblb) {
		blobList.put(tblb.getSessionID(), tblb);
		if (verbose)
			System.out.println("add blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ") "
					+ tblb.getX() + " " + tblb.getY() + " " + tblb.getAngle());
	}

	public void updateTuioBlob(TuioBlob tblb) {
		if (verbose)
			System.out.println("set blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ") "
					+ tblb.getX() + " " + tblb.getY() + " " + tblb.getAngle() + " " + tblb.getMotionSpeed()
					+ " " + tblb.getRotationSpeed() + " " + tblb.getMotionAccel() + " " + tblb.getRotationAccel());
	}

	public void removeTuioBlob(TuioBlob tblb) {
		blobList.remove(tblb.getSessionID());
		if (verbose)
			System.out.println("del blb " + tblb.getBlobID() + " (" + tblb.getSessionID() + ")");
	}

	public void refresh(TuioTime frameTime) {
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {

		int width = getWidth();
		int height = getHeight();
		float scale = height / (float) TABLE_SIZE;

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		g2.setColor(bgrColor);
		g2.fillRect(0, 0, width, height);

		int w = (int) Math.round(width - scale * FINGER_SIZE / 2.0f);
		int h = (int) Math.round(height - scale * FINGER_SIZE / 2.0f);

		for (TuioCursor tcur : cursorList.values()) {
			List<TuioPoint> path = tcur.getPath();
			if (path.isEmpty()) continue;
			TuioPoint currentPoint = path.get(0);

			// draw the cursor path
			g2.setPaint(Color.blue);
			for (TuioPoint point : path) {
				g2.drawLine(currentPoint.getScreenX(w), currentPoint.getScreenY(h),
						point.getScreenX(w), point.getScreenY(h));
				currentPoint = point;
			}

			// draw the finger tip
			g2.setPaint(curColor);
			int s = (int) (scale * FINGER_SIZE);
			g2.fillOval(currentPoint.getScreenX(w - s / 2), currentPoint.getScreenY(h - s / 2), s, s);
			g2.setPaint(Color.white);
			g2.drawString(String.valueOf(tcur.getCursorID()),
					currentPoint.getScreenX(w), currentPoint.getScreenY(h));
		}

		// draw the objects
		for (TuioObject tobj : objectList.values()) {
			float ox = tobj.getScreenX(width);
			float oy = tobj.getScreenY(height);
			float size = OBJECT_SIZE * scale;

			Rectangle2D square = new Rectangle2D.Float(-size / 2, -size / 2, size, size);

			AffineTransform transform = new AffineTransform();
			transform.rotate(tobj.getAngle(), ox, oy);
			transform.translate(ox, oy);

			g2.setPaint(objColor);
			g2.fill(transform.createTransformedShape(square));
			g2.setPaint(Color.white);
			g2.drawString(String.valueOf(tobj.getSymbolID()), ox - 10, oy);
		}

		// draw the blobs
		for (TuioBlob tblb : blobList.values()) {
			float bx = tblb.getScreenX(width);
			float by = tblb.getScreenY(height);
			float bw = tblb.getScreenWidth(width);
			float bh = tblb.getScreenHeight(height);
			Ellipse2D ellipse = new Ellipse2D.Float(-bw / 2.0f, -bh / 2.0f, bw, bh);

			AffineTransform transform = new AffineTransform();
			transform.rotate(tblb.getAngle(), bx, by);
			transform.translate(bx, by);

			g2.setPaint(blbColor);
			g2.fill(transform.createTransformedShape(ellipse));
			g2.setPaint(Color.white);
			g2.drawString(String.valueOf(tblb.getBlobID()), bx - 10, by);
		}
	}

	public static void main(String[] args) {

		TuioDemo demo = new TuioDemo();
		TuioClient client = null;

		switch (args.length) {
			case 1:
				try {
					client = new TuioClient(Integer.parseInt(args[0]));
				} catch (NumberFormatException e) {
					System.out.println("usage: java TuioDemo [port]");
					System.exit(0);
				}
				break;
			case 0:
				client = new TuioClient();
				break;
			default:
				System.out.println("usage: java TuioDemo [port]");
				System.exit(0);
				break;
		}

		if (client != null) {
			client.addTuioListener(demo);
			client.connect();
		} else {
			System.out.println("usage: java TuioDemo [port]");
			System.exit(0);
		}
	}
}
