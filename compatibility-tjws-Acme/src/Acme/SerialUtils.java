// SerialUtils - utilities for serializable objects
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

/// Utilities for serializable objects.
// <P>
// These static routines help you serialize and deserialize the primitive
// data types. Your own serialization routines will just be sequences of
// calls to these.
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
// <A HREF="/resources/classes/Acme/SerialUtils.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Serializable

public class SerialUtils {
	
	private static String tagNull = "Null";
	private static String tagString = "String";
	private static String tagArrayObject = "ArrayObject";
	private static String tagArrayByte = "ArrayByte";
	private static String tagArrayChar = "ArrayChar";
	private static String tagArrayShort = "ArrayShort";
	private static String tagArrayInt = "ArrayInt";
	private static String tagArrayLong = "ArrayLong";
	private static String tagArrayFloat = "ArrayFloat";
	private static String tagArrayDouble = "ArrayDouble";
	
	/// Utility routine to return a class identifier - name plus version.
	private static String getIdentifier(Class cl) {
		// We have to create a temporary instance just to call getVersion(),
		// because Java currently lacks a way to call static methods given
		// a Class.
		try {
			Object o = cl.newInstance();
			if(o instanceof Acme.Versioned) {
				Acme.Versioned v = (Acme.Versioned) o;
				return cl.getName() + " " + v.getVersion();
			}
		} catch(IllegalAccessException e) {
		} catch(InstantiationException e) {
		}
		// Instantiation problem, or it's not versionable; just use class name.
		return cl.getName();
	}
	
	/// Utility routine to write a serialization header.
	private static void serializeHeaderString(String str, DataOutputStream dout) throws IOException {
		dout.writeUTF("[" + str);
	}
	
	/// Utility routine to write a serialization header.
	private static void serializeHeader(Class cl, DataOutputStream dout) throws IOException {
		serializeHeaderString(getIdentifier(cl), dout);
	}
	
	/// Utility routine to write a serialization trailer.
	private static void serializeTrailerString(String str, DataOutputStream dout) throws IOException {
		// dout.writeUTF( str + "]" );
		dout.writeUTF("]");
	}
	
	/// Utility routine to write a serialization trailer.
	private static void serializeTrailer(Class cl, DataOutputStream dout) throws IOException {
		serializeTrailerString(getIdentifier(cl), dout);
	}
	
	/// Utility routine to serialize a sub-object.
	private static void serializeNull(DataOutputStream dout) throws IOException {
		serializeHeaderString(tagNull, dout);
		serializeTrailerString(tagNull, dout);
	}
	
	/// Utility routine to serialize a sub-object.
	public static void serializeObject(Acme.Serializable ser, DataOutputStream dout) throws IOException {
		if(ser == null)
			serializeNull(dout);
		else {
			serializeHeader(ser.getClass(), dout);
			ser.serialize(dout);
			serializeTrailer(ser.getClass(), dout);
		}
	}
	
	/// Utility routine to serialize a String.
	public static void serializeString(String str, DataOutputStream dout) throws IOException {
		if(str == null)
			serializeNull(dout);
		else {
			serializeHeaderString(tagString, dout);
			dout.writeUTF(str);
			serializeTrailerString(tagString, dout);
		}
	}
	
	/// Utility routine to serialize a boolean.
	public static void serializeBoolean(boolean b, DataOutputStream dout) throws IOException {
		dout.writeBoolean(b);
	}
	
	/// Utility routine to serialize a byte.
	public static void serializeByte(byte b, DataOutputStream dout) throws IOException {
		dout.writeByte(b);
	}
	
	/// Utility routine to serialize a char.
	public static void serializeChar(char c, DataOutputStream dout) throws IOException {
		dout.writeChar(c);
	}
	
	/// Utility routine to serialize a short.
	public static void serializeShort(short s, DataOutputStream dout) throws IOException {
		dout.writeShort(s);
	}
	
