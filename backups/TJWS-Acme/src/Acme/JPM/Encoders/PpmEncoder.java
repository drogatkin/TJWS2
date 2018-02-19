// PpmEncoder - write out an image as a PPM
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

package Acme.JPM.Encoders;

import java.awt.Image;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.io.OutputStream;

/// Write out an image as a PPM.
// <P>
// Writes an image onto a specified OutputStream in the PPM file format.
// <P>
// <A HREF="/resources/classes/Acme/JPM/Encoders/PpmEncoder.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see ToPpm

public class PpmEncoder extends ImageEncoder {
	
	/// Constructor.
	// @param img The image to encode.
	// @param out The stream to write the PPM to.
	public PpmEncoder(Image img, OutputStream out) throws IOException {
		super(img, out);
	}
	
	/// Constructor.
	// @param prod The ImageProducer to encode.
	// @param out The stream to write the PPM to.
	public PpmEncoder(ImageProducer prod, OutputStream out) throws IOException {
		super(prod, out);
	}
	
	void encodeStart(int width, int height) throws IOException {
		writeString(out, "P6\n");
		writeString(out, width + " " + height + "\n");
		writeString(out, "255\n");
	}
	
	static void writeString(OutputStream out, String str) throws IOException {
		byte[] buf = str.getBytes();
		out.write(buf);
	}
	
	void encodePixels(int x, int y, int w, int h, int[] rgbPixels, int off, int scansize) throws IOException {
		byte[] ppmPixels = new byte[w * 3];
		for(int row = 0; row < h; ++row) {
			int rowOff = off + row * scansize;
			for(int col = 0; col < w; ++col) {
				int i = rowOff + col;
				int j = col * 3;
				ppmPixels[j] = (byte) ((rgbPixels[i] & 0xff0000) >> 16);
				ppmPixels[j + 1] = (byte) ((rgbPixels[i] & 0x00ff00) >> 8);
				ppmPixels[j + 2] = (byte) (rgbPixels[i] & 0x0000ff);
			}
			out.write(ppmPixels);
		}
	}
	
	void encodeDone() throws IOException {
		// Nothing.
	}
	
}
