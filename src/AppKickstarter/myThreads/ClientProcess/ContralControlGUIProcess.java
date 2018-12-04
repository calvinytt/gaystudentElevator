package AppKickstarter.myThreads.ClientProcess;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.misc.MBox;
import AppKickstarter.misc.Msg;
import AppKickstarter.myThreads.ClientThread;
import ElevatorSystem.SocketManager;

public class ContralControlGUIProcess implements ClientThread.SocketInOutPut {
    @Override
    public String initialProcess(ClientThread clientThread, String dataString) {
        SocketManager._instance.addToClientList(clientThread, SocketManager.ClientType.CentralControlUI);
        clientThread.type = SocketManager.ClientType.CentralControlUI;
        startOutputThread(clientThread);
        return null;
    }

    @Override
    public String inputProcess(ClientThread clientThread, String dataString) {
        if (dataString.equals("ControlGUI setup done")) {
            AppThread center = AppKickstarter._instance.getThread("CentralControl");
            MBox centralMBox = center.getMBox();
            centralMBox.send(new Msg(clientThread.getID(), clientThread.getMBox(), Msg.Type.SocketMsg, "Elev_Num "));
        } else if (dataString.contains("Stop")) {
            AppThread center = AppKickstarter._instance.getThread("CentralControl");
            MBox centralMBox = center.getMBox();
            centralMBox.send(new Msg(clientThread.getID(), clientThread.getMBox(), Msg.Type.SocketMsg, "Elev_Power " + dataString.split(" ")[1] + " FALSE"));
        }
        return dataString;
    }

    static void startOutputThread(ClientThread clientThread) {
        new Thread(() -> {
            MBox mbox = clientThread.getMBox();
            while (!clientThread.quit) {
                Msg msg = mbox.receive();
                switch (msg.getType()) {
                    case SocketMsg:
                        //process output message
                        clientThread.out.println(msg.getDetails());
                        AppKickstarter._instance.getLogger().info(clientThread.getID() + ": write message - " + msg.getDetails());
                        break;

                    case Terminate:
                        clientThread.setQuit(true);
                        break;
                    default:
                        AppKickstarter._instance.getLogger().severe(clientThread.getID() + ": unknown message type!!");
                        break;
                }
            }
        }).start();
    }

    @Override
    public String outputProcess(ClientThread clientThread, String dataString) {
        return dataString;
    }
}