	/// Utility routine to serialize a int.
	public static void serializeInt(int i, DataOutputStream dout) throws IOException {
		dout.writeInt(i);
	}
	
	/// Utility routine to serialize a long.
	public static void serializeLong(long l, DataOutputStream dout) throws IOException {
		dout.writeLong(l);
	}
	
	/// Utility routine to serialize a float.
	public static void serializeFloat(float f, DataOutputStream dout) throws IOException {
		dout.writeFloat(f);
	}
	
	/// Utility routine to serialize a double.
	public static void serializeDouble(double d, DataOutputStream dout) throws IOException {
		dout.writeDouble(d);
	}
	
	/// Utility routine to serialize an array of Objects.
	public static void serializeArrayObject(Serializable[] ao, DataOutputStream dout) throws IOException {
		if(ao == null)
			serializeNull(dout);
		else {
			serializeHeaderString(tagArrayObject, dout);
			dout.writeShort(ao.length);
			for(int i = 0; i < ao.length; ++i)
				serializeObject(ao[i], dout);
			serializeTrailerString(tagArrayObject, dout);
		}
	}
	
	/// Utility routine to serialize an array of bytes.
	public static void serializeArrayByte(byte[] ab, DataOutputStream dout) throws IOException {
		if(ab == null)
			serializeNull(dout);
		else {
			serializeHeaderString(tagArrayByte, dout);
			dout.writeShort(ab.length);
			for(int i = 0; i < ab.length; ++i)
				dout.writeByte(ab[i]);
			serializeTrailerString(tagArrayByte, dout);
		}
	}
	
	/// Utility routine to serialize an array of chars.
	public static void serializeArrayChar(char[] ac, DataOutputStream dout) throws IOException {
		if(ac == null)
			serializeNull(dout);
		else {
			serializeHeaderString(tagArrayChar, dout);
			dout.writeShort(ac.length);
			for(int i = 0; i < ac.length; ++i)
				dout.writeChar(ac[i]);
			serializeTrailerString(tagArrayChar, dout);
		}
	}
	
	/// Utility routine to serialize an array of shorts.
	public static void serializeArrayShort(short[] as, DataOutputStream dout) throws IOException {
		if(as == null)
			serializeNull(dout);
		else {
			serializeHeaderString(tagArrayShort, dout);
			dout.writeShort(as.length);
			for(int i = 0; i < as.length; ++i)
				dout.writeShort(as[i]);
			serializeTrailerString(tagArrayShort, dout);
		}
	}
	
	/// Utility routine to serialize an array of ints.
	public static void serializeArrayInt(int[] ai, DataOutputStream dout) throws IOException {
		if(ai == null)
			serializeNull(dout);
		else {
			serializeHeaderString(tagArrayInt, dout);
			dout.writeShort(ai.length);
			for(int i = 0; i < ai.length; ++i)
				dout.writeInt(ai[i]);
			serializeTrailerString(tagArrayInt, dout);
		}
	}
	
	/// Utility routine to serialize an array of longs.
	public static void serializeArrayLong(long[] al, DataOutputStream dout) throws IOException {
		if(al == null)
			serializeNull(dout);
		else {
			serializeHeaderString(tagArrayLong, dout);
			dout.writeShort(al.length);
			for(int i = 0; i < al.length; ++i)
				dout.writeLong(al[i]);
			serializeTrailerString(tagArrayLong, dout);
		}
	}
	
	/// Utility routine to serialize an array of floats.
	public static void serializeArrayFloat(float[] af, DataOutputStream dout) throws IOException {
		if(af == null)
			serializeNull(dout);
		else {
			serializeHeaderString(tagArrayFloat, dout);
			dout.writeShort(af.length);
			for(int i = 0; i < af.length; ++i)
				dout.writeFloat(af[i]);
			serializeTrailerString(tagArrayFloat, dout);
		}
	}
	
