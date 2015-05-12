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


import TUIO.TuioClient;
import TUIO.TuioListener;
import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;

public final class TuioDemo {

    private final int window_width = 640;
    private final int window_height = 480;

    private boolean fullscreen = false;

    private final TuioDemoComponent demo;
    private JFrame frame;
    private final GraphicsDevice device;
    private final Cursor invisibleCursor;

    public TuioDemo() {
        demo = new TuioDemoComponent();
        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "invisible cursor");
        setupWindow();
        showWindow();
    }

    public TuioListener getTuioListener() {
        return demo;
    }

    public void setupWindow() {

        frame = new JFrame();
        frame.add(demo);

        frame.setTitle("TuioDemo");
        frame.setResizable(false);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                } else if (evt.getKeyCode() == KeyEvent.VK_F1) {
                    destroyWindow();
                    setupWindow();
                    fullscreen = !fullscreen;
                    showWindow();
                } else if (evt.getKeyCode() == KeyEvent.VK_V) {
                    demo.verbose = !demo.verbose;
                }
            }
        });
    }

    public void destroyWindow() {

        frame.setVisible(false);
        if (fullscreen) {
            device.setFullScreenWindow(null);
        }
        frame = null;
    }

    public void showWindow() {

        if (fullscreen) {
            int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
            int height = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
            demo.setSize(width, height);

            frame.setSize(width, height);
            frame.setUndecorated(true);
            device.setFullScreenWindow(frame);
            frame.setCursor(invisibleCursor);
        } else {
            int width = window_width;
            int height = window_height;
            demo.setSize(width, height);

            frame.pack();
            Insets insets = frame.getInsets();
            frame.setSize(width, height + insets.top);
            frame.setCursor(Cursor.getDefaultCursor());
        }

        frame.setVisible(true);
        frame.repaint();
    }

    public static void main(String argv[]) {

        TuioDemo demo = new TuioDemo();
        TuioClient client = null;

        switch (argv.length) {
            case 1:
                try {
                    client = new TuioClient(Integer.parseInt(argv[0]));
                } catch (Exception e) {
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
            client.addTuioListener(demo.getTuioListener());
            client.connect();
        } else {
            System.out.println("usage: java TuioDemo [port]");
            System.exit(0);
        }
    }

}
