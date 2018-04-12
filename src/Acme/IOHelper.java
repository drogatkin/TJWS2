// Copyright (C)2018 by Rohtash Singh Lakra <rohtash.singh@gmail.com>.
// All rights reserved.
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
// Visit the https://github.com/rslakra/TJWS2 page for up-to-date versions of
// this and other fine Java utilities.
//
// All enhancements Copyright (C)2018 by Rohtash Singh Lakra
// This version is compatible with JSDK 2.5
// https://github.com/rslakra/TJWS2
package Acme;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.rslakra.logger.LogManager;

import rogatkin.web.WebApp;

/**
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 04:13:46 PM
 */
public final class IOHelper {
	/** UTF-8 */
	public static String UTF_8 = "UTF-8";
	/** ISO-8859-1 */
	public static String ISO_8859_1 = "ISO-8859-1";
	
	/** JVM-Type - JVM_ANDROID */
	public static String JVM_ANDROID = "Dalvik".intern();
	
	/** HTTP - Constants. */
	public static String EXPIRES = "Expires";
	public static String PRAGMA = "Pragma";
	public static String PRAGMA_PUBLIC = "public";
	public static String CACHE_CONTROL = "Cache-Control";
	public static String USER_AGENT = "User-Agent";
	public static String NO_CACHE = "no-cache";
	
	/** Content-Type */
	public static String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
	public static String CONTENT_TYPE_JSON = "application/json";
	public static String CONTENT_TYPE_ICON = "image/x-icon";
	
	/** Content-Type */
	private static final String LINE_SEPARATOR = System.getProperty("line.separator").intern();
	
	/** singleton instance. */
	private IOHelper() {
		throw new RuntimeException("Object creation is not allowed for this object!");
	}
	
	/**
	 * Returns the system line separator.
	 * 
	 * @return
	 */
	public static final String getLineSeparator() {
		return LINE_SEPARATOR;
	}
	
	/**
	 * Installs the bouncy castle provider.
	 */
	public static void addBouncyCastleProvider() {
		try {
			/* add bouncy castle provider. */
			Security.addProvider(new BouncyCastleProvider());
			LogManager.debug("Added BouncyCastleProvider!");
		} catch (Exception ex) {
			LogManager.error(ex);
		}
	}
	
	/**
	 * Returns true if the object is null otherwise false.
	 * 
	 * @param string
	 * @return
	 */
	public static boolean isNull(Object object) {
		return (object == null);
	}
	
	/**
	 * Returns true if the object is not null otherwise false.
	 * 
	 * @param object
	 * @return
	 */
	public static boolean isNotNull(Object object) {
		return (!isNull(object));
	}
	
	/**
	 * Returns true if either the string is null or length is 0(zero) otherwise
	 * false.
	 * 
	 * @param string
	 * @return
	 */
	public static boolean isNullOrEmpty(CharSequence string) {
		return (isNull(string) || string.length() == 0);
	}
	
	/**
	 * Returns true if the JVM is android (dalvik) otherwise false.
	 * 
	 * @param
	 * @return
	 */
	public static boolean isAndroid() {
		return (System.getProperty("java.vm.name").startsWith(JVM_ANDROID));
	}
	
	/**
	 * Returns the path string for the given class.
	 * 
	 * @param className
	 * @return
	 */
	public static String pathString(Class<?> className, boolean pathOnly) {
		String urlString = null;
		URL url = className.getResource(className.getSimpleName() + ".class");
		if (url != null) {
			urlString = url.toExternalForm();
			/*
			 * The <code>urlString</code> most likely ends with a /, then the
			 * full
			 * class name with . replaced with /, and .class. Cut that part off
			 * if
			 * present. If not also check
			 * for backslashes instead. If that's also not present just return
			 * null
			 */
			if (pathOnly) {
				int fileIndex = urlString.indexOf(className.getSimpleName());
				urlString = urlString.substring(0, fileIndex);
			}
			
			/*
			 * <code>urlString</code> is now the URL of the location, but
			 * possibly
			 * with jar: in front and a trailing !
			 */
			if (urlString.startsWith("jar:") && urlString.endsWith("!")) {
				urlString = urlString.substring(4, urlString.length() - 1);
			}
			
			/*
			 * <code>urlString</code> is now the URL of the location, but
			 * possibly
			 * with file: in front.
			 */
			else if (urlString.startsWith("file:")) {
				urlString = urlString.substring("file:".length(), urlString.length());
			}
		}
		
		return urlString;
	}
	
