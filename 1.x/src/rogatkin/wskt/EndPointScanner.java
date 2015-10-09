package rogatkin.wskt;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ClassAnnotationMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.InterfaceMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.SubclassMatchProcessor;

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

public class EndPointScanner {

	protected static final char[] CR = { '\r', '\n' };

	public static void main(String... args) {
		File web_inf = new File(args[0]);
		if (web_inf.exists() == false || web_inf.isAbsolute() == false) {
			System.out.printf("Argument %s doesn't point ot valid WEB-INF directory%n", args[0]);
			System.exit(-1);
		}
		ArrayList<File> cp = new ArrayList<>();
		File classes = new File(web_inf, "classes");
		if (classes.exists() && classes.isDirectory())
			cp.add(classes);
		File lib = new File(web_inf, "lib");
		if (lib.exists() && lib.isDirectory()) {
			cp.addAll(Arrays.asList(lib.listFiles(new FileFilter() {

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
		try (FileWriter w = new FileWriter(args[1]);) {
			new EndPointScanner().scan(cp, w);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void usage() {

	}

	public void scan(final List<File> cp, final Writer w) {
		new FastClasspathScanner("") {
			@Override
			public List<File> getUniqueClasspathElements() {
				return cp;
			}

			@Override
			public ClassLoader getClassLoader() {
				URL[] urls = new URL[cp.size()];
				for(int j=0, n=cp.size(); j<n;j++)
					try {
						urls[j]=cp.get(j).toURI().toURL();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				return new URLClassLoader(urls);
			}
		}.matchClassesImplementing(ServerApplicationConfig.class,
				new InterfaceMatchProcessor<ServerApplicationConfig>() {

					@Override
					public void processMatch(Class<? extends ServerApplicationConfig> arg0) {
						try {
							w.write("ServerApplicationConfig ");
							w.write(arg0.getName());
							w.write(CR);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}).matchClassesWithAnnotation(ServerEndpoint.class, new ClassAnnotationMatchProcessor() {
			public void processMatch(Class<?> matchingClass) {
				try {
					w.write("ServerEndpoint ");
					w.write(matchingClass.getName());
					w.write(CR);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).matchSubclassesOf(Endpoint.class, new SubclassMatchProcessor<Endpoint>() {

			@Override
			public void processMatch(Class<? extends Endpoint> arg0) {
				try {
					w.write("Endpoint ");
					w.write(arg0.getName());
					w.write(CR);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).scan();

	}
}
