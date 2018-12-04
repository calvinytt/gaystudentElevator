package AppKickstarter.myThreads.ClientProcess;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.misc.MBox;
import AppKickstarter.misc.Msg;
import AppKickstarter.myThreads.ClientThread;
import ElevatorSystem.SocketManager;

public class SocketPassengerProcess implements ClientThread.SocketInOutPut {
    @Override
    public String initialProcess(ClientThread clientThread, String dataString) {
        clientThread.type = SocketManager.ClientType.PassengerStream;
        SocketManager._instance.addToClientList(clientThread, clientThread.type);

        CentralControlGUIProcess.startOutputThread(clientThread);

        return null;
    }

    @Override
    public String inputProcess(ClientThread clientThread, String dataString) {
        // send message the central control to read
        AppThread center = AppKickstarter._instance.getThread("CentralControl");
        MBox centralMBox = center.getMBox();
        centralMBox.send(new Msg(clientThread.getID(), clientThread.getMBox(), Msg.Type.SocketMsg, dataString));
        return dataString;
    }

    @Override
    public String outputProcess(ClientThread clientThread, String dataString) {

        return dataString;
    }
}
