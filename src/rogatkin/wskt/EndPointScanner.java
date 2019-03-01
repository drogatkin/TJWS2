package rogatkin.wskt;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ClassAnnotationMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.InterfaceMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.SubclassMatchProcessor;

public class EndPointScanner {
	
	protected static final char[] CR = { '\r', '\n' };
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String... args) {
		if (args.length != 2) {
			usage();
			System.exit(1);
		}
		
		File webINFFile = new File(args[0]);
		if (webINFFile.exists() == false || webINFFile.isAbsolute() == false) {
			System.out.printf("Argument %s doesn't point ot valid WEB-INF directory%n", args[0]);
			System.exit(-1);
		}
		
		ArrayList<File> classPaths = new ArrayList<File>();
		File classes = new File(webINFFile, "classes");
		if (classes.exists() && classes.isDirectory()) {
			classPaths.add(classes);
		}
		
		File lib = new File(webINFFile, "lib");
		if (lib.exists() && lib.isDirectory()) {
			classPaths.addAll(Arrays.asList(lib.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File f) {
					if (f.isFile()) {
						String name = f.getName().toLowerCase();
						return name.endsWith(".jar") || name.endsWith(".zip");
					}
					return false;
				}
			})));
		}
		
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(args[1]);
			new EndPointScanner().scan(classPaths, fileWriter);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	static void usage() {
		System.out.printf("Usage: EndPointScanner <WEB-INF directory> <scan info file>%n");
	}
	
	/**
	 * Scans the class paths.
	 * 
	 * @param classPaths
	 * @param writer
	 */
	public void scan(final List<File> classPaths, final Writer writer) {
		new FastClasspathScanner("") {
			URLClassLoader classLoader;
			
			@Override
			public List<File> getUniqueClasspathElements() {
				return classPaths;
			}
			
			@Override
			public ClassLoader getClassLoader() {
				if (classLoader == null) {
					URL[] urls = new URL[classPaths.size()];
					for (int j = 0, n = classPaths.size(); j < n; j++)
						try {
							urls[j] = classPaths.get(j).toURI().toURL();
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					classLoader = new URLClassLoader(urls);
				}
				return classLoader;
			}
			
			@Override
			public void scan() {
				try {
					super.scan();
				} finally {
					try {
						if (classLoader != null) {
							classLoader.getClass().getMethod("close").invoke(classLoader);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			
		}.matchClassesImplementing(ServerApplicationConfig.class, new InterfaceMatchProcessor<ServerApplicationConfig>() {
			
			@Override
			public void processMatch(Class<? extends ServerApplicationConfig> arg0) {
				try {
					writer.write("ServerApplicationConfig ");
					writer.write(arg0.getName());
					writer.write(CR);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}).matchClassesWithAnnotation(ServerEndpoint.class, new ClassAnnotationMatchProcessor() {
			public void processMatch(Class<?> matchingClass) {
				try {
					writer.write("ServerEndpoint ");
					writer.write(matchingClass.getName());
					writer.write(CR);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}).matchSubclassesOf(Endpoint.class, new SubclassMatchProcessor<Endpoint>() {
			
			@Override
			public void processMatch(Class<? extends Endpoint> arg0) {
				try {
					writer.write("Endpoint ");
					writer.write(arg0.getName());
					writer.write(CR);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}).scan();
		
	}
}
