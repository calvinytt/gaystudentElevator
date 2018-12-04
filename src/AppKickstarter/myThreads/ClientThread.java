package AppKickstarter.myThreads;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.myThreads.ClientProcess.CentralControlGUIProcess;
import AppKickstarter.myThreads.ClientProcess.KioskGUIProcess;
import AppKickstarter.myThreads.ClientProcess.LiftGUIProcess;
import AppKickstarter.myThreads.ClientProcess.SocketPassengerProcess;
import ElevatorSystem.SocketManager;

import java.io.*;
import java.net.Socket;


//======================================================================
// ClientThread
public class ClientThread extends AppThread {
    private Socket mySocket;
	private SocketInOutPut socketInOutPut;
	public BufferedReader in;
	public PrintWriter out;
	public boolean quit = false;
	public boolean skipInput = false;
	public boolean skipOutput = false;
	public SocketManager.ClientType type = null;
	public String UIId = null;

    //------------------------------------------------------------
    // ClientThread
    public ClientThread(String id, AppKickstarter appKickstarter, Socket mySocket) {
		super(id, appKickstarter);
		this.mySocket = mySocket;
		try {
			in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
			out = new PrintWriter(mySocket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // ClientThread

	public interface SocketInOutPut {
    	String initialProcess(ClientThread clientThread, String dataString);
		String inputProcess(ClientThread clientThread, String dataString);
		String outputProcess(ClientThread clientThread,String dataString) throws IOException;
	}

    //------------------------------------------------------------
    // run
    public void run() {
		String str = String.format(id + ": Client connected %s:%d through the local port %d",
				mySocket.getInetAddress().getHostAddress(), mySocket.getPort(),
				mySocket.getLocalPort());
		log.info(str);
		serve();
		log.info(id + " has disconnected");


		// declaring our departure
		appKickstarter.unregThread(this);
		SocketManager._instance.removeFromClientList(this, type, UIId);
		log.info(id + ": terminating...");
    } // run



	public void serve()  {
		String dataString = "";

		try {
			while (!quit) {
				//get message from socket
				if (!skipInput) {
					dataString = in.readLine();
					if (dataString != null) {
						if (dataString.equals("quit")) {
							quit = true;
							return;
						}
						dataString = dataString.replace("\n", "");
						log.info(id + ": receiving message - " + dataString);

						if (socketInOutPut == null) {
							if (dataString.contains("Svc_Req")) {
								socketInOutPut = new SocketPassengerProcess();

							} else if (dataString.contains("liftGUIclient")) {
								out.println("Run liftGUI");
								socketInOutPut = new LiftGUIProcess();
							} else if (dataString.contains("ControlGUIclient")) {
								out.println("Run ControlGUI");
								socketInOutPut = new CentralControlGUIProcess();
							} else if (dataString.contains("KioskGUIclient: Ready for KioskGUI setup")) {
                                socketInOutPut = new KioskGUIProcess();
                            }
							socketInOutPut.initialProcess(this, null);
						}

						//process input message
						dataString = socketInOutPut.inputProcess(this, dataString);
					}
				}
				if (!skipOutput)
					dataString = socketInOutPut.outputProcess(this, dataString);
			}
			log.severe(id + ": Terminate!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setQuit(boolean quit) {this.quit = quit;}
	public void setSkipInput (boolean skip) {skipInput = skip;}
} // ClientThread
