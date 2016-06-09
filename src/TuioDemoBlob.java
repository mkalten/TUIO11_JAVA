/*
 TUIO Java GUI Demo
 Copyright (c) 2005-2016 Martin Kaltenbrunner <martin@tuio.org>
 
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

import javax.swing.*;
import java.awt.geom.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import TUIO.*;

public class TuioDemoBlob extends TuioBlob {

	private Shape ellipse;

	public TuioDemoBlob(TuioBlob tblb) {
		super(tblb);
		float w = tblb.getScreenWidth(TuioDemoComponent.width);
		float h = tblb.getScreenHeight(TuioDemoComponent.height);
		ellipse = new Ellipse2D.Float(-w/2,-h/2,w,h);
		
		AffineTransform transform = new AffineTransform();
		transform.translate(xpos,ypos);
		transform.rotate(angle,xpos,ypos);
		ellipse = transform.createTransformedShape(ellipse);
	}
	
	public void paint(Graphics2D g) {
	
		float x = xpos*TuioDemoComponent.width;
		float y = ypos*TuioDemoComponent.height;
		//float scale = TuioDemoComponent.height/(float)TuioDemoComponent.table_size;

		AffineTransform trans = new AffineTransform();
		trans.translate(-xpos,-ypos);
		trans.translate(x,y);
		//trans.scale(scale,scale);
		Shape s = trans.createTransformedShape(ellipse);
	
		g.setPaint(Color.black);
		g.fill(s);
		g.setPaint(Color.white);
		g.drawString(blob_id+"",x-10,y);
	}

	public void update(TuioBlob tblb) {
		
		float dx = tblb.getX() - xpos;
		float dy = tblb.getY() - ypos;
		float da = tblb.getAngle() - angle;

		if (da!=0) {
			AffineTransform trans = AffineTransform.getRotateInstance(da,xpos,ypos);
			ellipse = trans.createTransformedShape(ellipse);
		}
		
		if ((dx!=0) || (dy!=0)) {
			AffineTransform trans = AffineTransform.getTranslateInstance(dx,dy);
			ellipse = trans.createTransformedShape(ellipse);
		}

		super.update(tblb);
	}

}