	/**
	 * Returns the path string for the given class.
	 * 
	 * @param className
	 * @return
	 */
	public static String pathString(Class<?> className) {
		return pathString(className, true);
	}
	
	/**
	 * 
	 * @param parentFolder
	 * @param fileName
	 * @return
	 */
	public static String pathString(final String parentFolder, final String fileName) {
		if (IOHelper.isNullOrEmpty(parentFolder)) {
			return fileName;
		} else if (IOHelper.isNullOrEmpty(fileName)) {
			return parentFolder;
		} else if (parentFolder.endsWith(File.separator) || fileName.startsWith(File.separator)) {
			return parentFolder + fileName;
		} else {
			return parentFolder + File.separator + fileName;
		}
	}
	
	/**
	 * Sets the default headers to the specified response.
	 *
	 * @param servletResponse
	 */
	public static void setDefaultHeaders(HttpServletResponse servletResponse) {
		if (servletResponse != null) {
			servletResponse.setDateHeader(EXPIRES, -1);
			servletResponse.setHeader(PRAGMA, PRAGMA_PUBLIC);
			servletResponse.setHeader(CACHE_CONTROL, NO_CACHE);
		}
	}
	
	/**
	 * Closes the specified <code>mCloseables</code> objects.
	 *
	 * @param mCloseables
	 */
	public static final void closeSilently(Object... mCloseables) {
		if (isNotNull(mCloseables)) {
			for (Object mCloseable : mCloseables) {
				try {
					if (mCloseable instanceof Closeable) {
						((Closeable) mCloseable).close();
					} else if (mCloseable instanceof Socket) {
						((Socket) mCloseable).close();
					} else if (mCloseable instanceof ServerSocket) {
						((ServerSocket) mCloseable).close();
					}
				} catch (IOException ex) {
					LogManager.error(ex);
				}
			}
		}
	}
	
