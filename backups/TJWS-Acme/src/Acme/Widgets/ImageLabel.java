// ImageLabel - a label with an image instead of a string
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@mail.acme.com>. All rights
// reserved.
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

package Acme.Widgets;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

/// A label with an image instead of a string.
// <P>
// <A HREF="/resources/classes/Acme/Widgets/ImageLabel.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class ImageLabel extends Canvas {
	
	private Image image;
	private int width = -1;
	private int height = -1;
	
	/// Constructor.
	public ImageLabel(Image image) {
		setImage(image);
	}
	
	/// Sets the image.
	public void setImage(Image image) {
		invalidate();
		this.image = image;
		width = height = -1;
		changeSize(image.getWidth(this), image.getHeight(this));
		repaint();
	}
	
	private void changeSize(int w, int h) {
		if(w == -1 || h == -1)
			return;
		width = w;
		height = h;
		resize(width, height);
		// setSize( width, height );
		Acme.GuiUtils.packWindow(this);
	}
	
	public Dimension preferredSize() {
		return minimumSize();
	}
	
	public Dimension minimumSize() {
		return new Dimension(width, height);
	}
	
	public void paint(Graphics graphics) {
		graphics.drawImage(image, 0, 0, this);
	}
	
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
		boolean errorFlag = (infoflags & ERROR) != 0;
		boolean abortFlag = (infoflags & ABORT) != 0;
		boolean widthFlag = (infoflags & WIDTH) != 0;
		boolean heightFlag = (infoflags & HEIGHT) != 0;
		boolean propertiesFlag = (infoflags & PROPERTIES) != 0;
		boolean somebitsFlag = (infoflags & SOMEBITS) != 0;
		boolean framebitsFlag = (infoflags & FRAMEBITS) != 0;
		boolean allbitsFlag = (infoflags & ALLBITS) != 0;
		if(errorFlag || abortFlag) {
			setImage(Acme.GuiUtils.brokenIcon(this));
			return false;
		}
		if(widthFlag || heightFlag || framebitsFlag || allbitsFlag)
			changeSize(w, h);
		if(framebitsFlag || allbitsFlag) {
			repaint();
			return false;
		}
		if(somebitsFlag)
			repaint(100);
		return true;
	}
	
}
