package tjws.test;

import java.io.IOException;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Set;

import javax.servlet.annotation.WebServlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.HttpConstraint;

@WebServlet(name = "AnnotatedServlet", description = "A sample annotated servlet", urlPatterns = {
		"/TestServlet" }, initParams = { @WebInitParam(name = "foo", value = "Hello "),
				@WebInitParam(name = "bar", value = " World!") })
@ServletSecurity()
public class SelfDeployServlet extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		PrintWriter writer = response.getWriter();
		writer.println("<html>Hello, I am a Java servlet!</html>");
		writer.flush();
		System.out.println("Hello from a servlet");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String paramWidth = request.getParameter("width");
		int width = Integer.parseInt(paramWidth);

		String paramHeight = request.getParameter("height");
		int height = Integer.parseInt(paramHeight);

		long area = width * height;

		PrintWriter writer = response.getWriter();
		writer.println("<html>Area of the rectangle is: " + area + "</html>");
		writer.flush();

	}

	@WebFilter(urlPatterns = { "/*" }, initParams = {
			@WebInitParam(name = "test-param", value = "Initialization Paramter") })
	public static class LogFilter implements Filter {
		@Override
		public void init(FilterConfig config) throws ServletException {
			// Get init parameter
			String testParam = config.getInitParameter("test-param");

			// Print the init parameter
			System.out.println("Test Param: " + testParam);
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

			// Log the current timestamp.
			System.out.println("Time " + new Date().toString());

			// Pass request back down the filter chain
			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {
			/*
			 * Called before the Filter instance is removed from service by the web
			 * container
			 */
			System.out.println("Filter destroied");
		}
	}

	@WebListener
	public static class ContextListener implements ServletContextListener {

		@Override
		public void contextInitialized(ServletContextEvent event) {
			System.out.println("The application started");
		}

		@Override
		public void contextDestroyed(ServletContextEvent event) {
			System.out.println("The application stopped");
		}
	}
	
	@HandlesTypes({
	    javax.servlet.http.HttpServlet.class,
	    javax.servlet.Filter.class
	})
	public static class AppInitializer implements ServletContainerInitializer {
	 
	    @Override
	    public void onStartup(Set<Class<?>> classes, ServletContext context)
	            throws ServletException {
	    	System.out.println("Classes "+classes+" getting initialized.");
	    }
	}
}