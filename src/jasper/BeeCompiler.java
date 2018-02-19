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
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.apache.jasper.JasperException;
import org.apache.jasper.util.SystemLogHandler;

/**
 * Main JSP compiler class. This class uses 7Bee for compiling.
 * 
 * @author Dmitriy Rogatkin
 */
public class BeeCompiler extends Compiler {

	static {
		System.setErr(new SystemLogHandler(System.err));
	}

	/**
	 * Compile the servlet from .java file to .class file
	 */
	protected void generateClass(String[] smap) throws FileNotFoundException, JasperException, Exception {

		long t1 = 0;
		if (log.isDebugEnabled()) {
			t1 = System.currentTimeMillis();
		}

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

		// Set the Java compiler to use
		String compiler = options.getCompiler();
		if (compiler == null) {
			compiler = "com.sun.tools.javac.Main";
		}
		info.append("    compiler=" + compiler + "\n");

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
		Exception ie = null;
		try {
			Integer success;
			Method cm = Class.forName(compiler).getMethod("compile", new Class[] { String[].class });
			if (ctxt.getOptions().getFork()) {
				success = (Integer)cm.invoke(null, new Object[] {parameters.toArray(new String[parameters.size()])});
			} else {
				synchronized (javacLock) {
					success = (Integer) cm.invoke(null, new Object[] {parameters.toArray(new String[parameters.size()])});
				}
			}
			if (success.intValue() != 0)
				compilationErrors = true;
		} catch (Throwable t) {
			if (t instanceof InvocationTargetException)
				t = t.getCause();
			if (t instanceof Exception)
				ie = (Exception)t;
			else
				ie = new Exception(t);
			log.error("Javac exception ", t);
			log.error("Env: " + info.toString());
		}

		// Stop capturing the System.err output for this thread
		String errorCapture = SystemLogHandler.unsetThread();
		if (compilationErrors && errorCapture != null) {
			errorReport.append(System.getProperty("line.separator"));
			errorReport.append(errorCapture);
		}

		if (!ctxt.keepGenerated()) {
			File javaFile = new File(javaFileName);
			javaFile.delete();
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
			log.debug("Install Smap " + (smap==null?"null":Arrays.toString(smap)));
			SmapUtil.installSmap(smap);
		}
	}

}
