package tjws.test;

import java.io.IOException;
import java.nio.file.*;
import java.io.File;
import javax.websocket.*;
import javax.websocket.server.*;

/**
 * @ServerEndpoint gives the relative name for the end point This will be
 *                 accessed via ws://localhost:8080/EchoChamber/echo Where
 *                 "localhost" is the address of the host, "FileupdateServer" is
 *                 the name of the package and "monitor" is the address to
 *                 access this class from the server
 */
@ServerEndpoint(value = "/monitor/{directory}", decoders = { FileupdateServer.Filecoders.class }, encoders = { FileupdateServer.Filecoders.class })
public class FileupdateServer implements Runnable {
	WatchService watchService;
	Session session;
	Thread pollThr;
	
	@OnOpen
	public void setMonitor(@PathParam("directory") String directory, Session s) {
		System.out.printf("Watch for %s%n", directory);
		try {
			watchService = FileSystems.getDefault().newWatchService();
			session = s;
			Path p = new File(directory).toPath();
			p.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			session.getBasicRemote().sendText("Registered " + p);
			pollThr = new Thread(this);
			pollThr.start();
		} catch(IOException e) {
			try {
				session.getBasicRemote().sendText("Can't setup watcher :" + e);
			} catch(IOException e2) {
				
			}
		}
	}
	
	@Override
	public void run() {
		for(;;) {
			try {
				WatchKey watchKey = watchService.take(); // poll(10, );
				if(!processWatchKey(watchKey))
					break;
			} catch(InterruptedException ie) {
				break;
			}
		}
	}
	
	@OnClose
	public void stopWatch() {
		if(pollThr != null)
			pollThr.interrupt();
	}
	
	private boolean processWatchKey(WatchKey watchKey) {
		for(WatchEvent<?> event : watchKey.pollEvents()) {
			if(StandardWatchEventKinds.OVERFLOW == event.kind())
				continue;
			try {
				session.getBasicRemote().sendObject(((Path) event.context()).toFile());
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return watchKey.reset();
		
	}
	
	public static class Filecoders implements Encoder.Text<File>, Decoder.Text<File> {
		
		public File decode(String s) throws DecodeException {
			return new File(s);
		}
		
		public boolean willDecode(String s) {
			return true;
		}
		
		public String encode(File file) throws EncodeException {
			return file.getPath();
		}
		
		public void init(EndpointConfig config) {
			
		}
		
		public void destroy() {
			
		}
		
	}
}