package tjws.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/upload/{file}", decoders = UploadServer.CmdDecoder.class, encoders = UploadServer.CmdEncoder.class)
public class UploadServer {
	
	RandomAccessFile uploadFile;
	String fileName;
	
	@OnMessage
	public void savePart(byte[] part, Session ses) {
		if(uploadFile == null) {
			if(fileName != null)
				try {
					uploadFile = new RandomAccessFile(fileName, "rw");
				} catch(FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
		}
		if(uploadFile != null)
			try {
				uploadFile.write(part);
				System.err.printf("Stored part of %db%n", part.length);
			} catch(IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	@OnMessage
	public void processCmd(CMD cmd, Session ses, @PathParam("file") String uploadDir) {
		switch(cmd.cmd) {
			case 1: // start
				fileName = cmd.data;
				if(uploadDir != null && uploadDir.isEmpty() == false) {
					// assure dir
					new File(uploadDir).mkdirs();
					fileName = uploadDir + File.separatorChar + fileName;
				}
				System.err.printf("Start upload of %s%n", cmd.data);
				break;
			case 2: // finish
				close(ses);
				cmd.cmd = 3;
				ses.getAsyncRemote().sendObject(cmd);
				break;
		}
	}
	
	@OnClose
	public void close(Session ses) {
		if(uploadFile != null) {
			try {
				uploadFile.close();
			} catch(IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			uploadFile = null;
			fileName = null;
		}
	}
	
	static class CMD {
		public int cmd;
		public String data;
	}
	
	public static class CmdDecoder implements Decoder.Text<CMD> {
		@Override
		public void init(final EndpointConfig config) {
		}
		
		@Override
		public void destroy() {
		}
		
		@Override
		public CMD decode(final String textMessage) throws DecodeException {
			CMD cmd = new CMD();
			JsonObject obj = Json.createReader(new StringReader(textMessage)).readObject();
			cmd.data = obj.getString("data");
			cmd.cmd = obj.getInt("cmd");
			return cmd;
		}
		
		@Override
		public boolean willDecode(final String s) {
			return true;
		}
	}
	
	public static class CmdEncoder implements Encoder.Text<CMD> {
		@Override
		public void init(final EndpointConfig config) {
		}
		
		@Override
		public void destroy() {
		}
		
		@Override
		public String encode(final CMD cmd) throws EncodeException {
			return Json.createObjectBuilder().add("cmd", cmd.cmd).add("data", cmd.data).build().toString();
		}
	}
}