	/**
	 * Returns the bytes of the specified input stream.
	 *
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public static byte[] readBytes(final InputStream inputStream, final boolean closeStream) throws IOException {
		LogManager.debug("+readBytes(inputStream, " + closeStream + ")");
		byte[] resultBytes = null;
		if (inputStream != null) {
			ByteArrayOutputStream outputStream = null;
			BufferedInputStream bInputStream = new BufferedInputStream(inputStream);
			try {
				bInputStream = new BufferedInputStream(inputStream);
				outputStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[bInputStream.available()];
				int length = 0;
				while ((length = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, length);
				}
				
				outputStream.flush();
				resultBytes = outputStream.toByteArray();
			} catch (IOException ex) {
				LogManager.error(ex);
				throw ex;
			} finally {
				/* close streams. */
				closeSilently(outputStream);
				if (closeStream) {
					closeSilently(bInputStream);
				}
			}
		}
		
		LogManager.debug("-readBytes(), resultBytes:" + resultBytes);
		return resultBytes;
	}
	
	/**
	 * Returns the bytes of the specified pathString.
	 * 
	 * @param pathString
	 * @param closeStream
	 * @return
	 * @throws IOException
	 */
	public static byte[] readBytes(final String pathString, final boolean closeStream) throws IOException {
		return readBytes(new FileInputStream(pathString), closeStream);
	}
	
	/**
	 * Writes the <code>bytes</code> to <code>outputStream</code> and closes it.
	 *
	 * @param dataBytes
	 * @param outputStream
	 * @param closeStream
	 * @throws IOException
	 */
	public static boolean writeBytes(byte[] dataBytes, OutputStream outputStream, boolean closeStream) throws IOException {
		boolean result = false;
		if (dataBytes != null && outputStream != null) {
			try {
				outputStream.write(dataBytes);
				/* flush output streams. */
				outputStream.flush();
				result = true;
			} catch (IOException ex) {
				LogManager.error(ex);
				throw ex;
			} finally {
				/* close streams. */
				if (closeStream) {
					closeSilently(outputStream);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Copies the contents of an <code>sourceStream</code> into an
	 * <code>targetStream</code>.
	 *
	 * @param sourceStream
	 * @param targetStream
	 * @param closeStreams
	 * @return
	 * @throws IOException
	 */
	public static int copyStream(final InputStream sourceStream, final OutputStream targetStream, boolean closeStreams) throws IOException {
		LogManager.debug("+copyStream(" + sourceStream + ", " + targetStream + ", " + closeStreams + ")");
		int fileSize = 0;
		if (sourceStream != null && targetStream != null) {
			try {
				// buffer
				byte[] buffer = new byte[sourceStream.available()];
				int byteCount = 0;
				while ((byteCount = sourceStream.read(buffer)) != -1) {
					targetStream.write(buffer, 0, byteCount);
					fileSize += byteCount;
				}
				
				/* flush output streams. */
				targetStream.flush();
			} catch (IOException ex) {
				LogManager.error(ex);
				throw ex;
			} finally {
				/* close streams. */
				if (closeStreams) {
					closeSilently(sourceStream, targetStream);
				}
			}
		}
		
		LogManager.debug("-copyStream(), fileSize:" + fileSize);
		return fileSize;
	}
	
	/**
	 * Converts the specified <code>bytes</code> to the specified
	 * <code>charsetName</code> String.
	 * 
	 * @param bytes
	 * @param charsetName
	 * @return
	 */
	public static String bytesAsString(byte[] bytes, String charsetName) {
		String bytesAsString = null;
		if (!IOHelper.isNull(bytes)) {
			try {
				if (IOHelper.isNullOrEmpty(charsetName)) {
					bytesAsString = new String(bytes);
				} else {
					bytesAsString = new String(bytes, charsetName);
				}
			} catch (Exception ex) {
				LogManager.error(ex);
				bytesAsString = (IOHelper.isNull(bytes) ? null : bytes.toString());
			}
		}
		
		return bytesAsString;
	}
	
	/**
	 * Returns the string representation of the given bytes.
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesAsString(byte[] bytes) {
		return bytesAsString(bytes, null);
	}
	
	/**
	 * /**
	 * Returns the UTF-8 String representation of the given <code>bytes</code>.
	 * 
	 * @param bytes
	 * @param replaceNonDigitCharacters
	 * @return
	 */
	public static String toUTF8String(byte[] bytes, boolean replaceNonDigitCharacters) {
		String utf8String = bytesAsString(bytes, UTF_8);
		if (replaceNonDigitCharacters && IOHelper.isNullOrEmpty(utf8String)) {
			utf8String = utf8String.replaceAll("\\D+", "");
		}
		
		return utf8String;
	}
	
	/**
	 * Returns the UTF-8 String representation of the given <code>bytes</code>.
	 * 
	 * @param bytes
	 * @return
	 */
	public static String toUTF8String(byte[] bytes) {
		return toUTF8String(bytes, false);
	}
	
	/**
	 * Returns the UTF-8 String representation of the given <code>string</code>.
	 * 
	 * @param string
	 * @return
	 */
	public static String toUTF8String(String string) {
		return toUTF8String(string.getBytes());
	}
	
	/**
	 * Returns the ISO-8859-1 String representation of the given
	 * <code>bytes</code>.
	 * 
	 * @param bytes
	 * @return
	 */
	public static String toISOString(byte[] bytes) {
		return bytesAsString(bytes, ISO_8859_1);
	}
	
	/**
	 * Converts the specified <code>string</code> into bytes using the specified
	 * <code>charsetName</code>.
	 * 
	 * @param string
	 * @param charsetName
	 * @return
	 */
	public static byte[] toBytes(String string, String charsetName) {
		byte[] stringAsBytes = null;
		if (!IOHelper.isNullOrEmpty(string)) {
			try {
				stringAsBytes = IOHelper.isNullOrEmpty(charsetName) ? string.getBytes() : string.getBytes(charsetName);
			} catch (Exception ex) {
				LogManager.error(ex);
			}
		}
		
		return stringAsBytes;
	}
	
	/**
	 * Converts the specified <code>string</code> into bytes.
	 * 
	 * @param string
	 * @return
	 */
	public static byte[] toBytes(String string) {
		return toBytes(string, null);
	}
	
	/**
	 * Converts the specified <code>string</code> into UTF-8 bytes.
	 * 
	 * @param string
	 * @return
	 */
	public static byte[] toUTF8Bytes(String string) {
		return toBytes(string, UTF_8);
	}
	
	/**
	 * Returns the bytes of the specified input stream.
	 *
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public static InputStream toInputStream(final byte[] dataBytes) {
		return new ByteArrayInputStream(dataBytes);
	}
	
	/**
	 * Sends the given responseBytes as response.
	 * 
	 * @param contentType
	 * @param responseBytes
	 * @param servletResponse
	 * @throws IOException
	 */
	public static void sendResponse(final String contentType, final byte[] responseBytes, final HttpServletResponse servletResponse) {
		try {
			servletResponse.setContentType(contentType);
			setDefaultHeaders(servletResponse);
			writeBytes(responseBytes, servletResponse.getOutputStream(), true);
		} catch (IOException ex) {
			LogManager.error(ex);
		}
	}
	
	/**
	 * Returns the "favicon.ico" icon image 16X16 pixels in size.
	 * 
	 * @return
	 */
	public static byte[] readIconBytes(final InputStream inputStream) {
		try {
			return readBytes(inputStream, true);
		} catch (IOException ex) {
			LogManager.error(ex);
			return null;
		}
	}
	
	/**
	 * Returns the "favicon.ico" icon image 16X16 pixels in size.
	 * 
	 * @return
	 */
	public static byte[] readIconBytes() {
		try {
			final String iconPath = IOHelper.pathString(IOHelper.pathString(WebApp.class), "../resource/tjws.gif");
			return readIconBytes(new FileInputStream(iconPath));
		} catch (IOException ex) {
			LogManager.error(ex);
			return null;
		}
	}
	
	/**
	 * Returns the user's directory.
	 * 
	 * @return
	 */
	public static String getUserDir() {
		return System.getProperty("user.dir");
	}
	
	/**
	 * Returns the user's home directory.
	 * 
	 * @return
	 */
	public static String getUserHome() {
		return System.getProperty("user.home");
	}
	
	/**
	 * Returns the java home folder.
	 * 
	 * @return
	 */
	public static String getJavaHome() {
		return System.getProperty("java.home");
	}
	
	/**
	 * Returns the logs directory under the project.
	 * 
	 * @return
	 */
	public static String getLogsDir() {
		return getUserDir() + File.separator + "logs";
	}
	
	/**
	 * Returns the java's temp folder.
	 * 
	 * @return
	 */
	public static String getTempDir() {
		return System.getProperty("java.io.tmpdir");
	}
	
	/**
	 * Returns the string representation of the <code>throwable</code> object.
	 * 
	 * @param throwable
	 * @return
	 */
	public static final String toString(final Throwable throwable) {
		if (throwable != null) {
			final StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			throwable.printStackTrace(printWriter);
			closeSilently(printWriter);
			return stringWriter.toString();
		}
		
		return null;
	}
	
	/**
	 * Returns the string representation of the given objects.
	 * 
	 * @param objects
	 * @param addNewLine
	 * @return
	 */
	public static final String toString(final Object[] objects, final boolean addNewLine) {
		final StringBuffer strBuilder = new StringBuffer();
		if (isNotNull(objects)) {
			if (addNewLine) {
				strBuilder.append("\n");
			}
			
			for (int i = 0; i < objects.length; i++) {
				if (isNotNull(objects[i])) {
					if (objects[i] instanceof String) {
						strBuilder.append((String) objects[i]);
					} else {
						strBuilder.append(objects[i].toString());
					}
				}
				
				// append new line character.
				if (i < objects.length - 1) {
					if (addNewLine) {
						strBuilder.append("\n");
					} else {
						strBuilder.append(" ");
					}
				}
			}
		}
		
		return strBuilder.toString();
	}
	
	/**
	 * Returns the string representation of the given objects.
	 * 
	 * @param objects
	 * @return
	 */
	public static final String toString(final Object[] objects) {
		return toString(objects, false);
	}
	
	/**
	 * Returns the boolean value of the given object.
	 * 
	 * @param object
	 * @return
	 */
	public static final boolean parseBoolean(final Object object) {
		return (isNull(object) ? false : Boolean.valueOf(object.toString()));
	}
	
	/**
	 * Returns true if the key store is supported otherwise false.
	 *
	 * @param keyStoreStream
	 * @param keyStorePassword
	 * @return
	 */
	public static boolean isKeyStoreSupported(final InputStream keyStoreStream, final String keyStorePassword) {
		final String trustStoreType = KeyStore.getDefaultType();
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance(trustStoreType);
			try {
				keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
			} catch (NoSuchAlgorithmException ex) {
				LogManager.error(ex);
				return false;
			} catch (CertificateException ex) {
				LogManager.error(ex);
				return false;
			} catch (IOException ex) {
				LogManager.error(ex);
				return false;
			}
		} catch (KeyStoreException ex) {
			LogManager.error(ex);
			return false;
		} catch (Exception ex) {
			LogManager.error(ex);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns the new IOException.
	 * 
	 * @param throwable
	 * @return
	 */
	public static IOException newIOException(final Throwable throwable) {
		return new IOException(throwable.toString(), throwable);
	}
	
}
