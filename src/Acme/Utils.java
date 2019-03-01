// Utils - assorted static utility routines
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@acme.com>. All rights reserved.
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
//
// Base64 code borrowed from public domain supported by Robert Harder
// Please visit <a
// href="http://iharder.net/base64">http://iharder.net/base64</a>
// periodically to check for updates or to contribute improvements.
//
// All enhancements Copyright (C)1998-2010 by Dmitriy Rogatkin
//
// $Id: Utils.java,v 1.39 2013/08/10 02:47:26 cvs Exp $

package Acme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.net.ssl.SSLParameters;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rslakra.logger.LogManager;

/// Assorted static utility routines.
// <P>
// Whenever I come up with a static routine that might be of general use,
// I put it here. So far the class includes:
// <UL>
// <LI> some string routines that were left out of java.lang.String
// <LI> a general array-to-string routine
// <LI> a fixed version of java.io.InputStream's byte-array read routine
// <LI> a bunch of URL-hacking routines
// <LI> some easy-to-use wrappers for Runtime.exec
// <LI> a debugging routine to dump the current call stack
// <LI> a URLDecoder to match java.net.URLEncoder
// </UL>
// and lots more.
// <P>
// <A HREF="/resources/classes/Acme/Utils.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.Z">Fetch the entire Acme package.</A>

public class Utils {
	/*
	 * Returns a date string formatted in Unix ls style - if it's within six
	 * months of now, Mmm dd hh:ss, else Mmm dd yyyy.
	 */
	static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm");
	static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat("MMM dd yyyy");
	
	public static final int COPY_BUF_SIZE = 4096 * 2;
	
	public static final Class<?>[] EMPTY_CLASSES = {};
	public static final Object[] EMPTY_OBJECTS = {};
	
	/**
	 * EMPTY_ENUMERATION
	 */
	public static final Enumeration<Object> EMPTY_ENUMERATION = new Enumeration<Object>() {
		public boolean hasMoreElements() {
			return false;
		}
		
		public Object nextElement() {
			return null;
		}
	};
	
	/**
	 * 
	 * @param date
	 * @return
	 */
	public static String lsDateStr(Date date) {
		if (Math.abs(System.currentTimeMillis() - date.getTime()) < 183L * 24L * 60L * 60L * 1000L) {
			return SHORT_DATE_FORMAT.format(date);
		} else {
			return LONG_DATE_FORMAT.format(date);
		}
	}
	
	/**
	 * 
	 * @param query
	 * @param encoding
	 * @return
	 */
	public static Hashtable<String, String[]> parseQueryString(final String query, String encoding) {
		Hashtable<String, String[]> parsedQueryString = new Hashtable<String, String[]>();
		if (IOHelper.isNullOrEmpty(encoding)) {
			encoding = IOHelper.UTF_8;
		}
		
		final StringTokenizer stringTokens = new StringTokenizer(query, "&");
		while (stringTokens.hasMoreTokens()) {
			String pair = stringTokens.nextToken();
			int ep = pair.indexOf('=');
			String key = ep > 0 ? pair.substring(0, ep) : pair;
			String value = ep > 0 ? pair.substring(ep + 1) : "";
			try {
				key = decode(key, encoding);
				if (value != null) {
					value = decode(value, encoding);
				}
			} catch (UnsupportedEncodingException ex) {
			}
			
			String[] values = parsedQueryString.get(key);
			String[] newValues;
			if (values == null) {
				newValues = new String[1];
				newValues[0] = value;
			} else {
				newValues = new String[values.length + 1];
				System.arraycopy(values, 0, newValues, 0, values.length);
				newValues[values.length] = value;
			}
			
			parsedQueryString.put(key, newValues);
		}
		
		return parsedQueryString;
	}
	
	/**
	 * 
	 * @param length
	 * @param inputStream
	 * @param encoding
	 * @param cachedStream
	 * @return
	 * @throws IOException
	 */
	public static Map<?, ?> parsePostData(long length, InputStream inputStream, String encoding, String[] cachedStream) throws IOException {
		// TODO: handle parsing data over 2 GB
		if (length > Integer.MAX_VALUE) {
			throw new RuntimeException("Can't process POST data over " + Integer.MAX_VALUE + ", requested: " + length);
		}
		
		byte[] buffer = new byte[(int) length];
		int filePointer = 0;
		while (filePointer < length) {
			int readBytes = inputStream.read(buffer, filePointer, buffer.length - filePointer);
			if (readBytes < 0) {
				break;
			}
			filePointer += readBytes;
		}
		
		// System.err.println("====>"+new String( buf));
		if (cachedStream != null && cachedStream.length > 0) {
			return parseQueryString(cachedStream[0] = new String(buffer, 0, filePointer, IOHelper.ISO_8859_1), encoding);
		} else {
			return parseQueryString(new String(buffer, 0, filePointer, IOHelper.ISO_8859_1), encoding);
		}
	}
	
