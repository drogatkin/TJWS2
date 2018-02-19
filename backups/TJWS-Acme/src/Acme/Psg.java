// Psg - a PostScript-like alternative to the Graphics class
//
// Copyright (C) 1996 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

package Acme;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

/// <A HREF="http://java.developer.com/"><IMG WIDTH=162 HEIGHT=35 ALIGN="right"
/// SRC="/resources/gamelan_best.jpg"></A>
// <A HREF="http://www.developer.com/whatscool/"><IMG WIDTH=173 HEIGHT=36
/// ALIGN="right" SRC="/resources/gamelan_cool.gif"></A>
// A PostScript-like alternative to the Graphics class.
// <P>
// This class provides a functional equivalent of java.awt.Graphics,
// but with a very PostScript-like interface. It implements the following
// PostScript operators:
// <BLOCKQUOTE>
// gsave grestore grestoreall initgraphics setlinewidth setcolor setgray
// sethsbcolor setrgbcolor translate scale rotate transform dtransform
// itransform idtransform
// newpath moveto rmoveto lineto rlineto arc arcn curveto rcurveto closepath
// flattenpath clippath pathbbox
// erasepage fill stroke
// rectfill rectstroke
// findfont scalefont setfont
// show stringwidth
// </BLOCKQUOTE>
// Not only is this a more powerful rendering idiom than the standard
// Graphics class, but it also makes it hellof easy to translate
// PostScript graphics hacks into Java. Here's a
// <A HREF="/resources/classes/Yoyo.java">sample</A>:
// <BLOCKQUOTE>
// <APPLET WIDTH=300 HEIGHT=200 CODEBASE="/resources/classes" CODE="Yoyo">
// </APPLET>
// </BLOCKQUOTE>
// <P>
// JavaSoft and Adobe are said to be working on a 2-D rendering API
// similar to PostScript, and therefore similar to this. When that
// comes out this class will probably be obsolete, so I don't plan
// on doing any major improvements.
// <P>
// <A HREF="/resources/classes/Acme/Psg.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class Psg {
	
	private Component component;
	private Graphics graphics;
	private PsgState current;
	private Stack gStack;
	private Vector path;		// holds PsgPathItems
	private PsgPathItem subpathStart;	// pointer to most recent move
	
	/// Constructor from Component.
	// This is the preferred way to make a Psg.
	public Psg(Component component) {
		this.component = component;
		this.graphics = component.getGraphics();
		initgraphics();
	}
	
	/// Constructor from Graphics.
	// If you don't have a Component, you can create a Psg from a Graphics
	// instead, but this is not as good.
	public Psg(Graphics graphics) {
		this.component = null;
		this.graphics = graphics;
		initgraphics();
	}
	
	/// Save the current graphics state onto a stack.
	public void gsave() {
		PsgState next = (PsgState) current.clone();
		gStack.push(current);
		current = next;
	}
	
	/// Restore the last Save()ed graphics state.
	public void grestore() {
		if(gStack.empty())
			initgraphics();
		else {
			current = (PsgState) gStack.pop();
			if(current.font == null)
				graphics.setFont(current.font);
		}
	}
	
	/// Pop to bottom-most graphics state.
	public void grestoreall() {
		// This is equivalent to initgraphics(), isn't it?
		while(!gStack.empty())
			grestore();
	}
	
	/// Reset graphics state.
	public void initgraphics() {
		current = new PsgState();
		gStack = new Stack();
		path = new Vector();
		subpathStart = null;
		// Set the current transformation to fit screen space.
		Dimension d = size();
		this.translate(0.0, d.height);
		this.scale(1.0, -1.0);
		erasepage();
	}
	
	/// Set the current line width.
	public void setlinewidth(double linewidth) {
		current.linewidth = wtransform(linewidth);
	}
	
	/// Set the current color.
	public void setcolor(Color color) {
		current.color = color;
	}
	
	/// Set the color to the specified gray value (0=black, 1=white).
	public void setgray(float grayVal) {
		setrgbcolor(grayVal, grayVal, grayVal);
	}
	
	/// Set the color from HSB coordinates.
	public void sethsbcolor(float hue, float saturation, float brightness) {
		setcolor(new Color(Color.HSBtoRGB(hue, saturation, brightness)));
	}
	
	/// Set the color from RGB coordinates.
	public void setrgbcolor(float r, float g, float b) {
		setcolor(new Color(r, g, b));
	}
	
	/// Translate graphics space.
	public void translate(double tx, double ty) {
		double nx = tx * current.matrix.xx + ty * current.matrix.yx + current.matrix.tx;
		double ny = ty * current.matrix.yy + tx * current.matrix.xy + current.matrix.ty;
		current.matrix.tx = nx;
		current.matrix.ty = ny;
	}
	
	/// Scale graphics space.
	public void scale(double sx, double sy) {
		current.matrix.xx *= sx;
		current.matrix.xy *= sx;
		current.matrix.yx *= sy;
		current.matrix.yy *= sy;
	}
	
	/// Rotate graphics space.
	public void rotate(double degrees) {
		double radians = toRadians(degrees);
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		double sxx = current.matrix.xx;
		double sxy = current.matrix.xy;
		current.matrix.xx = cos * sxx + sin * current.matrix.yx;
		current.matrix.xy = cos * sxy + sin * current.matrix.yy;
		current.matrix.yx = cos * current.matrix.yx - sin * sxx;
		current.matrix.yy = cos * current.matrix.yy - sin * sxy;
	}
	
	/// Transform (x, y) into a device-space x-coordinate.
	public int transform_x(double x, double y) {
		return (int) (x * current.matrix.xx + y * current.matrix.yx + current.matrix.tx);
	}
	
	/// Transform (x, y) into a device-space y-coordinate.
	public int transform_y(double x, double y) {
		return (int) (y * current.matrix.yy + x * current.matrix.xy + current.matrix.ty);
	}
	
	/// Transform (dx, dy) into a device-space x-distance.
	public int dtransform_x(double dx, double dy) {
		return (int) (dx * current.matrix.xx + dy * current.matrix.yx);
	}
	
	/// Transform (dx, dy) into a device-space y-distance.
	public int dtransform_y(double dx, double dy) {
		return (int) (dy * current.matrix.yy + dx * current.matrix.xy);
	}
	
	/// Inverse transform (x, y) into a user-space x-coordinate.
	// @exception Acme.PsgException "undefined result"
	public double itransform_x(int x, int y) throws PsgException {
		PsgMatrix inv = current.matrix.invert();
		return x * inv.xx + y * inv.yx + inv.tx;
	}
	
	/// Inverse transform (x, y) into a user-space y-coordinate.
	// @exception Acme.PsgException "undefined result"
	public double itransform_y(int x, int y) throws PsgException {
		PsgMatrix inv = current.matrix.invert();
		return y * inv.yy + x * inv.xy + inv.ty;
	}
	
	/// Inverse transform (dx, dy) into a user-space x-distance.
	// @exception Acme.PsgException "undefined result"
	public double idtransform_x(int dx, int dy) throws PsgException {
		PsgMatrix inv = current.matrix.invert();
		return dx * inv.xx + dy * inv.yx;
	}
	
	/// Inverse transform (dx, dy) into a user-space y-distance.
	// @exception Acme.PsgException "undefined result"
	public double idtransform_y(int dx, int dy) throws PsgException {
		PsgMatrix inv = current.matrix.invert();
		return dy * inv.yy + dx * inv.xy;
	}
	
	// Transform a width into device space.
	private int wtransform(double width) {
		int dx = dtransform_x(width, width);
		int dy = dtransform_y(width, width);
		return (Math.abs(dx) + Math.abs(dy)) / 2;
	}
	
	/// Start a new, empty path.
	public void newpath() {
		current.point = null;
		path.setSize(0);
		subpathStart = null;
	}
	
	/// Set the current point.
	public void moveto(double x, double y) {
		if(current.point == null)
			current.point = new DoublePoint();
		current.point.x = x;
		current.point.y = y;
		PsgPathItem n = new PsgPathItem(transform_x(x, y), transform_y(x, y), 0, false);
		path.addElement(n);
		subpathStart = n;
	}
	
	/// Relative moveto().
	// @exception Acme.PsgException "no current point"
	public void rmoveto(double dx, double dy) throws PsgException {
		if(current.point == null)
			throw new PsgException("no current point");
		moveto(current.point.x + dx, current.point.y + dy);
	}
	
	/// Add a line to the path.
	// @exception Acme.PsgException "no current point"
	public void lineto(double x, double y) throws PsgException {
		if(current.point == null)
			throw new PsgException("no current point");
		current.point.x = x;
		current.point.y = y;
		PsgPathItem n = new PsgPathItem(transform_x(x, y), transform_y(x, y), current.linewidth, true);
		path.addElement(n);
	}
	
	/// Relative lineto().
	// @exception Acme.PsgException "no current point"
	public void rlineto(double dx, double dy) throws PsgException {
		if(current.point == null)
			throw new PsgException("no current point");
		lineto(current.point.x + dx, current.point.y + dy);
	}
	
	private final double segAng = 5.0;
	
	/// Append counterclockwise arc.
	public void arc(double cx, double cy, double r, double ang1, double ang2) {
		ang1 = normAngle(ang1);
		ang2 = normAngle(ang2);
		if(ang2 <= ang1)
			ang2 += 360.0;
		for(double a = ang1; a < ang2; a += segAng)
			arcSeg(cx, cy, r, a);
		arcSeg(cx, cy, r, ang2);
	}
	
	/// Append clockwise arc.
	public void arcn(double cx, double cy, double r, double ang1, double ang2) {
		ang1 = normAngle(ang1);
		ang2 = normAngle(ang2);
		if(ang2 >= ang1)
			ang2 -= 360.0;
		for(double a = ang1; a > ang2; a -= segAng)
			arcSeg(cx, cy, r, a);
		arcSeg(cx, cy, r, ang2);
	}
	
	private void arcSeg(double cx, double cy, double r, double a) {
		double x = cx + r * Math.cos(toRadians(a));
		double y = cy + r * Math.sin(toRadians(a));
		if(current.point == null)
			moveto(x, y);
		else
			try {
				lineto(x, y);
			} catch(Acme.PsgException e) {
				// Shouldn't happen.
				throw new InternalError();
			}
	}
	
	private final double tStep = 0.025;
	
	/// Append a Bezier cubic section.
	// @exception Acme.PsgException "no current point"
	public void curveto(double x1, double y1, double x2, double y2, double x3, double y3) throws PsgException {
		if(current.point == null)
			throw new PsgException("no current point");
		double x0 = current.point.x;
		double y0 = current.point.y;
		double cx = (x1 - x0) * 3.0;
		double cy = (y1 - y0) * 3.0;
		double bx = (x2 - x1) * 3.0 - cx;
		double by = (y2 - y1) * 3.0 - cy;
		double ax = x3 - x0 - cx - bx;
		double ay = y3 - y0 - cy - by;
		for(double t = 0.0; t < 1.0; t += tStep)
			curveSeg(ax, ay, bx, by, cx, cy, x0, y0, t);
		curveSeg(ax, ay, bx, by, cx, cy, x0, y0, 1.0);
	}
	
	private void curveSeg(double ax, double ay, double bx, double by, double cx, double cy, double x0, double y0, double t) {
		double t2 = t * t;
		double t3 = t2 * t;
		double x = ax * t3 + bx * t2 + cx * t + x0;
		double y = ay * t3 + by * t2 + cy * t + y0;
		try {
			lineto(x, y);
		} catch(Acme.PsgException e) {
			// Shouldn't happen.
			throw new InternalError();
		}
	}
	
	/// Relative curveto.
	// @exception Acme.PsgException "no current point"
	public void rcurveto(double dx1, double dy1, double dx2, double dy2, double dx3, double dy3) throws PsgException {
		if(current.point == null)
			throw new PsgException("no current point");
		double x0 = current.point.x;
		double y0 = current.point.y;
		curveto(x0 + dx1, y0 + dy1, x0 + dx2, y0 + dy2, x0 + dx3, y0 + dy3);
	}
	
	/// Connect current path back to its starting point.
	public void closepath() {
		if(subpathStart != null)
			path.addElement(new PsgPathItem(subpathStart.p.x, subpathStart.p.y, current.linewidth, true));
		current.point = null;
		subpathStart = null;
	}
	
	/// Convert curves in the path to sequences of straight lines.
	public void flattenpath() {
		// This is actually a no-op, since we always represent curves
		// as sequences of straight lines.
	}
	
	/// Set the current path to the clipping path.
	// @exception Acme.PsgException "undefined result"
	public void clippath() throws PsgException {
		Dimension d = size();
		rectpath(itransform_x(0, 0), itransform_y(0, 0), idtransform_x(d.width, d.height), idtransform_y(d.width, d.height));
	}
	
	/// Return the bounding box of the current path.
	// The return is in the form of a four-element array - element 0 is
	// llx, 1 is lly, 2 is urx, and 3 is ury.
	// @exception Acme.PsgException "no current point"
	public double[] pathbbox() throws PsgException {
		if(path.isEmpty())
			throw new PsgException("no current point");
		int llx, lly, urx, ury;
		llx = ury = Integer.MAX_VALUE;
		urx = lly = Integer.MIN_VALUE;
		Enumeration enumeration = path.elements();
		while(enumeration.hasMoreElements()) {
			PsgPathItem pi = (PsgPathItem) enumeration.nextElement();
			llx = Math.min(llx, pi.p.x);
			lly = Math.max(lly, pi.p.y);
			urx = Math.max(urx, pi.p.x);
			ury = Math.min(ury, pi.p.y);
		}
		double[] r = new double[4];
		r[0] = itransform_x(llx, lly);
		r[1] = itransform_y(llx, lly);
		r[2] = itransform_x(urx, ury);
		r[3] = itransform_y(urx, ury);
		return r;
	}
	
	/// Paint the whole graphics area with the background color.
	public void erasepage() {
		Dimension d = size();
		graphics.clearRect(0, 0, d.width, d.height);
	}
	
	/// Fill current path with current color.
	public void fill() {
		int x, y;
		Polygon poly = null;
		graphics.setColor(current.color);
		Enumeration enumeration = path.elements();
		while(enumeration.hasMoreElements()) {
			PsgPathItem pi = (PsgPathItem) enumeration.nextElement();
			if(!pi.draw) {
				if(poly != null)
					graphics.fillPolygon(poly);
				poly = new Polygon();
			}
			poly.addPoint(pi.p.x, pi.p.y);
			x = pi.p.x;
			y = pi.p.y;
		}
		if(poly != null)
			graphics.fillPolygon(poly);
		newpath();
	}
	
	/// Draw lines along the current path.
	public void stroke() {
		int x = 0, y = 0;	// initial values don't ever get used
		graphics.setColor(current.color);
		Enumeration objEnum = path.elements();
		while(objEnum.hasMoreElements()) {
			PsgPathItem pi = (PsgPathItem) objEnum.nextElement();
			if(pi.draw)
				if(pi.linewidth < 1)
					graphics.drawLine(x, y, pi.p.x, pi.p.y);
				else
					Acme.GuiUtils.drawThickLine(graphics, x, y, pi.p.x, pi.p.y, pi.linewidth);
			x = pi.p.x;
			y = pi.p.y;
		}
		newpath();
	}
	
	/// Fill a rectangular path.
	public void rectfill(double x, double y, double width, double height) {
		gsave();
		rectpath(x, y, width, height);
		fill();
		grestore();
	}
	
	/// Stroke a rectangular path.
	public void rectstroke(double x, double y, double width, double height) {
		gsave();
		rectpath(x, y, width, height);
		stroke();
		grestore();
	}
	
	// Make a rectangular path.
	private void rectpath(double x, double y, double width, double height) {
		newpath();
		moveto(x, y);
		try {
			rlineto(width, 0);
			rlineto(0, height);
			rlineto(-width, 0);
		} catch(Acme.PsgException e) {
			// Shouldn't happen.
			throw new InternalError();
		}
		closepath();
	}
	
	/// Set the current font to the specified name, style, and size.
	// Similar to a PostScript findfont scalefont setfont sequence.
	// @param name the name of the font - Dialog, Helvetica, TimesRoman, Courier
	// @param style some combination of Font.PLAIN, Font.BOLD, and Font.ITALIC
	// @param size how tall the font should be
	public void setfont(String name, int style, double size) {
		setfontName(name);
		setfontStyle(style);
		setfontSize(size);
	}
	
	/// Set the current font to the specified name.
	// @param name the name of the font - Dialog, Helvetica, TimesRoman, Courier
	public void setfontName(String name) {
		current.fontName = new String(name);
		current.font = null;
	}
	
	/// Set the current font to the specified style.
	// @param style some combination of Font.PLAIN, Font.BOLD, and Font.ITALIC
	public void setfontStyle(int style) {
		current.fontStyle = style;
		current.font = null;
	}
	
	/// Set the current font to the specified size.
	// @param size how tall the font should be
	public void setfontSize(double size) {
		current.fontSize = dtransform_y(0.0, size);
		current.font = null;
	}
	
	/// Paint a string starting at the current point.
	// Doesn't do rotation yet.
	// @exception Acme.PsgException "no current point"
	public void show(String str) throws PsgException {
		if(current.point == null)
			throw new PsgException("no current point");
		if(current.font == null) {
			current.font = new Font(current.fontName, current.fontStyle, current.fontSize);
			graphics.setFont(current.font);
		}
		graphics.drawString(str, transform_x(current.point.x, current.point.y), transform_y(current.point.x, current.point.y));
	}
	
	/// Return the x-width of a string.
	// Doesn't do rotation yet.
	// @exception Acme.PsgException "undefined result"
	public double stringwidth_x(String str) throws PsgException {
		FontMetrics fm = graphics.getFontMetrics();
		return idtransform_x(fm.stringWidth(str), 0);
	}
	
	private Dimension size() {
		if(component != null)
			return component.size();
		// return component.getSize();
		// Second choice - use the clipping area.
		Rectangle r = graphics.getClipBounds();
		return new Dimension(r.x + r.width, r.y + r.height);
	}
	
	private static double normAngle(double a) {
		int circles = (int) (a / 360.0);
		return a - circles * 360.0;
	}
	
	private static double toRadians(double a) {
		return a * Math.PI / 180.0;
	}
	
	/// Test program.
	public static void main(String[] args) {
		new MainFrame(new PsgTest(), args, 350, 350);
	}
	
}

