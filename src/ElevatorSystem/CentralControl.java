package ElevatorSystem;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.misc.Msg;

import java.util.logging.Level;

/**
 * Handling all major manager such as SocketManager for networking and ElevatorManager for Elevator controlling.
 * As well as handle the communication between components.
 * This class extends AppThread which can provide thread ,message box and logging function under AppKickstarter.
 */
public class CentralControl extends AppThread {
    public static CentralControl _instance;

    /**
     * Handling Elevator
     */
    ElevatorManager elevatorManager;

    /**
     * min and max floor for validation
     */
    int maxFloorNum = -1, minFloorNum = -1;

    /**
     * Constructor of the class.
     * 1. start the app appKickstarter
     * 2. read properties from config file: number of elevators, min floor number, max floor number
     * 3. create ElevatorManager and SocketManager
     * @param appKickstarter pass the AppKickstarter object
     */
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

    /**
     *Stop the application safety
     */
    public void stopApp() {
        appKickstarter.stopApp();
        elevatorManager.removeAllElevator();
    }

    /**
     * main method to start the whole application
     * @param args unused
     */
    public static void main(String[] args) {
        AppKickstarter appKickstarter = new AppKickstarter("AppKickstarter", "etc/MyApp.cfg");
        new CentralControl(appKickstarter);
    }

    /**
     * Handling message Msg from message box MBox. Implementing Runnable method.
     */
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

    /**
     * Reply the passenger lift request.
     * @param message The message to reply in format "Reply &lt;pID&gt; &lt;srcNo&gt; &lt;dstNo&gt; &lt;lNo&gt;". e.g. "Reply Passenger-0001 5 19 A"
     * @param clientType "The type of socket client you want to send to including Passenger Stream and some GUI"
     */
    public void replyElevator(String message, SocketManager.ClientType clientType) {
        String id = message.split(" ")[2];
        SocketManager._instance.sendMsgToClient(new Msg(id, mbox, Msg.Type.SocketMsg, message), clientType, id);
        if (clientType == SocketManager.ClientType.KioskUI)
            SocketManager._instance.sendMsgToClient(new Msg(id, mbox, Msg.Type.SocketMsg, "Server tell which lift"), clientType, id);
    }

    /**
     * Read the socket message from message box and do action for that message.
     * @param msg The message received
     */
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
                SocketManager.ClientType clientType = SocketManager.ClientType.PassengerStream;
                if (pID.contains("K"))
                    clientType = SocketManager.ClientType.KioskUI;

                if (srcFNo < minFloorNum || srcFNo > maxFloorNum) {
                    appKickstarter.getLogger().log(Level.WARNING, "Svc_Req src floor "+ srcFNo +" is not in range: " + minFloorNum + ", " + maxFloorNum);
                    return;
                }
                if (dstFNo < minFloorNum || dstFNo > maxFloorNum) {
                    appKickstarter.getLogger().log(Level.WARNING, "Svc_Req src floor "+ dstFNo +" is not in range: " + minFloorNum + ", " + maxFloorNum);
                    return;
                }

                message = "Svc_Reply " + pID + " " + srcFNo + " " + dstFNo + " ";

                elevatorManager.addPassenger(message, pID, srcFNo, dstFNo, clientType);

                break;
            case "Elev_Dep":
            case "Elev_Arr":
                if (args.length < 5){
                    appKickstarter.getLogger().log(Level.WARNING, "Wrong args number of message Elev_Dep or Elev_Arr");
                    return;
                }
                message = msg.getDetails();

                if (args[args.length-1].equals("updateToUI")) {
                    message = message.replaceAll(" updateToUI", "");
                    msg = new Msg(msg.getSender(),msg.getSenderMBox(),msg.getType(), message);
                } else {
                    SocketManager._instance.sendMsgToClient(msg, SocketManager.ClientType.PassengerStream);
                }
                SocketManager._instance.sendMsgToClient(msg, SocketManager.ClientType.LiftUI);
                SocketManager._instance.sendMsgToClient(msg, SocketManager.ClientType.CentralControlUI);
                break;
            case "Elev_state":
                if (args.length != 3) {
                    appKickstarter.getLogger().log(Level.WARNING, "Wrong args number of message Elev_state");
                    return;
                }
                SocketManager._instance.sendMsgToClient(msg, SocketManager.ClientType.CentralControlUI);
                break;
            case "Elev_Power":
                if (args.length != 3) {
                    appKickstarter.getLogger().log(Level.WARNING, "Wrong args number of message Elev_Power");
                    return;
                }

                int eNo = args[1].charAt(0) - 65;
                boolean turnOn = args[2].toUpperCase().equals("TRUE");
                elevatorManager.powerElevator(eNo, turnOn);
                break;
            case "Elev_Num":
                msg.getSenderMBox().send(new Msg(id, mbox, Msg.Type.SocketMsg, "nOofLift: " + elevatorManager.getEleNum()));
                break;
            case "Floor_Num":
                msg.getSenderMBox().send(new Msg(id, mbox, Msg.Type.SocketMsg, "floorNum: " + maxFloorNum));
                msg.getSenderMBox().send(new Msg(id, mbox, Msg.Type.SocketMsg,  "Server: please give floor"));
            case "Elev_start":
            case "Elev_stop":
                SocketManager._instance.sendMsgToClient(msg, SocketManager.ClientType.CentralControlUI);
                break;
            default:
                appKickstarter.getLogger().warning("Wrong message: " + args[0]);
        }
    }


}
