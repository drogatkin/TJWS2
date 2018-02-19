package tjws.test;

import java.io.IOException;
 
import java.util.Date;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/** 
 * @ServerEndpoint gives the relative name for the end point
 * This will be accessed via ws://localhost:8080/EchoChamber/echo
 * Where "localhost" is the address of the host,
 * "EchoChamber" is the name of the package
 * and "echo" is the address to access this class from the server
 */
@ServerEndpoint(value="/echo/{room}", configurator = GetHttpSessionConfigurator.class) 
public class EchoServer {
    /**
     * @OnOpen allows us to intercept the creation of a new session.
     * The session class allows us to send data to the user.
     * In the method onOpen, we'll let the user know that the handshake was 
     * successful.
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config){
        System.out.println(session.getId() + " has opened a connection"); 
        boolean htp_sess = config.getUserProperties()
                .containsKey(HttpSession.class.getName());
        try {
            session.getBasicRemote().sendText((session.isSecure()?"Secure c":"C")+"onnection Established at "+new Date() +" htp session "+htp_sess);
            session.setMaxIdleTimeout(60*1000);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
 
    /**
     * When a user sends a message to the server, this method will intercept the message
     * and allow us to react to it. For now the message is read as a String.
     */
    @OnMessage
    public String onMessage(Session session, String message, @javax.websocket.server.PathParam("room") String room){
        System.out.printf("Message from %s/%dms : %s (%s)%n", session.getId(), session.getMaxIdleTimeout(),  message, room);
        /*try {
            session.getBasicRemote().sendText(message);

        } catch (IOException ex) {
            ex.printStackTrace();
        }*/
        String fromShare = "";
        for (Session s : session.getOpenSessions()) {
			if (s != session && s.isOpen() && s.getUserProperties().get("SHARE") != null)
					fromShare += " "+ (String) s.getUserProperties().get("SHARE");
		}
        session.getUserProperties().put("SHARE", message);
        if (fromShare .length() > 0)
        	message += " from others "+fromShare; 
        return message;
    }
 
    /**
     * The user closes the connection.
     * 
     * Note: you can't send messages to the client from this method
     */
    @OnClose
    public void onClose(Session session){
        System.out.println("Session " +session.getId()+" has ended at "+new Date());
    }
}