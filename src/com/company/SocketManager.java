package com.company;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketManager {

	Socket socket;
	ServerSocket serverSocket;
	DataInputStream in;
	DataOutputStream out;

	int port;
	static String clientName; // each thread has its own clientName
	String reply = "";

	public SocketManager() throws IOException {

		try {
			int port = 54321;
			ServerSocket sSocket = new ServerSocket(port);
			System.out.println("Listening at port " + port);

			while (true) {
				Socket cSocket = sSocket.accept();
				// create new thread when new user is connected
				Thread t = new Thread() {

					@Override
					public void run() {
						try {
							String str = String.format("Client connected %s:%d through the local port %d",
									cSocket.getInetAddress().getHostAddress(), cSocket.getPort(),
									cSocket.getLocalPort());
							System.out.println(str);
							serve(cSocket);
							System.out.println(clientName + " has disconnected");
						} catch (IOException e) {
						}
					}
				};
				t.start();
			}
		} catch (Exception e) {
			System.out.println("Cannot set up server");
		}
	}

	public void serve(Socket cSocket) throws IOException {
		byte[] messageByte = new byte[1000];
		boolean end = false;
		String dataString = "";

		try {
			DataInputStream in = new DataInputStream(cSocket.getInputStream());
			DataOutputStream out = new DataOutputStream(cSocket.getOutputStream());
			while (!end) {
				int bytesRead = in.read(messageByte);
				dataString = new String(messageByte, 0, bytesRead);
				if (dataString.length() == 100) {
					end = true;
				}
				System.out.println("MESSAGE: " + dataString);
				reply = dataString.toString();
//				out.writeInt(reply.length());
				System.out.println(reply.trim());
				out.writeUTF("Svc_Req Passenger-0002 3 4".toString());
				out.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	};

	public static void main(String args[]) throws IOException {
		new SocketManager();
	}
}