class PsgState implements Cloneable {
	
	public int linewidth;
	public Color color;
	public DoublePoint point;
	public PsgMatrix matrix;
	
	public String fontName;
	public int fontStyle;
	public int fontSize;
	public Font font;
	
	// Create a new PsgState.
	public PsgState() {
		linewidth = 0;
		color = Color.black;
		point = null;
		matrix = new PsgMatrix();
		fontName = "Dialog";
		fontStyle = Font.PLAIN;
		fontSize = 12;
		font = null;
	}
	
	public Object clone() {
		try {
			PsgState n = (PsgState) super.clone();
			n.color = new Color(color.getRGB());
			if(point != null)
				n.point = (DoublePoint) point.clone();
			n.matrix = (PsgMatrix) matrix.clone();
			return n;
		} catch(CloneNotSupportedException e) {
			// Shouldn't happen.
			throw new InternalError();
		}
	}
	
}

class PsgPathItem extends Acme.GenericCloneable {
	public Point p;
	public int linewidth;
	public boolean draw;
	
	public PsgPathItem(int x, int y, int linewidth, boolean draw) {
		this.p = new Point(x, y);
		this.linewidth = linewidth;
		this.draw = draw;
	}
	
	// You might think that since we contain a Point object that we need
	// to clone it. However, we treat Points as immutable, never
	// modifying them once they are created, so it's perfectly safe
	// to just copy the reference and share the contents.
}

