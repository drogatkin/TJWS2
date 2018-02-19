/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jasper.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.jasper.JasperException;

/**
 * Main JSP compiler class. This class uses 7Bee for compiling.
 * 
 * @author Dmitriy Rogatkin
 */
public class BeeCompiler extends Compiler {

	protected static Object javacLock = new Object();

	private Class javaCompiler;

	static {
		System.setErr(new SystemLogHandler(System.err));
	}

	/**
	 * Compile the servlet from .java file to .class file
	 */
	protected void generateClass(String[] smap) throws FileNotFoundException, JasperException, Exception {

		long t1 = 0;
		if (log.isDebugEnabled())
			t1 = System.currentTimeMillis();

		List<String> parameters = new ArrayList<String>(20);
		parameters.add("-encoding");
		parameters.add(ctxt.getOptions().getJavaEncoding());

		String javaFileName = new File(ctxt.getServletJavaFileName()).getPath();
		String classpath = ctxt.getClassPath();

		String sep = File.pathSeparator; // System.getProperty("path.separator");

		StringBuffer errorReport = new StringBuffer();

		StringBuffer info = new StringBuffer();
		info.append("Compile: javaFileName=" + javaFileName + "\n");

		// Start capturing the System.err output for this thread
		SystemLogHandler.setThread();

		// Initializing classpath
		String path = System.getProperty("java.class.path");
		info.append("    cmd cp=" + path + "\n");
		info.append("    ctx cp=" + classpath + "\n");
		path += sep;
		path += classpath;

		if (log.isDebugEnabled())
			log.debug("Using classpath: " + path);

		parameters.add("-classpath");
		parameters.add(path);

		// Initializing sourcepath
		parameters.add("-sourcepath");
		parameters.add(options.getScratchDir().getPath());

		info.append("    work dir=" + options.getScratchDir() + "\n");

		// Initialize and set java extensions
		String extdirs = System.getProperty("java.ext.dirs");
		if (extdirs != null) {
			parameters.add("-extdirs");
			parameters.add(extdirs);
			info.append("    extension dir=" + extdirs + "\n");
		}

		if (ctxt.getOptions().getFork()) {
			String endorsed = System.getProperty("java.endorsed.dirs");
			if (endorsed != null) {
				parameters.add("-endorseddirs"); // "-J-Djava.endorsed.dirs="+endorsed
				parameters.add(endorsed);
				info.append("    endorsed dir=" + endorsed + "\n");
			} else {
				info.append("    no endorsed dirs specified\n");
			}
		}

		if (ctxt.getOptions().getClassDebugInfo())
			parameters.add("-g");

		Exception ie = null;

		// Set the Java compiler to use
		if (javaCompiler == null) {
			// assumption, there is no dynamic changing Java compiler 
			String compiler = options.getCompiler();
			if (compiler == null)
				compiler = "com.sun.tools.javac.Main";
			// verify compiler
			try {
				javaCompiler = Class.forName(compiler);
			} catch (ClassNotFoundException cnfe) {
				// try to figure out class path to compiler
				String compileClassPath = System.getProperty("java.home");
				if (compileClassPath == null)
					try {
						compileClassPath = System.getenv("JAVA_HOME");
						if (compileClassPath == null)
							compileClassPath = System.getenv("java_home");
					} catch (SecurityException se) {

					}

				if (compileClassPath != null) {
					// HACK for now
					compileClassPath = compileClassPath.replace("jre", "jdk");
					compileClassPath += "/lib/tools.jar";
					info.append("    checking default compiler in " + compileClassPath + "\n");
					try {
						javaCompiler = Class.forName(compiler, true, new URLClassLoader(new URL[] { new URL("file",
								"localhost", compileClassPath) }));
					} catch (Error er) {
						log.error("Setting up Java compiler error ", er);
					} catch (Exception ex) {
						log.error("Setting up Java compiler exception ", ex);
					}
				} else
					info.append("    no Java home path specified\n");
			}
			info.append("    compiler=" + compiler + "\n");
		}

		if (options.getCompilerTargetVM() != null) {
			parameters.add("-target");
			parameters.add(options.getCompilerTargetVM());
			info.append("   compilerTargetVM=" + options.getCompilerTargetVM() + "\n");
		}

		if (options.getCompilerSourceVM() != null) {
			parameters.add("-source");
			parameters.add(options.getCompilerSourceVM());
			info.append("   compilerSourceVM=" + options.getCompilerSourceVM() + "\n");
		}

		info.append("   JavaPath=" + ctxt.getJavaPath() + "\n");

		parameters.add(javaFileName);

		boolean compilationErrors = false;
		String errorCapture = null;
		if (javaCompiler != null)
			try {
				Integer success;
				Method cm = javaCompiler.getMethod("compile", new Class[] { String[].class });
				if (ctxt.getOptions().getFork()) {
					success = (Integer) cm.invoke(null, new Object[] { parameters
							.toArray(new String[parameters.size()]) });
				} else {
					synchronized (javacLock) {
						success = (Integer) cm.invoke(null, new Object[] { parameters.toArray(new String[parameters
								.size()]) });
					}
				}
				if (success.intValue() != 0)
					compilationErrors = true;
			} catch (Throwable t) {
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
				if (t instanceof InvocationTargetException)
					t = t.getCause();
				if (t instanceof Exception)
					ie = (Exception) t;
				else
					ie = new Exception(t);
				log.error("Javac exception ", t);
				log.error("Env: " + info.toString());
			} finally {
				// Stop capturing the System.err output for this thread
				errorCapture = SystemLogHandler.unsetThread();
			}
		if (compilationErrors && errorCapture != null) {
			errorReport.append(System.getProperty("line.separator"));
			errorReport.append(errorCapture);
		}

		if (!ctxt.keepGenerated()) {
			if (new File(javaFileName).delete() == false)
				log.error("Couldn't delete source: " + javaFileName);
		}

		if (compilationErrors || ie != null) {
			String errorReportString = errorReport.toString();
			log.error("Error compiling file: " + javaFileName + " " + errorReportString);
			JavacErrorDetail[] javacErrors = ErrorDispatcher.parseJavacErrors(errorReportString, javaFileName,
					pageNodes);
			if (javacErrors != null) {
				errDispatcher.javacError(javacErrors);
			} else {
				errDispatcher.javacError(errorReportString, ie);
			}
		}

		if (log.isDebugEnabled()) {
			long t2 = System.currentTimeMillis();
			log.debug("Compiled " + ctxt.getServletJavaFileName() + " " + (t2 - t1) + "ms");
		}

		if (ctxt.isPrototypeMode()) {
			return;
		}

		// JSR45 Support
		if (!options.isSmapSuppressed()) {
			log.debug("Install Smap " + (smap == null ? "null" : Arrays.toString(smap)));
			SmapUtil.installSmap(smap);
		}
	}

