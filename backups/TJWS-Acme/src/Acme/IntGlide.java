// IntGlide - randomly glide an int around within a specified range
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

package Acme;

import java.util.Random;

/// Randomly glide an int around within a specified range.
// <P>
// <A HREF="/resources/classes/Acme/IntGlide.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class IntGlide {
	private final int scale = 32;
	private int low, high, maxdelta, value;
	private int delta;
	private static Random random = new Random();
	
	/// Constructor.
	public IntGlide(int low, int high, int maxdelta, int value) {
		this.low = low * scale;
		this.high = high * scale;
		this.maxdelta = maxdelta * scale;
		this.value = value * scale;
		validate();
		newDelta();
	}
	
	/// Constructor, unspecified initial value.
	public IntGlide(int low, int high, int maxdelta) {
		this(low, high, maxdelta, (Math.abs(random.nextInt()) % (high - low + 1)) + low);
	}
	
	private void validate() {
		if(high < low) {
			int t = high;
			high = low;
			low = t;
		}
		if(maxdelta < 1)
			maxdelta = 1;
	}
	
	/// Return the next value in the sequence.
	public int next() {
		int newval;
		for(int i = 0; i < 10; ++i) {
			newval = value + delta;
			if(newval >= low && newval <= high) {
				value = newval;
				return value / scale;
			}
			newDelta();
		}
		if(value < low)
			value = low;
		else if(value > high)
			value = high;
		return value / scale;
	}
	
	private void newDelta() {
		delta = (Math.abs(random.nextInt()) % (maxdelta * 2)) - maxdelta;
		if(delta <= 0)
			--delta;
	}
}