	/**
	 * Decodes URL encoded string including newly introduced JavaScript encoding
	 * with %uxxxx chars
	 * 
	 * @param s
	 *            encoded string
	 * @param enc
	 *            source encoding
	 * @return decoded string or original if no decoding required
	 * @throws UnsupportedEncodingException
	 */
	public static String decode(String s, String enc) throws UnsupportedEncodingException {
		if (enc == null || enc.length() == 0) {
			throw new UnsupportedEncodingException("decode: no source char encoding provided.");
		}
		
		if (s == null) {
			return null;
		}
		
		boolean decoded = false;
		int l = s.length();
		StringBuffer sBuffer = new StringBuffer(l > 1024 ? l / 3 : l);
		
		int state = sText;
		int i = 0;
		int code = 0;
		char c;
		int pos = 0;
		int ofs = 0;
		byte[] buf = null;
		boolean processDig = false;
		while (i < l) {
			c = s.charAt(i);
			switch (c) {
				case '+':
					decoded = true;
					if (state == sText) {
						sBuffer.append(' ');
					} else if (state == s2Dig) {
						sBuffer.append(new String(buf, 0, pos + 1, enc));
						state = sText;
						sBuffer.append(' ');
					} else {
						new IllegalArgumentException("decode: unexpected + at pos: " + i + ", of : " + s);
					}
					break;
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					ofs = '0';
					processDig = true;
					break;
				case 'a':
				case 'b':
				case 'c':
				case 'd':
				case 'e':
				case 'f':
					ofs = 'a' - 10;
					processDig = true;
					break;
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
					ofs = 'A' - 10;
					processDig = true;
					break;
				case '%':
					decoded = true;
					if (state == sText) {
						state = sEscape;
						if (buf == null) {
							buf = new byte[(l - i) / 3];
						}
						pos = 0;
					} else if (state == s2Dig) {
						state = sEscape;
						pos++;
					} else {
						new IllegalArgumentException("decode: unexpected escape % at pos: " + i + ", of : " + s);
					}
					break;
				case 'u':
					if (state == sEscape) {
						if (pos > 0) {
							sBuffer.append(new String(buf, 0, pos, enc));
							pos = 0;
						}
						state = sU1;
					} else if (state == sText) {
						sBuffer.append(c);
					} else if (state == s2Dig) {
						sBuffer.append(new String(buf, 0, pos + 1, enc));
						state = sText;
						sBuffer.append(c);
					} else {
						new IllegalArgumentException("decode: unexpected char in hex at pos: " + i + ", of : " + s);
					}
					break;
				default:
					if (state == sText) {
						sBuffer.append(c);
					} else if (state == s2Dig) {
						sBuffer.append(new String(buf, 0, pos + 1, enc));
						state = sText;
						sBuffer.append(c);
					} else {
						new IllegalArgumentException("decode: unexpected char in hex at pos: " + i + ", of : " + s);
					}
					
					break;
			}
			i++;
			
			if (processDig) {
				if (state == sEscape) {
					code = c - ofs;
					state = s1Dig;
				} else if (state == s1Dig) {
					buf[pos] = (byte) (code * 16 + (c - ofs));
					state = s2Dig;
				} else if (state == s2Dig) { // escape finished
					sBuffer.append(new String(buf, 0, pos + 1, enc));
					state = sText;
					sBuffer.append(c);
				} else if (state == sU1) {
					code = c - ofs;
					state = sU2;
				} else if (state == sU2) {
					code = code * 16 + c - ofs;
					state = sU3;
				} else if (state == sU3) {
					code = code * 16 + c - ofs;
					state = sU4;
				} else if (state == sU4) {
					sBuffer.append((char) (code * 16 + c - ofs));
					state = sText;
				} else {
					sBuffer.append(c);
				}
				processDig = false;
			}
		}
		
		if (state == s2Dig) {
			sBuffer.append(new String(buf, 0, pos + 1, enc));
		}
		
		return (decoded ? sBuffer.toString() : s);
	}
	
	private static final int sText = 0;
	private static final int s1Dig = 1;
	private static final int s2Dig = 2;
	private static final int sEscape = 3;
	private static final int sU1 = 4;
	private static final int sU2 = 5;
	private static final int sU3 = 6;
	private static final int sU4 = 7;
	
	/**
	 * 
	 * @param s
	 * @param encodeWS
	 * @return
	 */
	public static String htmlEncode(String s, boolean encodeWS) {
		if (s == null) {
			return null;
		}
		
		char[] ca = s.toCharArray();
		StringBuffer res = new StringBuffer(ca.length);
		int ls = 0;
		boolean blankMet = true;
		for (int i = 0; i < ca.length; i++) {
			switch (ca[i]) {
				case '<':
					res.append(ca, ls, i - ls);
					res.append("&lt;");
					ls = i + 1;
					break;
				case '>':
					res.append(ca, ls, i - ls);
					res.append("&gt;");
					ls = i + 1;
					break;
				case '"':
					res.append(ca, ls, i - ls);
					res.append("&quot;");
					ls = i + 1;
					break;
				case '&':
					res.append(ca, ls, i - ls);
					res.append("&amp;");
					ls = i + 1;
					break;
				case ' ':
					if (blankMet && encodeWS) {
						res.append(ca, ls, i - ls);
						res.append("&nbsp;");
						ls = i + 1;
					} else
						blankMet = true;
					break;
				case '\n':
					if (encodeWS) {
						res.append(ca, ls, i - ls);
						res.append("<BR>");
						ls = i + 1;
					}
					break;
				case '\r':
					if (encodeWS) {
						res.append(ca, ls, i - ls);
						ls = i + 1;
					}
					break;
				default:
					if (ca[i] > 127) { // no unicode
						res.append(ca, ls, i - ls);
						res.append("&#").append((int) ca[i]).append(';');
						ls = i + 1;
					}
					blankMet = false;
			}
		}
		
		if (ls < ca.length) {
			res.append(ca, ls, ca.length - ls);
		}
		
		return res.toString();
	}
	
