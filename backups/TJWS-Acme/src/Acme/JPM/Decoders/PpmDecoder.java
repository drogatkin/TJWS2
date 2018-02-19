// PpmDecoder - read in a PPM image
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/// Read in a PPM image.
// <P>
// <A HREF="/resources/classes/Acme/JPM/Decoders/PpmDecoder.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.JPM.Encoders.PpmEncoder

public class PpmDecoder extends ImageDecoder {
	
	/// Constructor.
	// @param in The stream to read the bytes from.
	public PpmDecoder(InputStream in) {
		super(in);
	}
	
	private int type;
	private static final int PBM_ASCII = 1;
	private static final int PGM_ASCII = 2;
	private static final int PPM_ASCII = 3;
	private static final int PBM_RAW = 4;
	private static final int PGM_RAW = 5;
	private static final int PPM_RAW = 6;
	
	private int width = -1, height = -1;
	private int maxval;
	
	/// Subclasses implement this to read in enough of the image stream
	// to figure out the width and height.
	void readHeader(InputStream in) throws IOException {
		char c1, c2;
		
		c1 = (char) readByte(in);
		c2 = (char) readByte(in);
		
		if(c1 != 'P')
			throw new IOException("not a PBM/PGM/PPM file");
		switch(c2) {
			case '1':
				type = PBM_ASCII;
				break;
			case '2':
				type = PGM_ASCII;
				break;
			case '3':
				type = PPM_ASCII;
				break;
			case '4':
				type = PBM_RAW;
				break;
			case '5':
				type = PGM_RAW;
				break;
			case '6':
				type = PPM_RAW;
				break;
			default:
				throw new IOException("not a standard PBM/PGM/PPM file");
		}
		width = readInt(in);
		height = readInt(in);
		if(type != PBM_ASCII && type != PBM_RAW)
			maxval = readInt(in);
	}
	
	/// Subclasses implement this to return the width, or -1 if not known.
	int getWidth() {
		return width;
	}
	
	/// Subclasses implement this to return the height, or -1 if not known.
	int getHeight() {
		return height;
	}
	
	/// Subclasses implement this to read pixel data into the rgbRow
	// array, an int[width]. One int per pixel, no offsets or padding,
	// RGBdefault (AARRGGBB) color model
	void readRow(InputStream in, int row, int[] rgbRow) throws IOException {
		int col, r, g, b;
		int rgb = 0;
		char c;
		
		for(col = 0; col < width; ++col) {
			switch(type) {
				case PBM_ASCII:
					c = readChar(in);
					if(c == '1')
						rgb = 0xff000000;
					else if(c == '0')
						rgb = 0xffffffff;
					else
						throw new IOException("illegal PBM bit");
					break;
				case PGM_ASCII:
					g = readInt(in);
					rgb = makeRgb(g, g, g);
					break;
				case PPM_ASCII:
					r = readInt(in);
					g = readInt(in);
					b = readInt(in);
					rgb = makeRgb(r, g, b);
					break;
				case PBM_RAW:
					if(readBit(in))
						rgb = 0xff000000;
					else
						rgb = 0xffffffff;
					break;
				case PGM_RAW:
					g = readByte(in);
					if(maxval != 255)
						g = fixDepth(g);
					rgb = makeRgb(g, g, g);
					break;
				case PPM_RAW:
					r = readByte(in);
					g = readByte(in);
					b = readByte(in);
					if(maxval != 255) {
						r = fixDepth(r);
						g = fixDepth(g);
						b = fixDepth(b);
					}
					rgb = makeRgb(r, g, b);
					break;
			}
			rgbRow[col] = rgb;
		}
	}
	
	/// Utility routine to read a byte. Instead of returning -1 on
	// EOF, it throws an exception.
	private static int readByte(InputStream in) throws IOException {
		int b = in.read();
		if(b == -1)
			throw new EOFException();
		return b;
	}
	
	private int bitshift = -1;
	private int bits;
	
	/// Utility routine to read a bit, packed eight to a byte, big-endian.
	private boolean readBit(InputStream in) throws IOException {
		if(bitshift == -1) {
			bits = readByte(in);
			bitshift = 7;
		}
		boolean bit = (((bits >> bitshift) & 1) != 0);
		--bitshift;
		return bit;
	}
	
	/// Utility routine to read a character, ignoring comments.
	private static char readChar(InputStream in) throws IOException {
		char c;
		
		c = (char) readByte(in);
		if(c == '#') {
			do {
				c = (char) readByte(in);
			} while(c != '\n' && c != '\r');
		}
		
		return c;
	}
	
	/// Utility routine to read the first non-whitespace character.
	private static char readNonwhiteChar(InputStream in) throws IOException {
		char c;
		
		do {
			c = readChar(in);
		} while(c == ' ' || c == '\t' || c == '\n' || c == '\r');
		
		return c;
	}
	
	/// Utility routine to read an ASCII integer, ignoring comments.
	private static int readInt(InputStream in) throws IOException {
		char c;
		int i;
		
		c = readNonwhiteChar(in);
		if(c < '0' || c > '9')
			throw new IOException("junk in file where integer should be");
		
		i = 0;
		do {
			i = i * 10 + c - '0';
			c = readChar(in);
		} while(c >= '0' && c <= '9');
		
		return i;
	}
	
	/// Utility routine to rescale a pixel value from a non-eight-bit maxval.
	private int fixDepth(int p) {
		return (p * 255 + maxval / 2) / maxval;
	}
	
	/// Utility routine make an RGBdefault pixel from three color values.
	private static int makeRgb(int r, int g, int b) {
		return 0xff000000 | (r << 16) | (g << 8) | b;
	}
	
}