	/// Utility routine to serialize an array of doubles.
	public static void serializeArrayDouble(double[] ad, DataOutputStream dout) throws IOException {
		if(ad == null)
			serializeNull(dout);
		else {
			serializeHeaderString(tagArrayDouble, dout);
			dout.writeShort(ad.length);
			for(int i = 0; i < ad.length; ++i)
				dout.writeDouble(ad[i]);
			serializeTrailerString(tagArrayDouble, dout);
		}
	}
	
	/// Utility routine to read a serialization header.
	// Special case: returns false if the object is null.
	private static boolean deserializeHeaderString(String str, DataInputStream din) throws IOException {
		String s2 = din.readUTF();
		if(s2.equals("[" + tagNull))
			return false;
		if(!s2.equals("[" + str))
			throw new IOException("bogus serialization header");
		return true;
	}
	
	/// Utility routine to read a serialization header.
	// Special case: returns false if the object is null.
	public static boolean deserializeHeader(Class cl, DataInputStream din) throws IOException {
		return deserializeHeaderString(getIdentifier(cl), din);
	}
	
	/// Utility routine to read a serialization trailer.
	private static void deserializeTrailerString(String str, DataInputStream din) throws IOException {
		String s2 = din.readUTF();
		// if ( ! s2.equals( str + "]" ) )
		// throw new IOException( "bogus serialization trailer" );
		if(!s2.equals("]"))
			throw new IOException("bogus serialization trailer");
	}
	
	/// Utility routine to read a serialization trailer.
	public static void deserializeTrailer(Class cl, DataInputStream din) throws IOException {
		deserializeTrailerString(getIdentifier(cl), din);
	}
	
	/// Utility routine to deserialize a sub-object.
	public static Serializable deserializeObject(Class cl, DataInputStream din) throws IOException {
		Serializable o;
		if(deserializeHeader(cl, din)) {
			try {
				o = (Serializable) cl.newInstance();
			} catch(IllegalAccessException e) {
				throw new IOException(e.toString());
			} catch(InstantiationException e) {
				throw new IOException(e.toString());
			}
			o.deserialize(din);
		} else
			o = null;
		deserializeTrailer(cl, din);
		return o;
	}
	
	/// Utility routine to deserialize a String.
	public static String deserializeString(DataInputStream din) throws IOException {
		String str;
		if(deserializeHeaderString(tagString, din))
			str = din.readUTF();
		else
			str = null;
		deserializeTrailerString(tagString, din);
		return str;
	}
	
	/// Utility routine to deserialize a boolean.
	public static boolean deserializeBoolean(DataInputStream din) throws IOException {
		return din.readBoolean();
	}
	
	/// Utility routine to deserialize a byte.
	public static byte deserializeByte(DataInputStream din) throws IOException {
		return din.readByte();
	}
	
	/// Utility routine to deserialize a char.
	public static char deserializeChar(DataInputStream din) throws IOException {
		return din.readChar();
	}
	
	/// Utility routine to deserialize a short.
	public static short deserializeShort(DataInputStream din) throws IOException {
		return din.readShort();
	}
	
	/// Utility routine to deserialize a int.
	public static int deserializeInt(DataInputStream din) throws IOException {
		return din.readInt();
	}
	
	/// Utility routine to deserialize a long.
	public static long deserializeLong(DataInputStream din) throws IOException {
		return din.readLong();
	}
	
	/// Utility routine to deserialize a float.
	public static float deserializeFloat(DataInputStream din) throws IOException {
		return din.readFloat();
	}
	
	/// Utility routine to deserialize a double.
	public static double deserializeDouble(DataInputStream din) throws IOException {
		return din.readDouble();
	}
	
