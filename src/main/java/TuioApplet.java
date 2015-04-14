/*
 TUIO Java Console Example
 Copyright (c) 2005-2014 Martin Kaltenbrunner <martin@tuio.org>

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

import TUIO.TuioClient;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JApplet;

public class TuioApplet extends JApplet {

    TuioClient client;
    int port = 3333;

    @Override
    public void init() {
        try {
            port = Integer.parseInt(getParameter("port"));
        } catch (Exception e) {
        }

        Dimension size = this.getSize();

        TuioDemoComponent demo = new TuioDemoComponent();
        demo.setSize(size.width, size.height);

        client = new TuioClient();
        client.addTuioListener(demo);

        add(demo);
        repaint();
    }

    @Override
    public void start() {
        if (!client.isConnected()) {
            client.connect();
        }
    }

    @Override
    public void stop() {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    @Override
    public void destroy() {
        if (client.isConnected()) {
            client.disconnect();
        }
        client = null;
    }

    @Override
    public void paint(Graphics g) {
    }
}