    protected static class SystemLogHandler extends PrintStream {


        // ----------------------------------------------------------- Constructors


        /**
         * Construct the handler to capture the output of the given steam.
         */
        public SystemLogHandler(PrintStream wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }


        // ----------------------------------------------------- Instance Variables


        /**
         * Wrapped PrintStream.
         */
        protected PrintStream wrapped = null;


        /**
         * Thread <-> PrintStream associations.
         */
        protected static ThreadLocal streams = new ThreadLocal();


        /**
         * Thread <-> ByteArrayOutputStream associations.
         */
        protected static ThreadLocal data = new ThreadLocal();


        // --------------------------------------------------------- Public Methods


        public PrintStream getWrapped() {
          return wrapped;
        }

        /**
         * Start capturing thread's output.
         */
        public static void setThread() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            data.set(baos);
            streams.set(new PrintStream(baos));
        }


        /**
         * Stop capturing thread's output and return captured data as a String.
         */
        public static String unsetThread() {
            ByteArrayOutputStream baos = 
                (ByteArrayOutputStream) data.get();
            if (baos == null) {
                return null;
            }
            streams.set(null);
            data.set(null);
            return baos.toString();
        }


        // ------------------------------------------------------ Protected Methods


        /**
         * Find PrintStream to which the output must be written to.
         */
        protected PrintStream findStream() {
            PrintStream ps = (PrintStream) streams.get();
            if (ps == null) {
                ps = wrapped;
            }
            return ps;
        }


        // ---------------------------------------------------- PrintStream Methods


        public void flush() {
            findStream().flush();
        }

        public void close() {
            findStream().close();
        }

        public boolean checkError() {
            return findStream().checkError();
        }

        protected void setError() {
            //findStream().setError();
        }

        public void write(int b) {
            findStream().write(b);
        }

        public void write(byte[] b)
            throws IOException {
            findStream().write(b);
        }

        public void write(byte[] buf, int off, int len) {
            findStream().write(buf, off, len);
        }

        public void print(boolean b) {
            findStream().print(b);
        }

        public void print(char c) {
            findStream().print(c);
        }

        public void print(int i) {
            findStream().print(i);
        }

        public void print(long l) {
            findStream().print(l);
        }

        public void print(float f) {
            findStream().print(f);
        }

        public void print(double d) {
            findStream().print(d);
        }

        public void print(char[] s) {
            findStream().print(s);
        }

        public void print(String s) {
            findStream().print(s);
        }

        public void print(Object obj) {
            findStream().print(obj);
        }

        public void println() {
            findStream().println();
        }

        public void println(boolean x) {
            findStream().println(x);
        }

        public void println(char x) {
            findStream().println(x);
        }

        public void println(int x) {
            findStream().println(x);
        }

        public void println(long x) {
            findStream().println(x);
        }

        public void println(float x) {
            findStream().println(x);
        }

        public void println(double x) {
            findStream().println(x);
        }

        public void println(char[] x) {
            findStream().println(x);
        }

        public void println(String x) {
            findStream().println(x);
        }

        public void println(Object x) {
            findStream().println(x);
        }

    }

}
