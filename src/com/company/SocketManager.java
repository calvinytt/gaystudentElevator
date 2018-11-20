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

	int port = 55555;
	String ipAdr = "127.0.0.1";
	static String clientName; // each thread has its own clientName

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

			while (!end) {
				int bytesRead = in.read(messageByte);
				System.out.println(bytesRead);
				dataString = new String(messageByte, 0, bytesRead);
				if (dataString.length() == 100) {
					end = true;
				}
				System.out.println("MESSAGE: " + dataString);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

//		DataInputStream in = new DataInputStream(cSocket.getInputStream());
//		// receive client
//		byte[] buffer = new byte[2048];
//		System.out.println("Length: " + in.readInt());
//		int sizeName = in.readInt();
//		in.read(buffer, 0, sizeName);
//		clientName = new String(buffer, 0, sizeName);
//		System.out.println(clientName);
	};

	public static void main(String args[]) throws IOException {
		new SocketManager();
	}
}
