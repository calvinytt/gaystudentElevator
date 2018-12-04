package AppKickstarter.myThreads.ClientProcess;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.misc.MBox;
import AppKickstarter.misc.Msg;
import AppKickstarter.myThreads.ClientThread;
import ElevatorSystem.SocketManager;


public class KioskGUIProcess implements ClientThread.SocketInOutPut {
    @Override
    public String initialProcess(ClientThread clientThread, String dataString) {
        clientThread.type = SocketManager.ClientType.KioskUI;
        clientThread.out.println("Run KioskGUI");
        startOutputThread(clientThread);

        return null;
    }

    private void startOutputThread(ClientThread clientThread) {
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
    public String inputProcess(ClientThread clientThread, String dataString) {
        if (dataString.equals("KioskGUI setup done")) {
            AppThread center = AppKickstarter._instance.getThread("CentralControl");
            MBox centralMBox = center.getMBox();
            centralMBox.send(new Msg(clientThread.getID(), clientThread.getMBox(), Msg.Type.SocketMsg, "Floor_Num "));
        } else if (dataString.contains("floor:")) {
            clientThread.UIId = dataString.split(" ")[1];
            SocketManager._instance.addToClientList(clientThread, SocketManager.ClientType.KioskUI, clientThread.UIId);
            clientThread.out.println("Floor number set");
        } else if (dataString.contains("Svc_Req")) {
            AppThread center = AppKickstarter._instance.getThread("CentralControl");
            MBox centralMBox = center.getMBox();
            centralMBox.send(new Msg(clientThread.getID(), clientThread.getMBox(), Msg.Type.SocketMsg, dataString));
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
