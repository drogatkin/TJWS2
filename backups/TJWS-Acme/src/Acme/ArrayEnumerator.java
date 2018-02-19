// ArrayEnumerator - Enumeration for an array of objects
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

import java.util.Enumeration;
import java.util.NoSuchElementException;

/// Enumeration for an array of objects
// <P>
// Vectors come with a method to make an Enumeration, but if you're
// using arrays instead of Vectors you don't get that.
// Instead you can use this class to make an Enumeration for any array
// of Objects.
// It returns the contents of the array in order,
// optionally skipping any null elements.
// <P>
// <A HREF="/resources/classes/Acme/ArrayEnumerator.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class ArrayEnumerator implements Enumeration {
	
	private Object[] objects;
	
	private boolean skipNulls;
	private int i;
	private int next;
	private boolean foundNext;
	
	public ArrayEnumerator(Object[] objects) {
		this(objects, false);
	}
	
	public ArrayEnumerator(Object[] objects, boolean skipNulls) {
		this.skipNulls = skipNulls;
		this.objects = objects;
		i = -1;
		foundNext = false;
	}
	
	public boolean hasMoreElements() {
		findNextElement();
		return next != -1;
	}
	
	public Object nextElement() {
		findNextElement();
		if(next == -1)
			throw new NoSuchElementException("ArrayEnumerator");
		i = next;
		foundNext = false;
		return objects[i];
	}
	
	private void findNextElement() {
		if(foundNext)
			return;
		foundNext = true;
		for(next = i + 1; next < objects.length; ++next)
			if(objects[next] != null || !skipNulls)
				return;
		next = -1;
	}
	
}