	/// Utility routine to deserialize an array of objects.
	public static Serializable[] deserializeArrayObject(DataInputStream din) throws IOException {
		Serializable[] ao;
		if(deserializeHeaderString(tagArrayObject, din)) {
			short len = din.readShort();
			ao = new Serializable[len];
			for(int i = 0; i < len; ++i)
				ao[i] = deserializeObject(null, din);	// doesn't actually work
		} else
			ao = null;
		deserializeTrailerString(tagArrayObject, din);
		return ao;
	}
	
	/// Utility routine to deserialize an array of bytes.
	public static byte[] deserializeArrayByte(DataInputStream din) throws IOException {
		byte[] ab;
		if(deserializeHeaderString(tagArrayByte, din)) {
			short len = din.readShort();
			ab = new byte[len];
			for(int i = 0; i < len; ++i)
				ab[i] = din.readByte();
		} else
			ab = null;
		deserializeTrailerString(tagArrayByte, din);
		return ab;
	}
	
	/// Utility routine to deserialize an array of chars.
	public static char[] deserializeArrayChar(DataInputStream din) throws IOException {
		char[] ac;
		if(deserializeHeaderString(tagArrayChar, din)) {
			short len = din.readShort();
			ac = new char[len];
			for(int i = 0; i < len; ++i)
				ac[i] = din.readChar();
		} else
			ac = null;
		deserializeTrailerString(tagArrayChar, din);
		return ac;
	}
	
	/// Utility routine to deserialize an array of shorts.
	public static short[] deserializeArrayShort(DataInputStream din) throws IOException {
		short[] as;
		if(deserializeHeaderString(tagArrayShort, din)) {
			short len = din.readShort();
			as = new short[len];
			for(int i = 0; i < len; ++i)
				as[i] = din.readShort();
		} else
			as = null;
		deserializeTrailerString(tagArrayShort, din);
		return as;
	}
	
	/// Utility routine to deserialize an array of ints.
	public static int[] deserializeArrayInt(DataInputStream din) throws IOException {
		int[] ai;
		if(deserializeHeaderString(tagArrayInt, din)) {
			short len = din.readShort();
			ai = new int[len];
			for(int i = 0; i < len; ++i)
				ai[i] = din.readInt();
		} else
			ai = null;
		deserializeTrailerString(tagArrayInt, din);
		return ai;
	}
	
	/// Utility routine to deserialize an array of longs.
	public static long[] deserializeArrayLong(DataInputStream din) throws IOException {
		long[] al;
		if(deserializeHeaderString(tagArrayLong, din)) {
			short len = din.readShort();
			al = new long[len];
			for(int i = 0; i < len; ++i)
				al[i] = din.readLong();
		} else
			al = null;
		deserializeTrailerString(tagArrayLong, din);
		return al;
	}
	
	/// Utility routine to deserialize an array of floats.
	public static float[] deserializeArrayFloat(DataInputStream din) throws IOException {
		float[] af;
		if(deserializeHeaderString(tagArrayFloat, din)) {
			short len = din.readShort();
			af = new float[len];
			for(int i = 0; i < len; ++i)
				af[i] = din.readFloat();
		} else
			af = null;
		deserializeTrailerString(tagArrayFloat, din);
		return af;
	}
	
	/// Utility routine to deserialize an array of doubles.
	public static double[] deserializeArrayDouble(DataInputStream din) throws IOException {
		double[] ad;
		if(deserializeHeaderString(tagArrayDouble, din)) {
			short len = din.readShort();
			ad = new double[len];
			for(int i = 0; i < len; ++i)
				ad[i] = din.readDouble();
		} else
			ad = null;
		deserializeTrailerString(tagArrayDouble, din);
		return ad;
	}
	
