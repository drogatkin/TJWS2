package tjws.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import Acme.Utils;

/**
 * @ServerEndpoint gives the relative name for the end point This will be
 *                 accessed via ws://localhost:8080/echoserver/echo Where
 *                 "localhost" is the address of the host, "FileupdateServer" is
 *                 the name of the package and "monitor" is the address to
 *                 access this class from the server
 */
@ServerEndpoint(value = "/slides/{mode}", decoders = { SlideServer.String2Int.class })
public class SlideServer {
	
	@OnOpen
	public void registerInRoom(Session ses, @PathParam("mode") String mode) {
		ses.getUserProperties().put("mode", mode);
	}
	
	@OnMessage
	public void showSlideNo(Integer slideNo, Session ses, @PathParam("mode") String mode) {
		List<String> params = ses.getRequestParameterMap().get("dir");
		if(params == null || params.size() == 0)
			return;
		File slideDir = new File(params.get(0).trim());
		File[] slides = slideDir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String n = pathname.getName().toLowerCase();
				return pathname.isFile() && (n.endsWith(".jpg") || n.endsWith(".gif") || n.endsWith(".png") || n.endsWith(".jpeg"));
			}
		});
		if(slides == null || slides.length == 0) {
			try {
				ses.getBasicRemote().sendText("Empty or non existent " + slideDir);
			} catch(IOException e1) {
				e1.printStackTrace();
			}
			return;
		}
		if(slides.length > 0) {
			if(slideNo < 0)
				slideNo = 0;
			else if(slideNo > slides.length - 1)
				slideNo = slides.length - 1;
		}
		
		for(Session s : ses.getOpenSessions()) {
			if(mode.equals(s.getUserProperties().get("mode")))
				showSlide(slides[slideNo], s);
		}
	}
	
	private void showSlide(File slide, Session ses) {
		try(FileInputStream slideIm = new FileInputStream(slide); OutputStream webIm = ses.getBasicRemote().getSendStream()) {
			Utils.copyStream(slideIm, webIm, 0);
		} catch(Exception e) {
			e.printStackTrace();
			try {
				ses.getBasicRemote().sendText("Can't open " + e);
			} catch(IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public static class String2Int implements Decoder.Text<Integer> {
		
		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void init(EndpointConfig arg0) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public Integer decode(String arg0) throws DecodeException {
			try {
				return Integer.parseInt(arg0);
			} catch(NumberFormatException e) {
				throw new DecodeException(arg0, "Can't decode", e);
			}
		}
		
		@Override
		public boolean willDecode(String arg0) {
			try {
				decode(arg0);
				return true;
			} catch(DecodeException e) {
				
			}
			return false;
		}
		
	}
}