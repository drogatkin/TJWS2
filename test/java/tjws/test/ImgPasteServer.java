package tjws.test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/paste/image")
public class ImgPasteServer {
	String imageName;
	
	@OnMessage
	public void processCommand(String cmd) {
		imageName = cmd;
	}
	
	@OnMessage
	public void processData(byte[] data) {
		try(OutputStream os = Files.newOutputStream(Paths.get(imageName))) {
			os.write(data);
		} catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