	/// Test routine.
	public static void main(String[] args) {
		if(args.length == 1 && args[0].equals("-s")) {
			SerialUtilsTest t;
			long[] al1 = { 0, 1, 2 };
			long[] al2 = { 3, 4, 5, 6 };
			t = new SerialUtilsTest(null, "Yo", (byte) 23, '@', (short) 23456, 234567890, 234567890123L, 23.4567F, 23.4567890123D, al2);
			t = new SerialUtilsTest(t, "Hi there", (byte) 123, 'x', (short) 12345, 123456789, 123456789012L, 123.456F, 123.456789012D, al1);
			DataOutputStream dout = new DataOutputStream(System.out);
			try {
				SerialUtils.serializeObject(t, dout);
				dout.flush();
			} catch(IOException e) {
				System.err.println(e.toString());
				e.printStackTrace();
				System.exit(1);
			}
		} else if(args.length == 1 && args[0].equals("-d")) {
			SerialUtilsTest t = new SerialUtilsTest();
			DataInputStream din = new DataInputStream(System.in);
			try {
				t = (SerialUtilsTest) SerialUtils.deserializeObject(t.getClass(), din);
			} catch(IOException e) {
				System.err.println(e.toString());
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println(t.toString());
		} else {
			System.err.println("usage:  SerialUtils -s|-d");
			System.exit(1);
		}
		System.exit(0);
	}
	
}

/// Sample/test class.

class SerialUtilsTest implements Acme.Serializable {
	
	SerialUtilsTest sub;
	String str;
	byte b;
	char c;
	short s;
	int i;
	long l;
	float f;
	double d;
	long[] al;
	
	/// Real constructor.
	public SerialUtilsTest(SerialUtilsTest sub, String str, byte b, char c, short s, int i, long l, float f, double d, long[] al) {
		this.sub = sub;
		this.str = str;
		this.b = b;
		this.c = c;
		this.s = s;
		this.i = i;
		this.l = l;
		this.f = f;
		this.d = d;
		this.al = al;
	}
	
	/// No-args constructor, makes a blank instance to be filled in by the
	// deserializer.
	public SerialUtilsTest() {
	}
	
	/// Version routine.
	public String getVersion() {
		return "1";
	}
	
	/// Serialize routine for the interface.
	public void serialize(DataOutputStream dout) throws IOException {
		System.err.println("Serializing...");
		SerialUtils.serializeObject(sub, dout);
		SerialUtils.serializeString(str, dout);
		SerialUtils.serializeByte(b, dout);
		SerialUtils.serializeChar(c, dout);
		SerialUtils.serializeShort(s, dout);
		SerialUtils.serializeInt(i, dout);
		SerialUtils.serializeLong(l, dout);
		SerialUtils.serializeFloat(f, dout);
		SerialUtils.serializeDouble(d, dout);
		SerialUtils.serializeArrayLong(al, dout);
		System.err.println("Done serializing.");
	}
	
	/// Deserialize routine for the interface.
	public void deserialize(DataInputStream din) throws IOException {
		System.err.println("Deserializing...");
		sub = (SerialUtilsTest) SerialUtils.deserializeObject(this.getClass(), din);
		str = SerialUtils.deserializeString(din);
		b = SerialUtils.deserializeByte(din);
		c = SerialUtils.deserializeChar(din);
		s = SerialUtils.deserializeShort(din);
		i = SerialUtils.deserializeInt(din);
		l = SerialUtils.deserializeLong(din);
		f = SerialUtils.deserializeFloat(din);
		d = SerialUtils.deserializeDouble(din);
		al = SerialUtils.deserializeArrayLong(din);
		System.err.println("Done deserializing.");
	}
	
	/// String output routine.
	public String toString() {
		return "[" + this.getClass().getName() + " " + sub + " " + str.toString() + " " + b + " " + c + " " + s + " " + i + " " + l + " " + f + " " + Acme.Fmt.fmt(d) + " " + arrayToString(al) + "]";
	}
	
	private static String arrayToString(long[] al) {
		if(al == null)
			return "null";
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		for(int i = 0; i < al.length; ++i) {
			if(i != 0)
				sb.append(", ");
			sb.append(al[i]);
		}
		sb.append("}");
		return sb.toString();
	}
	
}