	/**
	 * 
	 * @param contentEncoding
	 * @return
	 */
	public static float isGzipAccepted(String contentEncoding) {
		float result = 0f;
		if (contentEncoding != null) {
			int gzsl = "gzip;".length();
			int zp = contentEncoding.indexOf("gzip");
			if (zp >= 0) {
				if (contentEncoding.length() > (zp + gzsl) && contentEncoding.charAt(zp + gzsl) == ';') {
					zp = contentEncoding.indexOf("q=", zp + gzsl);
					if (zp > 0) {
						int qe = contentEncoding.indexOf(",", zp);
						if (qe < 0)
							qe = contentEncoding.length();
						try {
							result = Float.parseFloat(contentEncoding.substring(zp + 2, qe));
						} catch (NumberFormatException e) {
						}
					}
				} else
					result = 1f;
			}
		}
		return result;
	}
	
	// / Checks whether a string matches a given wildcard pattern.
	// Only does ? and *, and multiple patterns separated by |.
	public static boolean match(String pattern, String string) {
		for (int p = 0;; ++p) {
			for (int s = 0;; ++p, ++s) {
				boolean sEnd = (s >= string.length());
				boolean pEnd = (p >= pattern.length() || pattern.charAt(p) == '|');
				if (sEnd && pEnd) {
					return true;
				}
				if (sEnd || pEnd) {
					break;
				}
				if (pattern.charAt(p) == '?') {
					continue;
				}
				if (pattern.charAt(p) == '*') {
					int i;
					++p;
					for (i = string.length(); i >= s; --i) {
						if (match(pattern.substring(p), string.substring(i))) {
							// not quite right
							return true;
						}
					}
					break;
				}
				
				if (pattern.charAt(p) != string.charAt(s)) {
					break;
				}
			}
			
			p = pattern.indexOf('|', p);
			if (p == -1) {
				return false;
			}
		}
	}
	
	/*
	 * Finds the maximum length of a string that matches a given wildcard
	 * pattern. Only does ? and *, and multiple patterns separated by |.
	 */
	public static int matchSpan(String pattern, String string) {
		int result = 0;
		final StringTokenizer stringTokenizer = new StringTokenizer(pattern, "|");
		while (stringTokenizer.hasMoreTokens()) {
			final int length = matchSpan1(stringTokenizer.nextToken(), string);
			if (length > result) {
				result = length;
			}
		}
		
		return result;
	}
	
	static int matchSpan1(String pattern, String string) {
		int p = 0;
		for (; p < string.length() && p < pattern.length(); p++) {
			if (pattern.charAt(p) == string.charAt(p)) {
				continue;
			}
			
			if (pattern.charAt(p) == '*') {
				return p - 1;
			}
			
			return 0;
		}
		
		return p < (pattern.length() - 1) ? -1 : p;
	}
	
	// / Turns a String into an array of Strings, by using StringTokenizer
	// to split it up at whitespace.
	public static String[] splitStr(String str) {
		StringTokenizer st = new StringTokenizer(str);
		int n = st.countTokens();
		String[] strs = new String[n];
		for (int i = 0; i < n; ++i) {
			strs[i] = st.nextToken();
		}
		
		return strs;
	}
	
	// / Turns a String into an array of Strings, by splitting it at
	// the specified character. This does not use StringTokenizer,
	// and therefore can handle empty fields.
	public static String[] splitStr(String str, char delim) {
		int n = 1;
		int index = -1;
		while (true) {
			index = str.indexOf(delim, index + 1);
			if (index == -1) {
				break;
			}
			++n;
		}
		String[] strs = new String[n];
		index = -1;
		for (int i = 0; i < n - 1; ++i) {
			int nextIndex = str.indexOf(delim, index + 1);
			strs[i] = str.substring(index + 1, nextIndex);
			index = nextIndex;
		}
		strs[n - 1] = str.substring(index + 1);
		return strs;
	}
	
	public static String[] splitStr(String str, String quotes) {
		char[] ca = str.toCharArray();
		// List result = new ArrayList(10);
		String[] result = new String[0];
		boolean inArg = false;
		boolean quoted = false;
		int argStart = -1;
		for (int i = 0; i < ca.length; i++) {
			char c = ca[i];
			if (inArg) {
				if (quoted) {
					if (quotes.indexOf(c) >= 0) {
						result = copyOf(result, result.length + 1);
						result[result.length - 1] = new String(ca, argStart, i - argStart);
						argStart = -1;
						quoted = false;
						inArg = false;
					}
				} else {
					if (c == ' ') {
						result = copyOf(result, result.length + 1);
						result[result.length - 1] = new String(ca, argStart, i - argStart);
						argStart = -1;
						inArg = false;
					}
				}
			} else {
				if (c != ' ') {
					inArg = true;
					if (quotes.indexOf(c) >= 0) {
						quoted = true;
						argStart = i + 1;
					} else
						argStart = i;
				}
			}
		}
		
		if (argStart > 0) {
			result = copyOf(result, result.length + 1);
			result[result.length - 1] = new String(ca, argStart, ca.length - argStart);
		}
		// for(int i=0;i<result.length;i++)
		// System.err.println("Param["+i+"]="+result[i]);
		return result;
	}
	
	public static String[] copyOf(String[] original, int newLength) {
		return copyOfRange(original, 0, newLength);
	}
	
	public static String[] copyOfRange(String[] original, int from, int newLength) {
		String[] copy = new String[newLength];
		newLength = Math.min(original.length - from, newLength);
		System.arraycopy(original, from, copy, 0, newLength);
		return copy;
	}
	
