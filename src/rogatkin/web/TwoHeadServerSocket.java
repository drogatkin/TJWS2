/**
 * 
 */
package rogatkin.web;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Rohtash Singh Lakra (Rohtash.Lakra@nasdaq.com)
 * @date 03/19/2018 05:15:44 PM
 */
public class TwoHeadServerSocket extends ServerSocket {
	protected BlockingQueue<Socket> requestQueue;
	protected ServerSocket socket1, socket2;
	private Thread currentThread;
	
	public TwoHeadServerSocket(ServerSocket socket1, ServerSocket socket2) throws IOException {
		requestQueue = new LinkedBlockingQueue<Socket>(1000); // ?? backlog
		// ArrayBlockingQueue
		Thread thread;
		if (socket1 != null) {
			thread = new Thread(new AcceptQueuer(socket1), "Accept processor 1");
			thread.setDaemon(true);
			thread.start();
			this.socket1 = socket1;
		}
		if (socket2 != null) {
			thread = new Thread(new AcceptQueuer(socket2), "Accept processor 2");
			thread.setDaemon(true);
			thread.start();
			this.socket2 = socket2;
		}
	}
	
	public Socket accept() throws IOException {
		Socket result;
		currentThread = Thread.currentThread();
		for (;;) {
			try {
				result = requestQueue.poll(1000L, TimeUnit.SECONDS);
				if (result != null) {
					result.setSoTimeout(10 * 60 * 1000); // temp 10 mins
					return result;
				}
			} catch (InterruptedException e) {
				break;
			}
		}
		throw new IOException();
	}
	
	public void close() throws IOException {
		if (currentThread != null)
			currentThread.interrupt();
		IOException ioe = null;
		try {
			socket1.close();
		} catch (IOException ioe1) {
			ioe = ioe1;
		}
		socket2.close();
		if (ioe != null)
			throw ioe;
	}
	
	public String toString() {
		return "" + socket1 + "/" + socket2;
	}
	
	class AcceptQueuer implements Runnable {
		ServerSocket socket;
		
		AcceptQueuer(ServerSocket socket) {
			this.socket = socket;
		}
		
		public void run() {
			for (;;)
				try {
					requestQueue.put(socket.accept());
				} catch (InterruptedException e) {
					return;
				} catch (IOException e) {
					break;
				}
		}
	}
}
