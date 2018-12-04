package ElevatorSystem;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.Msg;
import AppKickstarter.myThreads.ClientThread;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SocketManager {

	public static SocketManager _instance;

	private List<ClientThread> centralControlThread = new ArrayList();
	private List<ClientThread> passengerStreamThread = new ArrayList();
	private HashMap<String, List<ClientThread>> liftUIList = new HashMap<>();
	private List<ClientThread> clientThreads = new ArrayList();
	private static int socketIndex = 0;

	public enum ClientType {
		PassengerStream, LiftUI, ElevatorUI, CentralControlUI
	}

	public SocketManager(AppKickstarter appKickstarter) {
		_instance = this;
		try {
			int port = Integer.parseInt(AppKickstarter._instance.getProperty("Server.Port"));
			ServerSocket sSocket = new ServerSocket(port);
			appKickstarter.getLogger().info("Listening at port " + port);

			while (true) {
				Socket cSocket = sSocket.accept();
				ClientThread clientThread = new ClientThread("Socket-"+socketIndex, AppKickstarter._instance, cSocket);
				clientThreads.add(clientThread);
				appKickstarter.startAndRegTread(clientThread);
				socketIndex ++;
			}
		} catch (Exception e) {
			appKickstarter.getLogger().info("Cannot set up server");
		}
	}

	public void addToClientList(ClientThread clientThread, ClientType clientType) {
		addToClientList(clientThread, clientType, null);
	}

	public void addToClientList(ClientThread clientThread, ClientType clientType, String id) {
		switch (clientType) {
			case PassengerStream:
				passengerStreamThread.add(clientThread);
				break;
			case LiftUI:
				if (!liftUIList.containsKey(id))
					liftUIList.put(id, new ArrayList<>());
				liftUIList.get(id).add(clientThread);
				break;
			case CentralControlUI:
				centralControlThread.add(clientThread);
				break;
		}
	}

	/**
	 *
	 * @param msg
	 * @param clientType
	 */
	public void sendMsgToClient(Msg msg, ClientType clientType) {
		switch (clientType) {
			case PassengerStream:
				passengerStreamThread.forEach((clientThread -> {
					clientThread.getMBox().send(msg);
				}));
				break;
			case LiftUI:
				String args[] = msg.getDetails().split(" ");
				if (liftUIList.containsKey(args[1])) {
					List<ClientThread> listGUIs = liftUIList.get(args[1]);
					listGUIs.forEach(clientThread -> {
						System.out.println("asd");
						clientThread.getMBox().send(msg);
					});
				}
				break;
			case CentralControlUI:
				centralControlThread.forEach((clientThread -> {
					clientThread.getMBox().send(msg);
				}));
				break;
		}
	}

	public void removeFromClientList(ClientThread clientThread, ClientType clientType, String id) {
		switch (clientType) {
			case PassengerStream:
				passengerStreamThread.remove(clientThread);
				break;
			case LiftUI:
				if (!liftUIList.containsKey(id))
					liftUIList.remove(clientThread);
				break;
			case CentralControlUI:
				centralControlThread.remove(clientThread);
				break;
		}
	}

	public static void main(String args[]) throws IOException {
		new SocketManager(new AppKickstarter("AppKickstarter", "etc/MyApp.cfg"));
	}
}
