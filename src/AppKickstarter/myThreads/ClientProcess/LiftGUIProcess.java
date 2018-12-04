package AppKickstarter.myThreads.ClientProcess;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.misc.MBox;
import AppKickstarter.misc.Msg;
import AppKickstarter.myThreads.ClientThread;
import ElevatorSystem.SocketManager;

public class LiftGUIProcess  implements ClientThread.SocketInOutPut {
    @Override
    public String initialProcess(ClientThread clientThread, String dataString) {
        clientThread.type = SocketManager.ClientType.LiftUI;
        return null;
    }

    @Override
    public String inputProcess(ClientThread clientThread, String dataString) {
        if (dataString.equals("liftGUIclient")) {
            clientThread.out.println("Run liftGUI");
        } else if (dataString.equals("GUI setup done")) {
            AppThread center = AppKickstarter._instance.getThread("CentralControl");
            MBox centralMBox = center.getMBox();
            centralMBox.send(new Msg(clientThread.getID(), clientThread.getMBox(), Msg.Type.SocketMsg, "Elev_Num "));
        } else if (dataString.contains("Lift")){
            clientThread.UIId = dataString.split(" ")[1];
            SocketManager._instance.addToClientList(clientThread, SocketManager.ClientType.LiftUI, dataString.split(" ")[1]);
            clientThread.skipInput = true;
        }
        return dataString;
    }

    @Override
    public String outputProcess(ClientThread clientThread, String dataString) {
        MBox mbox = clientThread.getMBox();
        while (mbox.getMsgCnt() > 0) {
            Msg msg = mbox.receive();
            switch (msg.getType()) {
                case SocketMsg:
                    //process output message
                    clientThread.out.println(msg.getDetails());
                    AppKickstarter._instance.getLogger().finest(clientThread.getID() + ": write message - " + msg.getDetails());
                    break;

                case Terminate:
                    clientThread.setQuit(true);
                    break;
                default:
                    AppKickstarter._instance.getLogger().severe(clientThread.getID() + ": unknown message type!!");
                    break;
            }
        }

        return dataString;
    }
}
