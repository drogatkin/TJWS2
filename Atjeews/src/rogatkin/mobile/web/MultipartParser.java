package rogatkin.mobile.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class MultipartParser {
	public static final String MULTIPARTDATA = "multipart/form-data";
	
	public static final String BOUNDARY_EQ = "boundary=";
	
	public static final String BOUNDARY_END_SFX = "--";
	
	public static final String CONTENT_DISP = "Content-Disposition";
	
	public static final String FORM_DATA = "form-data";
	
	public static final String FILENAME_EQ_QT = "filename=\"";
	
	public static final String NAME_EQ_QT = "name=\"";
	
	public static final String FILENAME = "filename";
	
	public static final String CONTENT_TYPE = "Content-Type";
	
	protected HashMap<String, Object[]> multipartData;
	
	public MultipartParser(ServletRequest req, ServletResponse response) throws IOException {
		String contentType = req.getContentType();
		ServletInputStream sis = null;
		try {
			sis = req.getInputStream();
		} catch (IllegalStateException ise) {
			throw new IOException("Input stream is unaccessible");
		}
		int bp = contentType.indexOf(BOUNDARY_EQ);
		if (bp < 0) {
			return;
		}
		
		String boundary = contentType.substring(bp + BOUNDARY_EQ.length()); // it
		// can be not last attribute
		int boundaryLength = boundary.length();
		// TODO can be -1, it is normal
		int contentLength = req.getContentLength();
		if (contentLength <= 0) {
			return;
		}
		
		int maxReqLength = 30 * 1024 * 1024;
		if (contentLength > maxReqLength) {
			sis.skip(contentLength);
			return;
		}
		multipartData = new HashMap<String, Object[]>();
		// TODO: do not allocate buffer for all content length, just keep
		// reading
		byte[] buffer = new byte[contentLength];
		int contentRead = 0;
		main_loop:
		do {
			if (contentRead > contentLength) {
				break main_loop;
			}
			
			// read --------------boundary
			int ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
			if (ec < 0) {
				break main_loop;
			}
			
			String s = new String(buffer, contentRead, ec, "UTF-8");
			contentRead += ec;
			int p = s.indexOf(boundary);
			if (p >= 0) {
				if (s.regionMatches(p + boundaryLength, BOUNDARY_END_SFX, 0, BOUNDARY_END_SFX.length())) {
					// it shouldn't happen here, but it's Ok
					break;
				}
				// skip the boundary, if it happens, because it's first
				ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
				s = new String(buffer, contentRead, ec, "UTF-8");
				contentRead += ec;
			}
			
			// s contains here first line of a part
			int dp, ep;
			String header, name = null, filename = null, token, partContentType = null;
			do {
				dp = s.indexOf(':');
				if (dp < 0) { // throw new IOException( ..
					break main_loop;
				}
				
				header = s.substring(0, dp);
				s = s.substring(dp + 2);
				if (CONTENT_DISP.equalsIgnoreCase(header)) {
					StringTokenizer ast = new StringTokenizer(s, ";");
					if (ast.hasMoreTokens()) {
						token = ast.nextToken();
						if (token.indexOf(FORM_DATA) < 0) {
							break main_loop; // throw new IOException( ..
						}
						
						while (ast.hasMoreTokens()) {
							token = ast.nextToken();
							dp = token.indexOf(FILENAME_EQ_QT);
							if (dp >= 0) {
								ep = token.indexOf('"', dp + FILENAME_EQ_QT.length());
								if (ep < 0 || filename != null) {
									break main_loop;
								}
								filename = token.substring(dp + FILENAME_EQ_QT.length(), ep);
								continue;
							}
							dp = token.indexOf(NAME_EQ_QT);
							if (dp >= 0) {
								ep = token.indexOf('"', dp + NAME_EQ_QT.length());
								if (ep < 0 || ep == dp + NAME_EQ_QT.length() || name != null) {
									break main_loop; // throw new
								}
								// IOException( ..
								name = token.substring(dp + NAME_EQ_QT.length(), ep);
								continue;
							}
						}
					}
					if (filename != null)
						addPart(name + '+' + FILENAME, filename);
				} else if (CONTENT_TYPE.equalsIgnoreCase(header)) {
					partContentType = s;
				}
				ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
				if (ec < 0)
					break main_loop; // throw new IOException( ..
				if (ec == 2 && buffer[contentRead] == 0x0D && buffer[contentRead + 1] == 0x0A) {
					contentRead += ec;
					break; // empty line read, skip it
				}
				s = new String(buffer, contentRead, ec, "UTF-8");
			} while (true);
			if (name == null)
				break main_loop; // throw new IOException( ..
			int marker = contentRead;
			if (partContentType == null || partContentType.indexOf("text/") >= 0 || partContentType.indexOf("application/") >= 0 || partContentType.indexOf("message/") >= 0 || partContentType.indexOf("unknown") >= 0) { // read
				// everything
				do {
					ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
					if (ec < 0)
						break main_loop;
					s = new String(buffer, contentRead, ec, "UTF-8");
					p = s.indexOf(boundary);
					if (p >= 0) { // we met a boundry
						// finish current part
						if (contentRead - marker <= 2) {
							// no file content in the stream, probably it's a
							// remote file
							try {
								URLConnection uc = new URL(filename).openConnection();
								if (uc.getContentType().indexOf("image/") >= 0) { // support
									int cl = uc.getContentLength();
									if (cl > 0 && cl < maxReqLength) {
										InputStream uis = uc.getInputStream();
										if (uis != null) {
											byte[] im = new byte[cl];
											cl = 0;
											int rc;
											do {
												rc = uis.read(im, cl, im.length - cl);
												if (rc < 0)
													break;
												cl += rc;
											} while (rc > 0);
											uis.close();
											addPart(name, im);
										}
									} else { // length unknown but we can try
										InputStream uis = uc.getInputStream();
										if (uis != null) {
											byte[] buf = new byte[2048];
											byte[] im = new byte[0];
											try {
												do {
													cl = uis.read(buf);
													if (cl < 0)
														break;
													byte[] wa = new byte[im.length + cl];
													System.arraycopy(im, 0, wa, 0, im.length);
													System.arraycopy(buf, 0, wa, im.length, cl);
													im = wa;
												} while (true);
											} finally {
												uis.close();
											}
											addPart(name, im);
										}
									}
								}
							} catch (MalformedURLException mfe) {
							}
						} else {
							if (partContentType != null && partContentType.indexOf("application/") >= 0) {
								byte[] im = new byte[contentRead - marker - 2];
								System.arraycopy(buffer, marker, im, 0, contentRead - marker - 2/* crlf */);
								addPart(name, im);
							} else {
								addPart(name, new String(buffer, marker, contentRead - marker - 2/* crlf */, "UTF-8"));
							}
						}
						if (s.regionMatches(p + boundaryLength, BOUNDARY_END_SFX, 0, BOUNDARY_END_SFX.length()))
							break main_loop; // it shouldn't happen here, but
						// it's Ok
						contentRead += ec;
						break;
					}
					contentRead += ec;
				} while (true);
			} else if (partContentType.indexOf("image/") >= 0 || partContentType.indexOf("audio/") >= 0) {
				do {
					ec = sis.readLine(buffer, contentRead, contentLength - contentRead);
					if (ec < 0)
						throw new IOException("Premature ending of input stream");
					
					s = new String(buffer, contentRead, ec, "UTF-8");
					p = s.indexOf(boundary);
					if (p >= 0) { // we met a bounder
						byte[] im = new byte[contentRead - marker - 2];
						System.arraycopy(buffer, marker, im, 0, contentRead - marker - 2);
						addPart(name, im);
						if (s.regionMatches(p + boundaryLength, BOUNDARY_END_SFX, 0, BOUNDARY_END_SFX.length()))
							break main_loop; // it shouldn't happen here, but
						// it's Ok
						contentRead += ec;
						break;
					}
					contentRead += ec;
				} while (true);
			} else {
				throw new IOException("Unsupported content type '" + partContentType + '\'');
			}
		} while (true);
	}
	
	protected void addPart(String name, Object data) {
		Object[] curData = multipartData.get(name);
		if (curData == null) {
			curData = new Object[1];
			curData[0] = data;
		} else {
			Object[] newdata = new Object[curData.length + 1];
			System.arraycopy(curData, 0, newdata, 0, curData.length);
			newdata[curData.length] = data;
			curData = newdata;
		}
		multipartData.put(name, curData);
	}
	
	public Object getParameter(String name) {
		if (multipartData == null)
			return null;
		Object[] curData = multipartData.get(name);
		if (curData == null)
			return null;
		return curData[0];
		
	}
}