	/*
	 * public static Object[] copyOf(Object[] original, int from, int newLength)
	 * {
	 * newLength = Math.min(original.length - from, newLength);
	 * Object[] copy = new Object[newLength];
	 * System.arraycopy(original, from, copy, 0, newLength);
	 * return copy;
	 * }
	 */
	public static String canonicalizePath(String path) {
		if (IOHelper.isNullOrEmpty(path)) {
			return path;
		}
		
		List<String> pathElements = new ArrayList<String>(6);
		char[] pathChars = path.toCharArray();
		int n = pathChars.length;
		int startIndex = -1;
		int lev = 0;
		for (int i = 0; i < n; i++) {
			if (startIndex < 0) {
				if (pathChars[i] != '/' && pathChars[i] != '\\') {
					startIndex = i;
				}
			} else {
				boolean found = false;
				if (pathChars[i] == '?') {
					found = true;
				}
				
				if (pathChars[i] == '/' || pathChars[i] == '\\' || found) {
					String element = new String(pathChars, startIndex, i - startIndex);
					if (element.equals("..")) {
						if (pathElements.size() > 0) {
							pathElements.remove(pathElements.size() - 1);
						} else {
							lev--;
						}
						// else exception ?
					} else if (element.equals(".") == false)
						if (lev >= 0) {
							pathElements.add(element);
						} else {
							lev++;
						}
					
					if (found) {
						startIndex = i;
						break;
					}
					startIndex = -1;
				}
			}
		}
		
		if (startIndex > 0) {
			String el = new String(pathChars, startIndex, n - startIndex);
			if (el.equals("..")) {
				if (pathElements.size() > 0) {
					pathElements.remove(pathElements.size() - 1);
				}
				// else exception ?
			} else if (el.equals(".") == false) {
				if (lev >= 0) {
					pathElements.add(el);
				}
			}
		} else {
			pathElements.add("");
		}
		
		if (pathElements.size() == 0) {
			return lev >= 0 ? "" : null;
		}
		
		StringBuffer result = new StringBuffer(n);
		result.append(pathElements.get(0));
		n = pathElements.size();
		for (int i = 1; i < n; i++) {
			result.append('/').append(pathElements.get(i));
		}
		
		// System.err.println("Before "+path+" after "+result);
		return result.toString();
	}
	
	// / Copy the input to the output until EOF.
	public static long copyStream(InputStream in, OutputStream out, long maxLen) throws IOException {
		byte[] buf = new byte[COPY_BUF_SIZE];
		int len;
		long total = 0;
		if (maxLen <= 0) {
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
				total += len;
			}
		} else {
			while ((len = in.read(buf)) > 0) {
				if (len <= maxLen) {
					out.write(buf, 0, len);
					maxLen -= len;
					total += len;
				} else {
					out.write(buf, 0, (int) maxLen);
					total += maxLen;
					break;
				}
			}
		}
		