class PsgMatrix extends Acme.GenericCloneable {
	public double xx, xy, yx, yy, tx, ty;
	
	public PsgMatrix() {
		// Initialize with the identity matrix.
		xx = 1.0;
		xy = 0.0;
		yx = 0.0;
		yy = 1.0;
		tx = 0.0;
		ty = 0.0;
	}
	
	public PsgMatrix multiply(PsgMatrix b) {
		PsgMatrix r = new PsgMatrix();
		r.xx = this.xx * b.xx + this.xy * b.yx;
		r.xy = this.xx * b.xy + this.xy * b.yy;
		r.yy = this.yx * b.xy + this.yy * b.yy;
		r.yx = this.yx * b.xx + this.yy * b.yx;
		r.tx = this.tx * b.xx + this.ty * b.yx + b.tx;
		r.ty = this.tx * b.xy + this.ty * b.yy + b.ty;
		return r;
	}
	
	public PsgMatrix invert() throws PsgException {
		PsgMatrix r = new PsgMatrix();
		double det = this.xx * this.yy - this.xy * this.yx;
		if(det == 0.0)
			throw new PsgException("undefined result");
		r.xx = this.yy / det;
		r.xy = -this.xy / det;
		r.yx = -this.yx / det;
		r.yy = this.xx / det;
		r.tx = -(this.tx * r.xx + this.ty * r.yx);
		r.ty = -(this.tx * r.xy + this.ty * r.yy);
		return r;
	}
}

