/**
 * 
 */
package tjws.embedded;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.devamatre.logger.LogManager;
import com.devamatre.logger.Logger;

import Acme.IOHelper;

/**
 * The <code>EmbeddedServlet</code> handles all local requests.
 * 
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 03:39:08 PM
 */
public class EmbeddedServlet extends HttpServlet {
	
	/** serialVersionUID */
	private static final long serialVersionUID = 1L;
	
	/** logger */
	private static Logger logger = LogManager.getLogger(EmbeddedServlet.class);
	
	public EmbeddedServlet() {
		logger.info("EmbeddedServlet()");
	}
	
	/**
	 * @param servletConfig
	 * @throws ServletException
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		logger.debug("init(" + servletConfig + ")");
	}
	
	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      *
	 *      javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}
	
	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      *
	 *      javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}
	
	/**
	 * Processes all requests.
	 *
	 * @param servletRequest
	 * @param servletResponse
	 * @throws ServletException
	 * @throws IOException
	 */
	private void process(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
		logger.debug("process(" + servletRequest + "," + servletResponse + ")");
		try {
			String pathSegment = servletRequest.getRequestURI();
			logger.debug("pathSegment:" + pathSegment);
			if (pathSegment.endsWith("/") || pathSegment.endsWith("html")) {
				byte[] dataBytes = IOHelper.readBytes(EmbeddedServlet.class.getResourceAsStream("web/index.html"), true);
				logger.debug("dataBytes:\n" + IOHelper.toUTF8String(dataBytes) + "\n");
				IOHelper.sendResponse(IOHelper.CONTENT_TYPE_HTML, dataBytes, servletResponse);
			} else if (pathSegment.endsWith("favicon.ico")) {
				IOHelper.sendResponse(IOHelper.CONTENT_TYPE_ICON, IOHelper.readFavIconBytes(), servletResponse);
			} else if (pathSegment.endsWith(".js")) {
				if (pathSegment.startsWith("/")) {
					pathSegment = pathSegment.substring(1);
				}
				byte[] dataBytes = IOHelper.readBytes(EmbeddedServlet.class.getResourceAsStream(pathSegment), true);
				IOHelper.sendResponse(IOHelper.CONTENT_TYPE_JSON, dataBytes, servletResponse);
			} else {
				IOHelper.sendResponse(IOHelper.CONTENT_TYPE_HTML, "Invalid Request".getBytes(), servletResponse);
			}
		} catch (Exception ex) {
			logger.error(ex);
		}
	}
}
