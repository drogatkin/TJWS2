// Rotate - rotate an image by some angle
//
// Copyright (C) 1997 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.
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

package Acme.JPM.Filters;

import java.awt.image.ImageProducer;

/// Rotate an image by some angle.
// <P>
// Rotates an image by the specified angle.
// The angle is in degrees measured counter-clockwise.
// It can be negative, but it should be between -90 and 90
// or the resulting image will be unreasonably large.
// Staying between -45 and 45 is best.
// <P>
// The rotation algorithm is Alan Paeth's three-shear method, described
// in "A Fast Algorithm for General Raster Rotation", Graphics Interface
// '86, pp. 77-81.
// <P>
// This filter is slow.
// <P>
// <A HREF="/resources/classes/Acme/JPM/Filters/Rotate.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class Rotate extends CompositeFilter {
	
	private double angle;
	
	/// Constructor.
	public Rotate(ImageProducer producer, double angle) {
		super(producer, new Shear(null, angle), new Flip(null, Flip.FLIP_XY));
		this.angle = angle * Math.PI / 180.0;
		double xshearfac = Math.tan(angle / 2.0);
		double yshearfac = Math.sin(angle);
	}
	
	// Main routine for command-line interface.
	public static void main(String[] args) {
		if(args.length != 1)
			usage();
		ImageFilterPlus filter = new Rotate(null, Integer.parseInt(args[0]));
		System.exit(ImageFilterPlus.filterStream(System.in, System.out, filter));
	}
	
	private static void usage() {
		System.err.println("usage: Rotate <angle>");
		System.exit(1);
	}
	
}