// Test class.

class PsgTest extends Applet {
	
	// Called when the applet is first created.
	public void init() {
		setBackground(Color.white);
	}
	
	// Called when the applet should paint itself.
	public void paint(Graphics graphics) {
		try {
			Psg psg = new Psg(this);
			psg.gsave();
			psg.translate(50, 50);
			psg.newpath();
			squarePath(psg, 50);
			psg.stroke();
			psg.grestore();
			psg.gsave();
			psg.translate(150, 50);
			psg.rotate(30);
			psg.newpath();
			squarePath(psg, 50);
			psg.stroke();
			psg.grestore();
			psg.gsave();
			psg.translate(250, 50);
			psg.rotate(30);
			psg.scale(1.5, 1.5);
			psg.newpath();
			squarePath(psg, 50);
			psg.stroke();
			psg.grestore();
			psg.gsave();
			psg.translate(50, 150);
			psg.newpath();
			squarePath(psg, 50);
			psg.fill();
			psg.grestore();
			psg.gsave();
			psg.translate(150, 150);
			psg.rotate(30);
			psg.newpath();
			squarePath(psg, 50);
			psg.fill();
			psg.grestore();
			psg.gsave();
			psg.translate(250, 150);
			psg.rotate(30);
			psg.scale(1.5, 1.5);
			psg.newpath();
			squarePath(psg, 50);
			psg.fill();
			psg.grestore();
			psg.gsave();
			psg.translate(50, 250);
			psg.newpath();
			psg.arc(25, 25, 25, 0, 360);
			psg.stroke();
			psg.grestore();
			psg.gsave();
			psg.translate(150, 250);
			psg.newpath();
			psg.arc(25, 25, 25, 0, 360);
			psg.fill();
			psg.grestore();
			psg.gsave();
			psg.translate(250, 250);
			psg.moveto(0, 0);
			psg.curveto(0, 50, 50, 50, 50, 0);
			psg.stroke();
			psg.grestore();
		} catch(PsgException e) {
			System.err.println(e);
			System.exit(1);
		}
	}
	
	private void squarePath(Psg psg, int size) throws PsgException {
		psg.moveto(0, 0);
		psg.rlineto(size, 0);
		psg.rlineto(0, size);
		psg.rlineto(-size, 0);
		psg.closepath();
	}
	
}
