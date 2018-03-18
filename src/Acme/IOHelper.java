/**
 * 
 */
package Acme;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import com.devamatre.logger.LogManager;
import com.devamatre.logger.LogUtility;
import com.devamatre.logger.Logger;

import rogatkin.web.WebApp;

/**
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 04:13:46 PM
 */
public final class IOHelper {
	/** logger */
	private static Logger logger = LogManager.getLogger(IOHelper.class);
	
	/** UTF-8 */
	public static String UTF_8 = "UTF-8";
	/** ISO-8859-1 */
	public static String ISO_8859_1 = "ISO-8859-1";
	
	/** HTTP - Constants. */
	public static String EXPIRES = "Expires";
	public static String PRAGMA = "Pragma";
	public static String PRAGMA_PUBLIC = "public";
	public static String CACHE_CONTROL = "Cache-Control";
	public static String USER_AGENT = "User-Agent";
	public static String NO_CACHE = "no-cache";
	
	// Content-Type
	public static String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
	public static String CONTENT_TYPE_JSON = "application/json";
	public static String CONTENT_TYPE_ICON = "image/x-icon";
	
	private IOHelper() {
		logger.info("IOHelper()");
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
		if (LogUtility.isNullOrEmpty(parentFolder)) {
			return fileName;
		} else if (LogUtility.isNullOrEmpty(fileName)) {
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
	 * Closes the given <code>mCloseable</code>.
	 *
	 * @param mCloseable
	 */
	public static final void safeClose(Object mCloseable, boolean nulify) {
		try {
			if (mCloseable != null) {
				if (mCloseable instanceof Closeable) {
					((Closeable) mCloseable).close();
				} else if (mCloseable instanceof Socket) {
					((Socket) mCloseable).close();
				} else if (mCloseable instanceof ServerSocket) {
					((ServerSocket) mCloseable).close();
				} else {
					throw new IllegalArgumentException("mCloseable is not an instance of closeable object!");
				}
			}
		} catch (IOException ex) {
			logger.error(ex);
		} finally {
			if (nulify) {
				mCloseable = null;
			}
		}
	}
	
	/**
	 * 
	 * @param mCloseable
	 */
	public static final void safeClose(Object mCloseable) {
		safeClose(mCloseable, false);
	}
	
	/**
	 * Returns the bytes of the specified input stream.
	 *
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public static byte[] readBytes(final InputStream inputStream, final boolean closeStream) throws IOException {
		logger.debug("+readBytes(inputStream, " + closeStream + ")");
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
				logger.error(ex);
				throw ex;
			} finally {
				/* close streams. */
				safeClose(outputStream);
				if (closeStream) {
					safeClose(bInputStream);
				}
			}
		}
		
		logger.debug("-readBytes(), resultBytes:" + resultBytes);
		return resultBytes;
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
				logger.error(ex);
				throw ex;
			} finally {
				/* close streams. */
				if (closeStream) {
					safeClose(outputStream);
				}
			}
		}
		
		return result;
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
		if (LogUtility.isNotNull(bytes)) {
			try {
				if (LogUtility.isNullOrEmpty(charsetName)) {
					bytesAsString = new String(bytes);
				} else {
					bytesAsString = new String(bytes, charsetName);
				}
			} catch (Exception ex) {
				logger.error(ex);
				bytesAsString = (LogUtility.isNull(bytes) ? null : bytes.toString());
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
		if (replaceNonDigitCharacters && LogUtility.isNullOrEmpty(utf8String)) {
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
		if (!LogUtility.isNullOrEmpty(string)) {
			try {
				stringAsBytes = LogUtility.isNullOrEmpty(charsetName) ? string.getBytes() : string.getBytes(charsetName);
			} catch (Exception ex) {
				logger.error(ex);
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
			logger.error(ex);
		}
	}
	
	/**
	 * Returns the "favicon.ico" icon image 16X16 pixels in size.
	 * 
	 * @return
	 */
	public static byte[] readFavIconBytes() {
		try {
			final String iconPath = IOHelper.pathString(IOHelper.pathString(WebApp.class), "../resource/tjws.gif");
			// return
			// readBytes(IOHelper.class.getResourceAsStream("Resource/favicon.ico"),
			// true);
			return readBytes(new FileInputStream(iconPath), true);
		} catch (IOException ex) {
			logger.error(ex);
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
	
}