		return total;
	}
	
	// / Copy the input to the output until EOF.
	public static void copyStream(Reader in, Writer out) throws IOException {
		char[] buf = new char[COPY_BUF_SIZE];
		int len;
		while ((len = in.read(buf)) != -1) {
			out.write(buf, 0, len);
		}
	}
	
	// / Copy the input to the output until EOF.
	public static void copyStream(Reader in, OutputStream out, String charSet) throws IOException {
		char[] buf = new char[4096];
		int len;
		if (charSet == null) {
			while ((len = in.read(buf)) != -1) {
				out.write(new String(buf, 0, len).getBytes());
			}
		} else {
			while ((len = in.read(buf)) != -1) {
				out.write(new String(buf, 0, len).getBytes(charSet));
			}
		}
	}
	
	protected final static char BASE64ARRAY[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };
	
	/**
	 * base 64 encoding, string converted to bytes using specified encoding
	 * 
	 * @param String
	 *            <val>textString</val> original string to encode
	 * @param String
	 *            encoding, can be null, then iso-8859-1 used
	 * @return String result of encoding as iso-8859-1 string<br>
	 *         return null in case of invalid encoding or original string null
	 * @exception no
	 *                exceptions
	 */
	public final static String base64Encode(String textString, String encoding) {
		if (IOHelper.isNull(textString)) {
			return null;
		} else if (!IOHelper.isNullOrEmpty(textString)) {
			if (IOHelper.isNullOrEmpty(encoding)) {
				encoding = IOHelper.ISO_8859_1;
			}
			try {
				return base64Encode(textString.getBytes(encoding));
			} catch (Exception ex) {
				LogManager.error(ex);
			}
		}
		
		return null;
	}
	
	/**
	 * base 64 encoding, array of bytes converted to bytes using specified
	 * encoding
	 * 
	 * @param String
	 *            <val>_s</val> original string to encode
	 * @param String
	 *            encoding, can be null, then iso-8859-1 used
	 * @return String result of encoding as iso-8859-1 string<br>
	 * 
	 * @exception <code>NullPointerException</code>
	 *                if input parameter is null
	 */
	public final static String base64Encode(byte[] _bytes) {
		StringBuffer encodedBuffer = new StringBuffer((int) (_bytes.length * 1.5));
		int i = 0;
		int pad = 0;
		while (i < _bytes.length) {
			int b1 = (0xFF & _bytes[i++]);
			int b2;
			int b3;
			if (i >= _bytes.length) {
				b2 = 0;
				b3 = 0;
				pad = 2;
			} else {
				b2 = 0xFF & _bytes[i++];
				if (i >= _bytes.length) {
					b3 = 0;
					pad = 1;
				} else {
					b3 = (0xFF & _bytes[i++]);
				}
			}
			byte c1 = (byte) (b1 >> 2);
			byte c2 = (byte) (((b1 & 0x3) << 4) | (b2 >> 4));
			byte c3 = (byte) (((b2 & 0xf) << 2) | (b3 >> 6));
			byte c4 = (byte) (b3 & 0x3f);
			encodedBuffer.append(BASE64ARRAY[c1]).append(BASE64ARRAY[c2]);
			switch (pad) {
				case 0:
					encodedBuffer.append(BASE64ARRAY[c3]).append(BASE64ARRAY[c4]);
					break;
				case 1:
					encodedBuffer.append(BASE64ARRAY[c3]).append('=');
					break;
				case 2:
					encodedBuffer.append("==");
					break;
			}
		}
		
		return encodedBuffer.toString();
	}
	
	/**
	 * Translates a Base64 value to either its 6-bit reconstruction value or a
	 * negative number indicating some other meaning.
	 */
	protected final static byte[] DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal
																					// 0
																					// -
																					// 8
					-5, -5, // Whitespace: Tab and Linefeed
					-9, -9, // Decimal 11 - 12
					-5, // Whitespace: Carriage Return
					-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal
																		// 14 -
					// 26
					-9, -9, -9, -9, -9, // Decimal 27 - 31
					-5, // Whitespace: Space
					-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
					62, // Plus sign at decimal 43
					-9, -9, -9, // Decimal 44 - 46
					63, // Slash at decimal 47
					52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero
															// through nine
					-9, -9, -9, // Decimal 58 - 60
					-1, // Equals sign at decimal 61
					-9, -9, -9, // Decimal 62 - 64
					0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A'
					// through 'N'
					14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters
																	// 'O'
					// through 'Z'
					-9, -9, -9, -9, -9, -9, // Decimal 91 - 96
					26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters
																		// 'a'
					// through 'm'
					39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters
																		// 'n'
					// through 'z'
					-9, -9, -9, -9 // Decimal 123 - 126
	};
	
	// Indicates white space in encoding
	protected final static byte WHITE_SPACE_ENC = -5;
	
	// Indicates equals sign in encoding
	protected final static byte EQUALS_SIGN_ENC = -1;
	
	/** The equals sign (=) as a byte. */
	protected final static byte EQUALS_SIGN = (byte) '=';
	
	/**
	 * base 64 decoding
	 * 
	 * @param encoded
	 *            string
	 * @param encoding
	 *            used to get string bytes
	 * @return result of encoding, or null if encoding invalid or string null,
	 *         or string is invalid base 64 encoding
	 */
	public final static String base64Decode(String base64Encoded, String encoding) {
		if (IOHelper.isNull(base64Encoded)) {
			return null;
		} else if (!IOHelper.isNullOrEmpty(base64Encoded)) {
			if (IOHelper.isNullOrEmpty(encoding)) {
				encoding = IOHelper.ISO_8859_1;
			}
			try {
				return new String(decode64(base64Encoded), encoding);
			} catch (UnsupportedEncodingException ex) {
				LogManager.error(ex);
			}
		}
		
		return null;
	}
	
	/**
	 * Decodes four bytes from array <var>source</var> and writes the resulting
	 * bytes (up to three of them) to <var>destination</var>. The source and
	 * destination arrays can be manipulated anywhere along their length by
	 * specifying <var>srcOffset</var> and <var>destOffset</var>. This method
	 * does not
	 * check to make sure your arrays are large enough to accomodate
	 * <var>srcOffset</var> + 4 for the <var>source</var> array or
	 * <var>destOffset</var> + 3
	 * for the <var>destination</var> array. This method returns the actual
	 * number of bytes that were converted from the Base64 encoding.
	 * 
	 * 
	 * @param source
	 *            the array to convert
	 * @param srcOffset
	 *            the index where conversion begins
	 * @param destination
	 *            the array to hold the conversion
	 * @param destOffset
	 *            the index where output will be put
	 * @return the number of decoded bytes converted
	 * @since 1.3
	 */
	private static int decode4to3(byte[] source, int srcOffset, byte[] destination, int destOffset) {
		// Example: Dk==
		if (source[srcOffset + 2] == EQUALS_SIGN) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6
			// )
			// | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );
			int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12);
			
			destination[destOffset] = (byte) (outBuff >>> 16);
			return 1;
		}
		
		// Example: DkL=
		else if (source[srcOffset + 3] == EQUALS_SIGN) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6
			// )
			// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
			// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );
			int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12) | ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6);
			
			destination[destOffset] = (byte) (outBuff >>> 16);
			destination[destOffset + 1] = (byte) (outBuff >>> 8);
			return 2;
		}
		
		// Example: DkLE
		else {
			try {
				// Two ways to do the same thing. Don't know which way I like
				// best.
				// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 )
				// >>> 6 )
				// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
				// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )
				// | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );
				int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12) | ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6) | ((DECODABET[source[srcOffset + 3]] & 0xFF));
				
				destination[destOffset] = (byte) (outBuff >> 16);
				destination[destOffset + 1] = (byte) (outBuff >> 8);
				destination[destOffset + 2] = (byte) (outBuff);
				
				return 3;
			} catch (Exception e) {
				System.out.println("" + source[srcOffset] + ": " + (DECODABET[source[srcOffset]]));
				System.out.println("" + source[srcOffset + 1] + ": " + (DECODABET[source[srcOffset + 1]]));
				System.out.println("" + source[srcOffset + 2] + ": " + (DECODABET[source[srcOffset + 2]]));
				System.out.println("" + source[srcOffset + 3] + ": " + (DECODABET[source[srcOffset + 3]]));
				return -1;
			} // e nd catch
		}
	} // end decodeToBytes
	
	/**
	 * Very low-level access to decoding ASCII characters in the form of a byte
	 * array. Does not support automatically gunzipping or any other "fancy"
	 * features.
	 * 
	 * @param source
	 *            The Base64 encoded data
	 * @param off
	 *            The offset of where to begin decoding
	 * @param len
	 *            The length of characters to decode
	 * @return decoded data
	 * @since 1.3
	 */
	public static byte[] decode(byte[] source, int off, int len) {
		int len34 = len * 3 / 4;
		byte[] outBuff = new byte[len34]; // Upper limit on size of output
		int outBuffPosn = 0;
		
		byte[] b4 = new byte[4];
		int b4Posn = 0;
		int i = 0;
		byte sbiCrop = 0;
		byte sbiDecode = 0;
		for (i = off; i < off + len; i++) {
			sbiCrop = (byte) (source[i] & 0x7f); // Only the low seven bits
			sbiDecode = DECODABET[sbiCrop];
			
			if (sbiDecode >= WHITE_SPACE_ENC) // Whitesp ace,Eq ualssi gnor be
			// tter
			{
				if (sbiDecode >= EQUALS_SIGN_ENC) {
					b4[b4Posn++] = sbiCrop;
					if (b4Posn > 3) {
						outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn);
						b4Posn = 0;
						
						// If that was the equals sign, break out of 'for' loop
						if (sbiCrop == EQUALS_SIGN) {
							break;
						}
					} // end if: quartet built
					
				} // end if: equals sign or better
				
			} // end if: white space, equals sign or better
			else {
				System.err.println("Bad Base64 input character at " + i + ": " + source[i] + "(decimal)");
				return null;
			} // end else:
		} // each input character
		
		byte[] out = new byte[outBuffPosn];
		System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
		return out;
	} // end decode
	
	/**
	 * Decodes data from Base64 notation, automatically detecting
	 * gzip-compressed data and decompressing it.
	 * 
	 * @param s
	 *            the string to decode
	 * @return the decoded data
	 * @since 1.4
	 */
	public static byte[] decode64(String string) {
		byte[] bytes;
		try {
			bytes = string.getBytes(IOHelper.ISO_8859_1);
		} catch (java.io.UnsupportedEncodingException uee) {
			bytes = string.getBytes();
		}
		
		// </change>
		
		// Decode
		bytes = decode(bytes, 0, bytes.length);
		
		// Check to see if it's gzip-compressed
		// GZIP Magic Two-Byte Number: 0x8b1f (35615)
		if (bytes != null && bytes.length >= 4) {
			
			int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
			if (java.util.zip.GZIPInputStream.GZIP_MAGIC == head) {
				java.io.ByteArrayInputStream bais = null;
				java.util.zip.GZIPInputStream gzis = null;
				java.io.ByteArrayOutputStream baos = null;
				byte[] buffer = new byte[2048];
				int length = 0;
				
				try {
					baos = new java.io.ByteArrayOutputStream();
					bais = new java.io.ByteArrayInputStream(bytes);
					gzis = new java.util.zip.GZIPInputStream(bais);
					
					while ((length = gzis.read(buffer)) >= 0) {
						baos.write(buffer, 0, length);
					}
					
					// No error? Get new bytes.
					bytes = baos.toByteArray();
					
				} catch (java.io.IOException e) {
					// Just return originally-decoded bytes
				} finally {
					IOHelper.closeSilently(baos);
					IOHelper.closeSilently(gzis);
					IOHelper.closeSilently(bais);
				}
			} // end if: gzipped
		} // end if: bytes.length >= 2
		
		return bytes;
	}
	
	/**
	 * calculate local file based class path for class loader if possible
	 * (servlet classes must be located there)
	 * 
	 * @param cl
	 *            class loader
	 * @return class path in string
	 */
	static public String calculateClassPath(ClassLoader classLoader) {
		// scan cl chain to find
		StringBuffer classPath = new StringBuffer();
		boolean jspFound = false, servletFound = false;
		while (classLoader != null) {
			if (classLoader instanceof URLClassLoader) {
				boolean addClasses = false;
				if (jspFound == false) {
					jspFound = ((URLClassLoader) classLoader).findResource("javax/servlet/jsp/JspPage.class") != null;
					addClasses |= jspFound;
				}
				if (servletFound == false) {
					servletFound = ((URLClassLoader) classLoader).findResource("javax/servlet/http/HttpServlet.class") != null;
					addClasses |= servletFound;
				}
				if (addClasses) {
					URL[] urls = ((URLClassLoader) classLoader).getURLs();
					for (int i = 0; i < urls.length; i++) {
						String classFile = toFile(urls[i]);
						if (classFile == null) {
							continue;
						}
						if (classPath.length() > 0) {
							classPath.append(File.pathSeparatorChar).append(classFile);
						} else {
							classPath.append(classFile);
						}
					}
				}
				
				if (jspFound && servletFound) {
					return classPath.toString();
				}
			}
			classLoader = classLoader.getParent();
		}
		
		return System.getProperty("java.class.path");
	}
	
	/**
	 * 
	 * @param numObject
	 * @param defValue
	 * @return
	 */
	public static int parseInt(Object numObject, int defValue) {
		if (numObject instanceof Number) {
			return ((Number) numObject).intValue();
		}
		try {
			return Integer.parseInt(numObject.toString());
		} catch (Exception ex) {
			return defValue;
		}
	}
	
	public static final String toFile(URL url) {
		if (url.getProtocol().indexOf("file") < 0) {
			return null;
		}
		
		String result = url.getPath();
		if (result.charAt(0) == '/' && File.separatorChar == '\\') {
			result = result.substring(1);
		}
		
		try {
			return decode(result, IOHelper.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			return result;
		}
	}
	
	/**
	 * ThreadPool
	 */
	public static final class ThreadPool {
		static final int DEF_MAX_POOLED_THREAD = 20;
		static final String ID = "Acme.Utils.ThreadPool";
		public static final String MAXNOTHREAD = ID + ".maxpooledthreads";
		
		protected static int counter;
		protected ArrayList<PooledThread> freeThreads;
		protected HashMap<PooledThread, PooledThread> busyThreads;
		protected int maxThreads;
		protected ThreadPoolFactory threadFactory;
		
		/**
		 * Creates a thread pool not queued with max number of threads defined
		 * in properties or DEF_MAX_POOLED_THREAD = 20
		 * 
		 * @param Properties
		 *            where property THREADSINPOOL gives max threads Note if
		 *            THREADSINPOOL not integers, or negative then
		 *            DEF_MAX_POOLED_THREAD used
		 */
		public ThreadPool(Properties properties, ThreadPoolFactory threadfactory) {
			maxThreads = parseInt(properties.getProperty(MAXNOTHREAD), DEF_MAX_POOLED_THREAD);
			if (maxThreads < 0) {
				maxThreads = DEF_MAX_POOLED_THREAD;
			}
			
			freeThreads = new ArrayList<PooledThread>(maxThreads);
			busyThreads = new HashMap<PooledThread, PooledThread>(maxThreads);
			this.threadFactory = threadfactory;
		}
		
		/**
		 * Assigns a new value for max threads
		 * 
		 * @param int
		 *            new value of max threads, can't be less than 2, but can be
		 *            0 If current number threads exceed the value, then extra
		 *            thread will be
		 *            discarded gracefully
		 */
		public void setMaxThreads(int newSize) {
			if (newSize > 2 || newSize == 0) {
				maxThreads = newSize;
			}
		}
		
		/**
		 * Returns setting for max number of threads
		 * 
		 * @return int setting for max number of threads, doesn't reflect actual
		 *         number of threads though
		 */
		public int getMaxThreads() {
			return maxThreads;
		}
		
		/**
		 * Takes a new task for execution by a threads in pool will wait until
		 * free threads if number of threads reached max
		 * 
		 * @param Runnable
		 *            task for execution
		 */
		public void executeThread(Runnable runnable) {
			PooledThread pooledThread = null;
			do {
				synchronized (freeThreads) {
					if (freeThreads.size() > 0) {
						pooledThread = freeThreads.remove(0);
					}
				}
				
				if (pooledThread != null && !pooledThread.isAlive()) {
					pooledThread = null;
				}
				
				if (pooledThread == null) {
					synchronized (busyThreads) {
						if (busyThreads.size() < maxThreads || maxThreads == 0)
							pooledThread = new PooledThread();
					}
				}
				
				if (pooledThread == null) {
					synchronized (freeThreads) {
						try {
							freeThreads.wait();
						} catch (InterruptedException ex) {
							// ignore me!
						}
					}
				}
			} while (pooledThread == null);
			pooledThread.setName("-PooledThread: " + runnable);
			pooledThread.setRunner(runnable);
			synchronized (busyThreads) {
				busyThreads.put(pooledThread, pooledThread);
			}
		}
		
		protected void finalize() throws Throwable {
			synchronized (freeThreads) {
				Iterator<PooledThread> itr = freeThreads.iterator();
				while (itr.hasNext()) {
					itr.next().interrupt();
				}
			}
			
			synchronized (busyThreads) {
				Iterator<PooledThread> itr = freeThreads.iterator();
				while (itr.hasNext()) {
					itr.next().interrupt();
				}
			}
			super.finalize();
		}
		
		/**
		 * Returns the string representation of this object.
		 * 
		 * @return
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			if (freeThreads != null && busyThreads != null) {
				return ID + ": free threads:" + freeThreads.size() + " busy threads:" + busyThreads.size();
			} else {
				return ID + ": not initialized yet. " + super.toString();
			}
		}
		
		final class PooledThread implements Runnable {
			Runnable runner;
			boolean quit;
			Thread delegateThread;
			String id = ID + "(" + (counter++) + ")";
			
			PooledThread() {
				if (threadFactory != null) {
					delegateThread = threadFactory.create(this);
				} else {
					delegateThread = new Thread(this);
				}
				setName("-PooledThread: CREATED");
				delegateThread.start();
			}
			
			public void setName(String name) {
				delegateThread.setName(id + name);
			}
			
			public boolean isAlive() {
				return delegateThread.isAlive();
			}
			
			synchronized public void run() {
				do {
					if (runner == null) {
						try {
							this.wait();
						} catch (InterruptedException ie) {
							// ignore me!
						}
					}
					
					if (runner != null) {
						try {
							runner.run();
						} catch (Throwable ex) {
							if (ex instanceof ThreadDeath) {
								throw (ThreadDeath) ex;
							}
							ex.printStackTrace();
						} finally {
							runner = null;
						}
						
						int activeThreads = 0;
						synchronized (busyThreads) {
							busyThreads.remove(this);
							activeThreads = busyThreads.size();
						}
						
						synchronized (freeThreads) {
							if (freeThreads.size() + activeThreads > maxThreads) {
								// discard this thread
								break;
							}
							freeThreads.add(this);
							delegateThread.setName(ID + "-PooledThread: FREE");
							freeThreads.notify();
						}
					}
				} while (!quit);
			}
			
			synchronized public void interrupt() {
				quit = true;
				delegateThread.interrupt();
			}
			
			/**
			 * 
			 * @param runnable
			 */
			synchronized void setRunner(Runnable runnable) {
				if (runner != null) {
					throw new RuntimeException("Invalid worker thread state, current runner not null.");
				}
				runner = runnable;
				this.notifyAll();
			}
		}
	}
	
	public static class SimpleBuffer {
		byte[] buffer;
		int fillPosition;
		byte[] emptyBuffer;
		
		public SimpleBuffer() {
			fillPosition = 0;
			setSize(COPY_BUF_SIZE);
		}
		
		public synchronized void setSize(int size) {
			if (size < 0) {
				throw new IllegalArgumentException("Size can't be negative");
			}
			
			if (fillPosition <= 0) {
				buffer = new byte[size];
			} else {
				throw new IllegalStateException("Can't resize buffer containing data");
			}
		}
		
		public synchronized int getSize() {
			return buffer.length;
		}
		
		public synchronized byte[] put(byte[] data, int off, int len) {
			// System.err.println("put in buff:" + len+", fp:"+fillPos);
			if (buffer.length > fillPosition + len) {
				System.arraycopy(data, off, buffer, fillPosition, len);
				fillPosition += len;
				return getEmptyBuffer();
			}
			byte[] result = new byte[Math.max(fillPosition + len - buffer.length, buffer.length)];
			// System.err.println("fp:" + fillPos + ",bl:" + buffer.length +
			// ",rl:" + result.length + ",l:" + len);
			// fill result
			int rfilled = 0;
			if (fillPosition < result.length) {
				System.arraycopy(buffer, 0, result, 0, fillPosition);
				rfilled = result.length - fillPosition;
				System.arraycopy(data, off, result, fillPosition, rfilled);
				fillPosition = 0;
				// System.err.println("1rf:"+rfilled);
			} else {
				System.arraycopy(buffer, 0, result, 0, result.length);
				System.arraycopy(buffer, result.length, buffer, 0, fillPosition - result.length);
				fillPosition -= result.length;
				rfilled = 0;
				// System.err.println("qrf: 0");
			}
			
			if (rfilled < len) {
				System.arraycopy(data, off + rfilled, buffer, fillPosition, len - rfilled);
				fillPosition += len - rfilled;
				// System.err.println("added to buf:"+(len - rfilled));
			}
			
			return result;
		}
		
		public synchronized byte[] get() {
			// System.err.println("get fp: "+fillPos);
			if (fillPosition <= 0) {
				return getEmptyBuffer();
			}
			byte[] result = new byte[fillPosition];
			System.arraycopy(buffer, 0, result, 0, fillPosition);
			fillPosition = 0;
			return result;
		}
		
		public synchronized void reset() {
			// System.err.println("reset buf");
			fillPosition = 0;
		}
		
		private synchronized byte[] getEmptyBuffer() {
			if (emptyBuffer == null) {
				emptyBuffer = new byte[0];
			}
			
			return emptyBuffer;
		}
	}
	
	/**
	 * Converts the request headers to map.
	 * 
	 * @param request
	 * @return
	 */
	public static HashMap<String, List<String>> getRequestHeaders(HttpServletRequest request) {
		HashMap<String, List<String>> requestHeaders = new HashMap<String, List<String>>();
		for (Enumeration<String> itr = request.getHeaderNames(); itr.hasMoreElements();) {
			String name = itr.nextElement();
			requestHeaders.put(name, Collections.list(request.getHeaders(name)));
		}
		
		return requestHeaders;
	}
	
	/**
	 * Converts the request parameters as map.
	 * 
	 * @param request
	 * @return
	 */
	public static HashMap<String, List<String>> getRequestParameters(HttpServletRequest request) {
		HashMap<String, List<String>> requestParameters = new HashMap<String, List<String>>();
		for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			requestParameters.put(entry.getKey(), Arrays.asList(entry.getValue()));
		}
		
		return requestParameters;
	}
	
	/**
	 * Converts the request headers to map.
	 * 
	 * @param request
	 * @return
	 */
	public static HashMap<String, List<String>> getResponseHeaders(final HttpServletResponse response) {
		HashMap<String, List<String>> responseHeaders = new HashMap<String, List<String>>();
		if (response.getHeaderNames() instanceof Collections) {
			/* TODO - Implement it later. */
			// for (String name : response.getHeaderNames()) {
			// responseHeaders.put(name, new
			// ArrayList<String>(response.getHeaders(name)));
			// }
		} else {
			for (Enumeration<String> itr = response.getHeaderNames(); itr.hasMoreElements();) {
				String name = itr.nextElement();
				responseHeaders.put(name, Collections.list(response.getHeaders(name)));
			}
		}
		
		return responseHeaders;
	}
	
	/**
	 * Logs the provider information.
	 *
	 * @param provider
	 */
	public static String toString(final Provider provider) {
		return (provider == null ? "" : new StringBuilder("Provider - Info:").append(provider.getInfo()).append(", Name:").append(provider.getName()).append(", Services:").append(provider.getServices()).append(", Version:").append(provider.getVersion()).toString());
	}
	
	/**
	 * Logs the provider information.
	 *
	 * @param sslParameters
	 */
	public static String toString(final SSLParameters sslParameters) {
		return (sslParameters == null ? "" : new StringBuilder("SSLParameters - CipherSuites:").append(IOHelper.toString(sslParameters.getCipherSuites())).append(", Protocols:").append(IOHelper.toString(sslParameters.getProtocols())).append(", WantClientAuth:").append(sslParameters.getWantClientAuth()).append(", NeedClientAuth:").append(sslParameters.getNeedClientAuth()).toString());
	}
	
	public static void main(String[] args) {
		try {
			System.out.println(args[0]);
			System.out.println(canonicalizePath(args[0]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
