// ImageDecoder - abstract class for reading in an image
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

package Acme.JPM.Decoders;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/// Abstract class for reading in an image.
// <P>
// A framework for classes that read in and decode an image in
// a particular file format.
// <P>
// This provides a very simplified rendition of the ImageProducer interface.
// It requires the decoder to read the image a row at a time. It requires
// use of the RGBdefault color model.
// If you want more flexibility you can always implement ImageProducer
// directly.
// <P>
// <A HREF="/resources/classes/Acme/JPM/Decoders/ImageDecoder.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see PpmDecoder
// @see Acme.JPM.Encoders.ImageEncoder

public abstract class ImageDecoder implements ImageProducer {
	
	private InputStream in;
	private int width, height;
	private boolean[] rowsRead;
	private int[][] rgbPixels;
	private boolean startedRead = false;
	private boolean gotSize = false;
	private boolean err = false;
	private boolean producing = false;
	private Vector consumers = new Vector();
	private static final ColorModel model = ColorModel.getRGBdefault();
	
	/// Constructor.
	// @param in The stream to read the bytes from.
	public ImageDecoder(InputStream in) {
		this.in = in;
	}
	
	// Methods that subclasses implement.
	
	/// Subclasses implement this to read in enough of the image stream
	// to figure out the width and height.
	abstract void readHeader(InputStream in) throws IOException;
	
	/// Subclasses implement this to return the width, or -1 if not known.
	abstract int getWidth();
	
	/// Subclasses implement this to return the height, or -1 if not known.
	abstract int getHeight();
	
	/// Subclasses implement this to read pixel data into the rgbRow
	// array, an int[width]. One int per pixel, no offsets or padding,
	// RGBdefault (AARRGGBB) color model.
	abstract void readRow(InputStream in, int row, int[] rgbRow) throws IOException;
	
	// Our own methods.
	
	void readImage() {
		try {
			readHeader(in);
			width = getWidth();
			height = getHeight();
			if(width == -1 || height == -1)
				err = true;
			else {
				rowsRead = new boolean[height];
				for(int row = 0; row < height; ++row)
					rowsRead[row] = false;
				gotSize = true;
				notifyThem();
				rgbPixels = new int[height][width];
				for(int row = 0; row < height; ++row) {
					readRow(in, row, rgbPixels[row]);
					rowsRead[row] = true;
					notifyThem();
				}
			}
		} catch(IOException e) {
			err = true;
			width = -1;
			height = -1;
			rowsRead = null;
			rgbPixels = null;
		}
	}
	
	private synchronized void notifyThem() {
		notifyAll();
	}
	
	void sendImage() {
		// Grab the list of consumers, in case it changes while we're sending.
		ImageConsumer[] c = new ImageConsumer[consumers.size()];
		int i;
		for(i = 0; i < c.length; ++i)
			c[i] = (ImageConsumer) consumers.elementAt(i);
		// Try to be as parallel as possible.
		waitForSize();
		for(i = 0; i < c.length; ++i)
			sendHead(c[i]);
		for(int row = 0; row < height; ++row)
			for(i = 0; i < c.length; ++i)
				sendPixelRow(c[i], row);
		for(i = 0; i < c.length; ++i)
			sendTail(c[i]);
		producing = false;
	}
	
	private synchronized void waitForSize() {
		while((!err) && (!gotSize)) {
			try {
				wait();
			} catch(InterruptedException ignore) {
			}
		}
	}
	
	private synchronized void waitForRow(int row) {
		while((!err) && (!rowsRead[row])) {
			try {
				wait();
			} catch(InterruptedException ignore) {
			}
		}
	}
	
	private void sendHead(ImageConsumer ic) {
		if(err)
			return;
		ic.setDimensions(width, height);
		ic.setColorModel(model);
		ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES | ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME);
	}
	
	private void sendPixelRow(ImageConsumer ic, int row) {
		if(err)
			return;
		waitForRow(row);
		if(err)
			return;
		ic.setPixels(0, row, width, 1, model, rgbPixels[row], 0, width);
	}
	
	private void sendTail(ImageConsumer ic) {
		if(err)
			ic.imageComplete(ImageConsumer.IMAGEERROR);
		else
			ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
		
	}
	
	// Methods from ImageProducer.
	
	/// This method is used to register an ImageConsumer with the
	// ImageProducer for access to the image data during a later
	// reconstruction of the Image. The ImageProducer may, at its
	// discretion, start delivering the image data to the consumer
	// using the ImageConsumer interface immediately, or when the
	// next available image reconstruction is triggered by a call
	// to the startProduction method.
	// @see #startProduction
	public void addConsumer(ImageConsumer ic) {
		if(ic != null && !isConsumer(ic))
			consumers.addElement(ic);
	}
	
	/// This method determines if a given ImageConsumer object
	// is currently registered with this ImageProducer as one
	// of its consumers.
	public boolean isConsumer(ImageConsumer ic) {
		return consumers.contains(ic);
	}
	
	/// This method removes the given ImageConsumer object
	// from the list of consumers currently registered to
	// receive image data. It is not considered an error
	// to remove a consumer that is not currently registered.
	// The ImageProducer should stop sending data to this
	// consumer as soon as is feasible.
	public void removeConsumer(ImageConsumer ic) {
		consumers.removeElement(ic);
	}
	
	/// This method both registers the given ImageConsumer object
	// as a consumer and starts an immediate reconstruction of
	// the image data which will then be delivered to this
	// consumer and any other consumer which may have already
	// been registered with the producer. This method differs
	// from the addConsumer method in that a reproduction of
	// the image data should be triggered as soon as possible.
	// @see #addConsumer
	public void startProduction(ImageConsumer ic) {
		addConsumer(ic);
		if(!startedRead) {
			startedRead = true;
			new ImageDecoderRead(this);
		}
		if(!producing) {
			producing = true;
			sendImage();
		}
	}
	
	/// This method is used by an ImageConsumer to request that
	// the ImageProducer attempt to resend the image data one
	// more time in TOPDOWNLEFTRIGHT order so that higher
	// quality conversion algorithms which depend on receiving
	// pixels in order can be used to produce a better output
	// version of the image. The ImageProducer is free to
	// ignore this call if it cannot resend the data in that
	// order. If the data can be resent, then the ImageProducer
	// should respond by executing the following minimum set of
	// ImageConsumer method calls:
	// <PRE>
	// ic.setHints( TOPDOWNLEFTRIGHT | [otherhints] );
	// ic.setPixels( [...] ); // as many times as needed
	// ic.imageComplete( [status] );
	// </PRE>
	// @see ImageConsumer#setHints
	public void requestTopDownLeftRightResend(ImageConsumer ic) {
		addConsumer(ic);
		waitForSize();
		sendHead(ic);
		for(int row = 0; row < height; ++row)
			sendPixelRow(ic, row);
		sendTail(ic);
	}
	
}

class ImageDecoderRead extends Thread {
	
	private ImageDecoder parent;
	
	public ImageDecoderRead(ImageDecoder parent) {
		this.parent = parent;
		start();
	}
	
	// Methods from Runnable.
	
	public void run() {
		parent.readImage();
	}
	
}
