// PostURLConnection - a URLConnection that implements POST
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

package Acme;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownServiceException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Vector;

/// A URLConnection that implements POST.
// <P>
// Some implementations of URLConnection, e.g. the one in Navigator 3.0,
// do not support POST. This is a stripped-down version that does.
// <P>
// Note that it can't inherit from java.net.URLConnection because that
// class has no public constructors. Not all the standard URLConnection
// methods are re-implemented here, just the ones necessary for posting.
// <P>
// This class is not needed in current browsers.
// <P>
// <A HREF="/resources/classes/Acme/PostURLConnection.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class PostURLConnection {
	
	private URL url;
	private boolean doInput = false;
	private boolean doOutput = true;
	private boolean useCaches = false;
	
	private Vector reqHeaderNames = new Vector();
	private Vector reqHeaderValues = new Vector();
	private Vector resHeaderNames = null;
	private Vector resHeaderValues = null;
	private Socket socket;
	private OutputStream out;
	private InputStream in;
	
	/// Constructs a POST URL connection to the specified URL.
	// @param url the specified URL
	public PostURLConnection(URL url) {
		this.url = url;
	}
	
	private boolean connected = false;
	
	public void connect() throws IOException {
		if(connected)
			return;
		if(!useCaches)
			setRequestProperty("Pragma", "no-cache");
		String protocol = url.getProtocol();
		if(!protocol.equals("http"))
			throw new UnknownServiceException("unknown protocol");
		String host = url.getHost();
		int port = url.getPort();
		if(port == -1)
			port = 80;
		String file = url.getFile();
		socket = new Socket(host, port);
		out = socket.getOutputStream();
		PrintStream pout = new PrintStream(out);
		String method;
		if(doOutput)
			method = "POST";
		else
			method = "GET";
		pout.println(method + " " + file + " HTTP/1.0");
		for(int i = 0; i < reqHeaderNames.size(); ++i) {
			String name = (String) reqHeaderNames.elementAt(i);
			String value = (String) reqHeaderValues.elementAt(i);
			pout.println(name + ": " + value);
		}
		pout.println("");
		pout.flush();
		connected = true;
	}
	
	private boolean inputStarted = false;
	
	private void startInput() throws IOException {
		connect();
		if(inputStarted)
			return;
		in = socket.getInputStream();
		resHeaderNames = new Vector();
		resHeaderValues = new Vector();
		DataInputStream din = new DataInputStream(in);
		String line;
		// Read and ignore the status line.
		line = din.readLine();
		// Read and save the header lines.
		while(true) {
			line = din.readLine();
			if(line == null || line.length() == 0)
				break;
			int colonBlank = line.indexOf(": ");
			if(colonBlank != -1) {
				String name = line.substring(0, colonBlank);
				String value = line.substring(colonBlank + 2);
				resHeaderNames.addElement(name.toLowerCase());
				resHeaderValues.addElement(value);
			}
		}
		inputStarted = true;
	}
	
	public void close() throws IOException {
		if(!connected)
			return;
		out.close();
		if(inputStarted)
			in.close();
		socket.close();
	}
	
	/// Gets the URL for this connection.
	public URL getURL() {
		return url;
	}
	
	// Gets the content length. Returns -1 if not known.
	public int getContentLength() throws IOException {
		return getHeaderFieldInt("content-length", -1);
	}
	
	/// Gets the content type. Returns null if not known.
	public String getContentType() throws IOException {
		return getHeaderField("content-type");
	}
	
	/// Gets a header field by name. Returns null if not known.
	// @param name the name of the header field
	public String getHeaderField(String name) throws IOException {
		if(resHeaderNames == null)
			startInput();
		int i = resHeaderNames.indexOf(name.toLowerCase());
		if(i == -1)
			return null;
		return (String) resHeaderValues.elementAt(i);
	}
	
	/// Gets a header field by name. Returns null if not known.
	// The field is parsed as an integer.
	// @param name the name of the header field
	// @param def the value to return if the field is missing or malformed.
	public int getHeaderFieldInt(String name, int def) throws IOException {
		try {
			return Integer.parseInt(getHeaderField(name));
		} catch(NumberFormatException t) {
			return def;
		}
	}
	
	/// Gets a header field by name. Returns null if not known.
	// The field is parsed as a date.
	// @param name the name of the header field
	// @param def the value to return if the field is missing or malformed.
	public long getHeaderFieldDate(String name, long def) throws IOException {
		try {
			return DateFormat.getDateInstance().parse(getHeaderField(name)).getTime();
		} catch(ParseException e) {
			throw new IOException(e.toString());
		}
	}
	
	/// Call this routine to get an InputStream that reads from the object.
	// @exception UnknownServiceException If the protocol does not support
	/// input.
	public InputStream getInputStream() throws IOException {
		if(!doInput)
			throw new UnknownServiceException("connection doesn't support input");
		startInput();
		return in;
	}
	
	/// Call this routine to get an OutputStream that writes to the object.
	// @exception UnknownServiceException If the protocol does not support
	/// output.
	public OutputStream getOutputStream() throws IOException {
		if(!doOutput)
			throw new UnknownServiceException("connection doesn't support output");
		connect();
		return out;
	}
	
	/// Returns the String representation of the URL connection.
	public String toString() {
		return this.getClass().getName() + ":" + url;
	}
	
	/// A URL connection can be used for input and/or output. Set the DoInput
	// flag to true if you intend to use the URL connection for input,
	// false if not. The default for PostURLConnections is false.
	public void setDoInput(boolean doInput) {
		if(connected)
			throw new IllegalAccessError("already connected");
		this.doInput = doInput;
	}
	
	public boolean getDoInput() {
		return doInput;
	}
	
	/// A URL connection can be used for input and/or output. Set the DoOutput
	// flag to true if you intend to use the URL connection for output,
	// false if not. The default for PostURLConnections is true.
	public void setDoOutput(boolean doOutput) {
		if(connected)
			throw new IllegalAccessError("already connected");
		this.doOutput = doOutput;
	}
	
	public boolean getDoOutput() {
		return doOutput;
	}
	
	/// Some protocols do caching of documents. Occasionally, it is important
	// to be able to "tunnel through" and ignore the caches (e.g. the "reload"
	// button in a browser). If the UseCaches flag on a connection is true,
	// the connection is allowed to use whatever caches it can. If false,
	// caches are to be ignored. The default for PostURLConnections is false.
	public void setUseCaches(boolean useCaches) {
		if(connected)
			throw new IllegalAccessError("already connected");
		this.useCaches = useCaches;
	}
	
	public boolean getUseCaches() {
		return useCaches;
	}
	
	// Sets/gets a general request property.
	// @param name The keyword by which the request is known (eg "accept")
	// @param value The value associated with it.
	public void setRequestProperty(String name, String value) {
		if(connected)
			throw new IllegalAccessError("already connected");
		reqHeaderNames.addElement(name);
		reqHeaderValues.addElement(value);
	}
	
	public String getRequestProperty(String name) {
		if(connected)
			throw new IllegalAccessError("already connected");
		int i = reqHeaderNames.indexOf(name);
		if(i == -1)
			return null;
		return (String) reqHeaderValues.elementAt(i);
	}
	
}
