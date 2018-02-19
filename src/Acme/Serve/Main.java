/* tjws - Main.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  
 *  Visit http://tjws.sourceforge.net to get the latest information
 *  about Rogatkin's products.                                                        
 *  $Id: Main.java,v 1.25 2013/07/24 06:20:37 cvs Exp $                
 *  Created on Feb 22, 2007
 *  @author Dmitriy
 */
package Acme.Serve;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import Acme.Utils;

public class Main extends Serve {
	public static final String CLI_FILENAME = "cmdparams";

	private static final String progName = "Serve";

	protected static Serve serve;

	private static Thread sdHook;
	
	/** main entry for standalone run
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		//int code = main1(args);
		Runtime.getRuntime().halt(main1(args));
	}

	/**
	 * Main entry for embedded run
	 * @param args
	 */
	public static int main1(String[] args) {
		String workPath = System.getProperty("user.dir", ".");
		StringBuffer messages = null;

		int argc = args.length;
		int argn;
		if (argc == 0) { // a try to read from file for java -jar server.jar
			args = readArguments(workPath, CLI_FILENAME);
			if (args == null) {
				messages = appendMessage(messages, "Can't read from CLI file ("+CLI_FILENAME+") at "+workPath+"\n");
			} else
				argc = args.length;
		}

		Map arguments = new HashMap(20);
		arguments.put(ARG_WORK_DIRECTORY, workPath);
		// Parse args.
		// TODO: redesign process of parameters based on a map
		for (argn = 0; argn < argc && args[argn].length() > 0 && args[argn].charAt(0) == '-';) {
			if (args[argn].equals("-p") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_PORT, new Integer(args[argn]));
			} else if (args[argn].equals("-t") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_THROTTLES, args[argn]);
			} else if (args[argn].equals("-s") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_SERVLETS, args[argn]);
			} else if (args[argn].equals("-r") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_REALMS, args[argn]);
			} else if (args[argn].equals("-a") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_ALIASES, args[argn]);
			} else if (args[argn].equals("-b") && argn + 1 < argc) {
				++argn;
				if (arguments.containsKey(ARG_BINDADDRESS)) 
					messages = appendMessage(messages, "Multiple usage of a bind address. "+args[argn]+" ignored\n");
				else
					arguments.put(ARG_BINDADDRESS, args[argn]);
			} else if (args[argn].equals("-k") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_BACKLOG, args[argn]/*new Integer(args[argn])*/);
			} else if (args[argn].equals("-j") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_JSP, args[argn]);
			} else if (args[argn].equals("-w") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_WAR, args[argn]);
			} else if (args[argn].equals("-c") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_CGI_PATH, args[argn]);
			} else if (args[argn].equals("-mka") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_MAX_CONN_USE, args[argn]);
				arguments.put(ARG_KEEPALIVE, Boolean.TRUE);
			} else if (args[argn].equals("-nka")) {
				arguments.put(ARG_KEEPALIVE, Boolean.FALSE);
			} else if (args[argn].equals("-sp")) {
				arguments.put(ARG_SESSION_PERSIST, Boolean.TRUE);
			} else if (args[argn].equals("-kat") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_KEEPALIVE_TIMEOUT, args[argn]);
				arguments.put(ARG_KEEPALIVE, Boolean.TRUE);
			} else if (args[argn].equals("-e") && argn + 1 < argc) {
				++argn;
				try {
					arguments.put(ARG_SESSION_TIMEOUT, new Integer(args[argn]));
				} catch (NumberFormatException nfe) {
				}
			} else if (args[argn].equals("-z") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_THREAD_POOL_SIZE, args[argn]);
				// backlog will be anyway upper limitation
			} else if (args[argn].equals("-d") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_LOG_DIR, args[argn]);
			} else if (args[argn].startsWith("-l")) {
				arguments
						.put(ARG_ACCESS_LOG_FMT,
								"{0}:{9,number,#} {1} {2} [{3,date,dd/MMM/yyyy:HH:mm:ss Z}] \"{4} {5} {6}\" {7,number,#} {8,number} {10} {11}");
				if (args[argn].length() > 2) {
					arguments.put(ARG_LOG_OPTIONS, args[argn].substring(2).toUpperCase());
					if (args[argn].indexOf('f') >= 0 && argn < argc-1) {
						++argn;
						arguments.put(ARG_ACCESS_LOG_FMT, args[argn]);
					}
				} else
					arguments.put(ARG_LOG_OPTIONS, "");
			} else if (args[argn].startsWith("-nohup")) {
				arguments.put(ARG_NOHUP, ARG_NOHUP);
			} else if (args[argn].equals("-m") && argn + 1 < argc) {
				++argn;
				try {
					arguments.put(ARG_MAX_ACTIVE_SESSIONS, new Integer(args[argn]));
					if (((Integer) arguments.get(ARG_MAX_ACTIVE_SESSIONS)).intValue() < DEF_MIN_ACT_SESS)
						arguments.put(ARG_MAX_ACTIVE_SESSIONS, new Integer(DEF_MIN_ACT_SESS));
				} catch (NumberFormatException nfe) {
					// ignored
				}
			} else if (args[argn].equals("-err")) {
				if (argn + 1 < argc && args[argn + 1].startsWith("-") == false) {
					++argn;
					try {
						arguments.put(ARG_ERR, (PrintStream) Class.forName(args[argn]).newInstance());
					} catch (Error er) {
						messages = appendMessage(messages,
								"Problem of processing class parameter of error redirection stream: ").append(er)
								.append('\n');
					} catch (Exception ex) {
						messages = appendMessage(messages,
								"Exception in processing class parameter of error redirection stream: ").append(ex)
								.append('\n');
					}
				} else
					arguments.put(ARG_ERR, System.err);
			} else if (args[argn].equals("-out")) {
				if (argn + 1 < argc && args[argn + 1].startsWith("-") == false) {
					++argn;
					try {
						arguments.put(ARG_OUT, (PrintStream) Class.forName(args[argn]).newInstance());
					} catch (Error er) {
						messages = appendMessage(messages,
								"Problem of processing class parameter of out redirection stream: ").append(er).append(
								'\n');
					} catch (Exception ex) {
						messages = appendMessage(messages,
								"Exception in processing class parameter of out redirection stream: ").append(ex)
								.append('\n');
					}
				}
			} else if (args[argn].equals("-sh")) {
				arguments.put(ARG_HTTPONLY_SC, ARG_HTTPONLY_SC);
			} else if (args[argn].equals("-ss")) {
				arguments.put(ARG_SECUREONLY_SC, ARG_SECUREONLY_SC);
			} else if (args[argn].equals("-g") && argn + 1 < argc) {
				arguments.put(ARG_LOGROLLING_LINES, Integer.valueOf(args[++argn]));
			} else if (args[argn].startsWith("-")) { // free args, note it generate problem since free arguments can match internal arguments
				if (args[argn].length() > 1) {
					String name = args[argn].substring(1);
					if (arguments.containsKey(name))
						messages = appendMessage(messages, "Multiple usage of  '-"+name+"'="+args[++argn]+ " ignored\n");
					else
						arguments.put(name,// .toUpperCase(),
							argn < argc - 1 ? args[++argn] : "");
				//System.out.println("Added free arg:"+args[argn-1]+"="+args[argn]);
				} else
					messages = appendMessage(messages, "Parameter '-' ignored, perhaps extra blank separator was used.\n");
			} else
				usage();

			++argn;
		}
		if (argn != argc)
			usage();
		if (System.getProperty(DEF_PROXY_CONFIG) != null)
			arguments.put(ARG_PROXY_CONFIG, System.getProperty(DEF_PROXY_CONFIG));
		// log and error stream manipulation 
		// TODO add log rotation feature, it can be done as plug-in
		PrintStream printstream = System.err;
		if (arguments.get(ARG_OUT) != null)
			printstream = (PrintStream) arguments.get(ARG_OUT);
		else {
			String logEncoding = System.getProperty(DEF_LOGENCODING);
			try {
				File logDir = new File(workPath);
				if (arguments.get(ARG_LOG_DIR) != null) {
					File dir = new File((String) arguments.get(ARG_LOG_DIR));
					if (dir.isAbsolute() == true) {
						logDir = dir;
					} else {
						logDir = new File(workPath, dir.getPath());
					}
				}
				File logFile = new File(logDir, "TJWS-" + System.currentTimeMillis() + ".log");
				int logRollThreshold = arguments.get(ARG_LOGROLLING_LINES)!=null?((Integer)arguments.get(ARG_LOGROLLING_LINES)).intValue():0;
				OutputStream logStream = logRollThreshold>1000?(OutputStream)new RollingOutputStream(logFile, logRollThreshold): (OutputStream)new FileOutputStream(logFile);
				if (logEncoding != null)
					printstream = new PrintStream(logStream, true, logEncoding); /* 1.4 */
				else
					printstream = new PrintStream(logStream, true);
			} catch (IOException e) {
				System.err.println("I/O problem at setting a log stream " + e);
			}
		}
		if (arguments.get(ARG_ERR) != null) {
			System.setErr((PrintStream) arguments.get(ARG_ERR));
		} else {
			System.setErr(printstream);
		}
		if (messages != null)
			System.err.println(messages);
		/**
		 * format path mapping from=givenpath;dir=realpath
		 */
		PathTreeDictionary mappingtable = new PathTreeDictionary();
		if (arguments.get(ARG_ALIASES) != null) {
			File file = new File((String) arguments.get(ARG_ALIASES));
			if (file.isAbsolute() == false)
				file = new File(workPath, file.getPath());
			if (file.exists() && file.canRead()) {
				try {
					// DataInputStream in = new DataInputStream(
					// new FileInputStream(file));
					BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
					do {
						String mappingstr = in.readLine(); // no arguments in non ASCII encoding allowed
						if (mappingstr == null)
							break;
						if (mappingstr.startsWith("#"))
							continue;
						StringTokenizer maptokenzr = new StringTokenizer(mappingstr, "=;");
						if (maptokenzr.hasMoreTokens()) {
							if (maptokenzr.nextToken("=").equalsIgnoreCase("from")) {
								if (maptokenzr.hasMoreTokens()) {
									String srcpath = maptokenzr.nextToken("=;").trim();
									if (maptokenzr.hasMoreTokens()
											&& maptokenzr.nextToken(";=").equalsIgnoreCase("dir"))
										try {
											if (maptokenzr.hasMoreTokens()) {
												File mapFile = new File(maptokenzr.nextToken());
												if (mapFile.isAbsolute() == false)
													mapFile = new File(workPath, mapFile.getPath());
												if (srcpath.endsWith("/*") == false)
													if (srcpath.endsWith("/"))
														srcpath += "*";
													else
														srcpath += "/*";
												if (mapFile.getCanonicalFile().exists())
													mappingtable.put(srcpath, mapFile);
												else
													System.err.println("TJWS: Mapping file " + mapFile + " (" + srcpath
															+ ") doesn't exist or not readable.");
											}
										} catch (NullPointerException e) {
										}
								}
							}
						}
					} while (true);
				} catch (IOException e) {
					System.err.println("TJWS: Problem reading aliases file: " + arguments.get(ARG_ALIASES) + "/" + e);
				}
			} else
				System.err.println("TJWS: File " + file + " (" + arguments.get(ARG_ALIASES)
						+ ") doesn't exist or not readable.");
		}
		// format realmname=path,user:password,,,,
		// TODO consider to add a role, like realmname=path,user:password[:role]
		PathTreeDictionary realms = new PathTreeDictionary();
		if (arguments.get(ARG_REALMS) != null) {
			try {
				File file = new File((String) arguments.get(ARG_REALMS));
				if (file.isAbsolute() == false)
					file = new File(workPath, file.getPath());
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

				do {
					String realmstr = in.readLine();
					if (realmstr == null)
						break;
					if (realmstr.startsWith("#"))
						continue;
					StringTokenizer rt = new StringTokenizer(realmstr, "=,:");
					if (rt.hasMoreTokens()) {
						String realmname = null;
						realmname = rt.nextToken();
						if (rt.hasMoreTokens()) {
							String realmPath = null;
							realmPath = rt.nextToken();
							if (rt.hasMoreTokens()) {
								String user = rt.nextToken();
								if (rt.hasMoreTokens()) {
									String password = rt.nextToken();
									BasicAuthRealm realm = null;
									Object o[] = realms.get(realmPath);
									if (o != null && o[0] != null)
										realm = (BasicAuthRealm) o[0];
									else {
										realm = new BasicAuthRealm(realmname);
										if (realmPath.endsWith("/*") == false)
											realmPath+="/*";
										else if (realmPath.endsWith("/"))
											realmPath+="*";
										realms.put(realmPath, realm);
									}
									realm.put(user, password);
								}
							}
						}
					}
				} while (true);
			} catch (IOException ioe) {
				System.err.println("TJWS: I/O problem in reading realms file " + arguments.get(ARG_REALMS) + ": " + ioe);
			}
		}
		// Create the server.
		serve = new Serve(arguments, printstream);
		// can use log(.. after this point
		serve.setMappingTable(mappingtable);
		serve.setRealms(realms);
		File tempFile = arguments.get(ARG_SERVLETS) == null ? null : new File((String) arguments.get(ARG_SERVLETS));
		if (tempFile != null && tempFile.isAbsolute() == false)
			tempFile = new File(workPath, tempFile.getPath());
		final File servFile = tempFile;
		// TODO analyze possible race condition
		if (servFile != null)
			new Thread(new Runnable() {
				public void run() {
					readServlets(servFile);
				}
			}).start();
		// And add the standard Servlets.
		String throttles = (String) arguments.get(ARG_THROTTLES);
		if (throttles == null)
			serve.addDefaultServlets((String) arguments.get(ARG_CGI_PATH));
		else
			try {
				serve.addDefaultServlets((String) arguments.get(ARG_CGI_PATH), throttles);
			} catch (IOException e) {
				serve.log("Problem reading throttles file: " + e, e);
				System.exit(1);
			}
		serve.addWebsocketProvider((String) arguments.get(ARG_WEBSOCKET));
		serve.addWarDeployer((String) arguments.get(ARG_WAR), throttles);
		if (arguments.get(ARG_NOHUP) == null)
			new Thread(new Runnable() {
				public void run() {
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					String line;
					while (true) {
						try {
							System.out.print("Press \"q\" <ENTER>, for gracefully stopping the server ");
							line = in.readLine();
							if (line != null && line.length() > 0 && line.charAt(0) == 'q') {
								serve.notifyStop();
								break;
							}
						} catch (IOException e) {
							serve.log("Exception in reading from console ", e);
							break;
						}
					}
				}
			}, "Stop Monitor").start();
		else {
			Runtime.getRuntime().addShutdownHook(sdHook = new Thread(new Runnable() {
				synchronized public void run() {
					serve.destroyAllServlets();
				}
			}, "ShutDownHook"));
		}
		// And run.
		int code = serve.serve();		
		if (code != 0 && arguments.get(ARG_NOHUP) == null)
			try {
				System.out.println();
				System.in.close(); // to break termination thread
			} catch (IOException e) {
			}
		try {
			if (sdHook != null)
				Runtime.getRuntime().removeShutdownHook(sdHook);
			serve.destroyAllServlets();
		} catch (IllegalStateException ise) {

		} catch (Throwable t) {
			if (t instanceof ThreadDeath)
				throw (ThreadDeath)t;
			serve.log("At destroying ", t);
		}
		killAliveThreads();
		printstream.close();
		return code;
	}

	private static StringBuffer appendMessage(StringBuffer messages, String message) {
		if (messages == null)
			messages = new StringBuffer(100);
		return messages.append(message);
	}

	public static String[] readArguments(String workPath, String file) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(workPath, file)));
			return Utils.splitStr(br.readLine(), "\"");
		} catch (Exception e) { // many can happen
			//e.printStackTrace();
			return null;
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException ioe) {
				}
		}
	}

	public static void stop() throws IOException {
		serve.notifyStop();
	}

	private static void usage() {
		System.out.println(Identification.serverName + " " + Identification.serverVersion + "\n" + "Usage:  "
				+ progName + " [-p port] [-s servletpropertiesfile] [-a aliasmappingfile]\n"
				+ "         [-b bind address] [-k backlog] [-l[a][r][f access_log_fmt]]\n"
				+ "         [-c cgi-bin-dir] [-m max_active_session] [-d log_directory]\n"
				+ "         [-sp] [-j jsp_servlet_class] [-w war_deployment_module_class]\n"
				+ "         [-nka] [-kat timeout_in_secs] [-mka max_times_connection_use]\n"
				+ "         [-e [-]duration_in_minutes] [-nohup] [-z max_threadpool_size]\n"
				+ "         [-err [class_name?PrintStream]] [-out [class_name?PrintStream]] [-g <rolling threshld>]\n"
				+ "         [-acceptorImpl class_name_of_Accpetor_impl [extra_acceptor_parameters] ]\n" + "  Legend:\n"
				+ "    -sp    session persistence\n" + "    -l     access log a - with user agent, and r - referer\n"
				+ "    -nka   no keep alive for connection");
		System.exit(1);
	}

	private static void killAliveThreads() {
		serve.serverThreads.interrupt();
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while (tg.getParent() != null)
			tg = tg.getParent();
		int ac = tg.activeCount() + tg.activeGroupCount() + 10;

		Thread[] ts = new Thread[ac];
		ac = tg.enumerate(ts, true);
		if (ac == ts.length)
			serve.log("Destroy:interruptRunningProcesses: Not all threads will be stopped.");
		// kill non daemon
		for (int i = 0; i < ac; i++)
			if (ts[i].isDaemon() == false) {
				String tn = ts[i].getName();
				//System.err.println("Interrupting and kill " + tn);

				if (ts[i] == Thread.currentThread() || "Stop Monitor".equals(tn) || "ShutDownHook".equals(tn)
						|| "DestroyJavaVM".equals(tn) || (tn != null && tn.startsWith("AWT-")) || "main".equals(tn))
					continue;
				ts[i].interrupt();
				Thread.yield();
				if (ts[i].isAlive()) {
					try {
						ts[i].stop();
					} catch (Throwable t) {
						if (t instanceof ThreadDeath) {
							serve
									.log(
											"Thread death exception happened and stopping thread, thread stopping loop will be terminated",
											t);
							throw (ThreadDeath) t;
						} else
							serve.log("An exception at stopping " + ts[i] + " " + t);
					}
				}
			}// else
		//serve.log("Daemon thread "+ts[i].getName()+" is untouched.");
	}

	private static void readServlets(File servFile) {
		/**
		 * servlet.properties file format servlet. <servletname>.code= <servletclass>servlet. <servletname>.initArgs= <name=value>, <name=value>
		 */
		Hashtable servletstbl, parameterstbl;
		servletstbl = new Hashtable();
		parameterstbl = new Hashtable();
		if (servFile != null && servFile.exists() && servFile.canRead()) {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(servFile)));
				/**
				 * format of servlet.cfg file servlet_name;servlet_class;init_parameter1=value1;init_parameter2=value2...
				 */
				do {
					String servletdsc = in.readLine();
					if (servletdsc == null)
						break;
					if (servletdsc.startsWith("#"))
						continue;
					StringTokenizer dsctokenzr = new StringTokenizer(servletdsc, ".=,", false);
					if (dsctokenzr.hasMoreTokens()) {
						if (!dsctokenzr.nextToken().equalsIgnoreCase("servlet")) {
							serve.log("No leading 'servlet' keyword, the sentence is skipped");
							break;
						}
						if (dsctokenzr.hasMoreTokens()) {
							String servletname = dsctokenzr.nextToken();

							while (dsctokenzr.hasMoreTokens()) {
								String lt = dsctokenzr.nextToken();
								if (lt.equalsIgnoreCase("code")) {
									if (dsctokenzr.hasMoreTokens())
										servletstbl.put(servletname, dsctokenzr.nextToken("="));
								} else if (lt.equalsIgnoreCase("initArgs")) {
									Hashtable initparams = new Hashtable();
									while (dsctokenzr.hasMoreTokens()) {
										String key = dsctokenzr.nextToken("=");
										if (key.startsWith(","))
											key = key.substring(1);
										//System.err.println("Key:"+key);
										try {
										   initparams.put(key, dsctokenzr.nextToken(",").substring(1).replaceAll("%2c",  ","));
										   //System.err.println("Key:"+key+" val:"+initparams.get(key));										
										} catch(NoSuchElementException nse) {
											initparams.put(key,"");
											break;
										}
									}
									//System.err.println("init:"+initparams+" for "+servletname);
									parameterstbl.put(servletname, initparams);
								} else {
									servletname +='.'+lt;
									serve
									.log("No expected token (code|initArgs), "+lt+" added to servlet "
											+ servletname +" for line: "+servletdsc);
								}
							}
						}
					}
				} while (true);
			} catch (IOException e) {
				serve.log("IO problem in processing servlets definition file (" + servFile + "): " + e);
			}
			Enumeration se = servletstbl.keys();
			String servletname;
			while (se.hasMoreElements()) {
				servletname = (String) se.nextElement();
				//System.err.println("Adding servlet fro "+servletname+" as "+servletstbl.get(servletname));
				serve.addServlet(servletname, (String) servletstbl.get(servletname), (Hashtable) parameterstbl
						.get(servletname));
			}
		} else
			serve.log("Servlets definition file neither provided, found, nor readable: " + servFile);
	}
	
	static class RollingOutputStream extends FilterOutputStream {
		private int rollingThresh;
		private File nameBase;
		private volatile int currentLine;
		private int numRoll;

		public RollingOutputStream(File file, int rollingSize) throws IOException {
			super(null);
			rollingThresh = rollingSize;
			if (rollingThresh < 1000)
				rollingThresh = 1000;
			nameBase = file;
			out = new FileOutputStream(nameBase);
		}

		//@Override 1.4
		public void flush() throws IOException {
			super.flush();
			if (currentLine++ > rollingThresh) {
				synchronized (this) {
					if (currentLine++ > rollingThresh) {
						out.close();
						if (nameBase.renameTo(new File(nameBase.getPath()+ "." + new DecimalFormat("0000").format(numRoll++)))) {
							out = new FileOutputStream(nameBase);
							currentLine = 0;
						} else {
							// TODO warn that roll didn't happen - overwriting
							out = new FileOutputStream(nameBase);
						}
					}
				}
			}
		}
	}
}
