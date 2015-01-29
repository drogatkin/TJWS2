package rogatkin.wskt;

import java.net.Socket;

import java.util.Map;
import java.nio.channels.SocketChannel;

import Acme.Serve.Serve.WebsocketProvider;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

public class SimpleProvider implements WebsocketProvider {

	@Override
	public void init(Map properties) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handshake(Socket socket, Servlet servlet) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
