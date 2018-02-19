// Serializable - interface for serializable objects
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/// Interface for serializable objects.
// <P>
// This implementation of serialization is much simpler and less convenient
// to use than the official one that will be in JDK1.1; but it has the
// significant albeit temporary advantage that it works in current browsers.
// <P>
// One thing this version does not do is handle cyclic graphs of objects.
// It only handles tree-shaped graphs. If you need to serialize more
// complicated structures, consider using an ID-based scheme - instead of
// having your objects contain actual references to other objects, have
// them contain IDs which can be translated into real references by an
// ID-manager class. This scheme also has the advantage that you don't
// have to deserialize the entire graph all at once, you can do it piece
// by piece as needed, and you can even set up a least-recently-used
// flush policy in the ID-manager.
// <P>
// There are five simple steps to making your class serializable.
// <OL>
// <LI> Add "implements Acme.Serializable" to the class declaration.
// <LI> Make sure you have a no-arguments constructor that doesn't
// do anything permanent.
// <LI> Add a getVersion() method.
// <LI> Add a serialize() method that serializes all your class's variables.
// <LI> Add a deserialize() method that deserializes all your class's variables.
// </OL>
// <P>
// Here's an example of a class that uses this interface:
// <PRE><CODE>
// import java.io.*;
//
// public class Tree implements Acme.Serializable
// {
//
// long hash;
// Tree left, right;
//
// /// Real constructor.
// public Tree( long hash, Tree left, Tree right )
// {
// this.hash = hash;
// this.left = left;
// this.right = right;
// }
//
// /// No-args constructor, makes a blank instance to be filled in by the
// // deserializer.
// public Tree()
// {
// }
//
// /// Version routine.
// public String getVersion()
// {
// return "1";
// }
//
// /// Serialize routine.
// public void serialize( DataOutputStream dout ) throws IOException
// {
// Acme.SerialUtils.serializeLong( hash, dout );
// Acme.SerialUtils.serializeObject( left, dout );
// Acme.SerialUtils.serializeObject( right, dout );
// }
//
// /// Deserialize routine.
// public void deserialize( DataInputStream din ) throws IOException
// {
// hash = Acme.SerialUtils.deserializeLong( din );
// left = (Tree) Acme.SerialUtils.deserializeObject(
// this.getClass(), din );
// right = (Tree) Acme.SerialUtils.deserializeObject(
// this.getClass(), din );
// }
//
// }
// </CODE></PRE>
// <P>
// Once you've got your class all set up, you'll need some code to
// start the serialization / deserialization process going.
// Here are examples of that:
// <PRE><CODE>
// // Serialize.
// Tree t = [...];
// DataOutputStream dout = new DataOutputStream( System.out );
// try
// {
// SerialUtils.serializeObject( t, dout );
// dout.flush();
// }
// catch ( IOException e )
// {
// System.err.println( e.toString() );
// }
//
// // Deserialize.
// Tree t = new Tree();
// DataInputStream din = new DataInputStream( System.in );
// try
// {
// t = (Tree) SerialUtils.deserializeObject( t.getClass(), din );
// }
// catch ( IOException e )
// {
// System.err.println( e.toString() );
// }
// </CODE></PRE>
// <P>
// <A HREF="/resources/classes/Acme/Serializable.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see SerialUtils

public interface Serializable extends Versioned {
	
	/// Write a serial representation of the object to the stream.
	public void serialize(DataOutputStream dout) throws IOException;
	
	/// Read a serial representation of the object from the stream.
	// The object must have already been constructed, presumably by
	// an empty no-arguments constructor. This routine just fills
	// in the fields.
	public void deserialize(DataInputStream din) throws IOException;
	
}
