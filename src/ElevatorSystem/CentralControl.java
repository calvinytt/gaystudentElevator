package ElevatorSystem;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.misc.Msg;

import java.util.logging.Level;

public class CentralControl extends AppThread {
    public static CentralControl _instance;

    //SocketManager socketManager;
    ElevatorManager elevatorManager;
    int maxFloorNum = -1, minFloorNum = -1;

    public CentralControl(AppKickstarter appKickstarter){
        super("CentralControl", appKickstarter);
        _instance = this;
        appKickstarter.startApp();
        appKickstarter.startAndRegTread(this);

        int eleNum = Integer.parseInt(appKickstarter.getProperty("Bldg.NElevators"));
        minFloorNum = Integer.parseInt(appKickstarter.getProperty("Bldg.MinFloorNumber"));
        maxFloorNum = Integer.parseInt(appKickstarter.getProperty("Bldg.MaxFloorNumber"));
        Elevator.timeOutTime = (long)(Double.parseDouble(appKickstarter.getProperty("Timer.AccessTimeOutTime"))*1000);

        elevatorManager = new ElevatorManager(appKickstarter,eleNum);

        new SocketManager(appKickstarter);

    }

    public void stopApp() {
        appKickstarter.stopApp();
    }

    public static void main(String[] args) {
        AppKickstarter appKickstarter = new AppKickstarter("AppKickstarter", "etc/MyApp.cfg");
        new CentralControl(appKickstarter);
    }

    public void run() {

        for (boolean quit = false; !quit; ) {
            Msg msg = mbox.receive();

            //log.info(id + ": message received: [" + msg + "].");

            String details;

            switch (msg.getType()) {
                case SocketMsg:
                    readMsg(msg);
                    break;

                case Restart:
                    mbox.cleanUpMBox();
                    elevatorManager.restart();
                    break;

                case Terminate:
                    quit = true;
                    break;

                default:
                    log.severe(id + ": unknown message type!!");
                    break;
            }
        }
    }

    public void replyElevator(String message) {
        SocketManager._instance.sendMsgToClient(new Msg(id, mbox, Msg.Type.SocketMsg, message), SocketManager.ClientType.PassengerStream);
    }

    public void readMsg(Msg msg) {
        String message = "";
        String[] args = msg.getDetails().split(" ");
        switch (args[0]) {
            case "Svc_Req":
                if (args.length != 4) {
                    appKickstarter.getLogger().log(Level.WARNING, "Wrong args number of message Svc_Req");
                    return;
                }

                String pID = args[1];
                int srcFNo = Integer.parseInt(args[2]);
                int dstFNo = Integer.parseInt(args[3]);

                if (srcFNo < minFloorNum || srcFNo > maxFloorNum) {
                    appKickstarter.getLogger().log(Level.WARNING, "Svc_Req src floor "+ srcFNo +" is not in range: " + minFloorNum + ", " + maxFloorNum);
                    return;
                }
                if (dstFNo < minFloorNum || dstFNo > maxFloorNum) {
                    appKickstarter.getLogger().log(Level.WARNING, "Svc_Req src floor "+ dstFNo +" is not in range: " + minFloorNum + ", " + maxFloorNum);
                    return;
                }

                message = "Svc_Reply " + pID + " " + srcFNo + " " + dstFNo + " ";

                elevatorManager.addPassenger(message, pID, srcFNo, dstFNo);

                break;
            case "Elev_Dep":
            case "Elev_Arr":
                if (args.length < 5){
                    appKickstarter.getLogger().log(Level.WARNING, "Wrong args number of message Elev_Dep or Elev_Arr");
                    return;
                }
                message = msg.getDetails();
                if (args[args.length-1] == "NoPassenger") {
                    message = message.replaceAll(" NoPassenger", "");
                } else {
                    SocketManager._instance.sendMsgToClient(msg, SocketManager.ClientType.PassengerStream);
                    SocketManager._instance.sendMsgToClient(msg, SocketManager.ClientType.LiftUI);
                    SocketManager._instance.sendMsgToClient(msg, SocketManager.ClientType.CentralControlUI);
                }
                break;
            case "Elev_Power":
                if (args.length != 3) {
                    appKickstarter.getLogger().log(Level.WARNING, "Wrong args number of message Elev_Dep or Elev_Arr");
                    return;
                }

                int eNo = ((int)args[1].indexOf(0)) - 65;
                boolean turnOn = args[2].toUpperCase() == "TRUE";

                elevatorManager.powerElevator(eNo, turnOn);
                break;
            case "Elev_Num":
                msg.getSenderMBox().send(new Msg(id, mbox, Msg.Type.SocketMsg, "nOofLift: " + elevatorManager.getEleNum()));
                break;
            default:
                appKickstarter.getLogger().warning("Wrong message: " + args[0]);
        }
    }


}
