package ElevatorSystem;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.Msg;
import AppKickstarter.myThreads.ElevatorThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Elevator {

    public enum ElevatorState{
        Off, Idle, Open, WaitForClose, Close, MovingUp, MovingDown, AccUp, AccDown, DecUp, DecDown
    }

    static int maxPassenger = 10; //not in user right now
    static long timeOutTime = 5000;

    private ElevatorThread thread;
    private AppKickstarter appKickstarter;

    int currFNo = 0;
    private int direction = 1;
    private boolean canGetIn = true;
    private boolean changingState = false;
    private boolean planedToOff = false;

    public char eNo;
    public List<Integer> DestFNo = new ArrayList<>();
    private ElevatorState state = ElevatorState.Idle;
    private List<Passenger> passengers = new ArrayList();

    //For locking data
    private List<Integer> accessingDataQuene = new ArrayList<>();
    private static int queueIndexId = 0;

    public void stateFinish() {
        appKickstarter.getLogger().fine(thread.getID() + ": floor " + currFNo + " " + state.toString() + " " + directionMessage() + " " + direction);
        changingState = false;
        Integer lockId;
        switch (state) {
            case MovingUp:
                lockId = tryToAccessData();
                if (stopNextFloor())
                    changeState(ElevatorState.DecDown);
                else {
                    sendMessage(false, true);
                    currFNo += direction;
                    changeState(state);
                }
                finishedAccessData(lockId);
                break;
            case MovingDown:
                lockId = tryToAccessData();
                if (stopNextFloor())
                    changeState(ElevatorState.DecUp);
                else {
                    sendMessage(false, true);
                    currFNo += direction;
                    changeState(state);
                }
                finishedAccessData(lockId);
                break;
            case DecDown:
            case DecUp:
                lockId = tryToAccessData();
                arriveFloor();
                changeState(ElevatorState.Open);
                finishedAccessData(lockId);
                break;
            case Open:
                changeState(ElevatorState.WaitForClose);
                break;
            case WaitForClose:
                changeState(ElevatorState.Close);
                break;
            case Close:
                lockId = tryToAccessData();
                decideNextMove();
                finishedAccessData(lockId);
                break;
            case AccUp:
                changeState(ElevatorState.MovingUp);
                break;
            case AccDown:
                changeState(ElevatorState.MovingDown);
                break;
        }
    }

    public boolean stopNextFloor() {
        if (DestFNo.size() > 0) {
            return (DestFNo.get(0) == currFNo + direction);
        } else {
            return true;
        }
    }

    public void arriveFloor() {
        canGetIn = true;
        currFNo += direction;

        DestFNo.remove(0);
        direction = directinoUpdate();
        passengerUpdate();
        sendMessage(false);
    }

    public int directinoUpdate() {
        if (DestFNo.size() > 0)
            return (int)Math.signum(DestFNo.get(0) - currFNo);
        else
            return 0;
    }

    public void decideNextMove() {
        if (direction == 0) {
            if (planedToOff) {
                changeState(ElevatorState.Off);
            } else
                changeState(ElevatorState.Idle);
        }else {
            canGetIn = false;
            sendMessage(true);
            if (direction > 0)
                changeState(ElevatorState.AccUp);
            else
                changeState(ElevatorState.AccDown);
        }
    }

    public Elevator(char eNo) {
        this(eNo, AppKickstarter._instance);
    }

    public Elevator(char eNo, AppKickstarter appKickstarter) {
        this.eNo = eNo;
        if (appKickstarter != null) {
            this.appKickstarter = appKickstarter;
            thread = new ElevatorThread("eleThread_" + eNo, appKickstarter, this);
            new Thread(thread).start();
        }
    }

    public Integer tryToAccessData() {
        long lastTime = System.currentTimeMillis();
        Integer myId = new Integer(queueIndexId ++);
        if (queueIndexId > 99999) queueIndexId = 0;
        accessingDataQuene.add(myId);
        while (!accessingDataQuene.get(0).equals(myId) && System.currentTimeMillis() - lastTime < timeOutTime) {
        }
        if (System.currentTimeMillis() - lastTime >= timeOutTime) {
            accessingDataQuene.remove(myId);
            appKickstarter.getLogger().warning("Ele "+eNo+": getting data time out");
            return -1;
        }
        return myId;
    }

    public void finishedAccessData(Integer myId) {
        if (myId < 0) return;
        accessingDataQuene.remove(myId);
    }

    public void turnOnElevator(boolean turnOn) {
        turnOnElevator(turnOn, false);
    }

    public void turnOnElevator(boolean turnOn, boolean forceChange) {
        if (forceChange) appKickstarter.getLogger().warning("Evelator " + eNo + ": Try to force change state");
        if (turnOn) {
            if (state == ElevatorState.Off || forceChange) {
                planedToOff = false;
                changeState(ElevatorState.Idle);
                appKickstarter.getThread("CentralControl").getMBox().send(new Msg(thread.getID(), thread.getMBox(), Msg.Type.SocketMsg, "Elev_start " + eNo));
            }
        } else {
            if (state == ElevatorState.Idle || forceChange) {
                changeState(ElevatorState.Off);
            } else {
                planedToOff = true;
            }
        }
    }

    public void addPassenger(String pID, int srcFNo, int dstFNo, int index1, int index2) {addPassenger(new Passenger(pID, srcFNo, dstFNo), index1, index2); }
    public void addPassenger(Passenger passenger,int index1,int index2) {
        appKickstarter.getLogger().fine(thread.getID() +" before: " + currFNo + " ");
        printDestF();
        if (index1 >= 0)
            addDestF(passenger.getSrcFNo(), index1);
        if (index2 >= 0)
            addDestF(passenger.getDstFNo(), index2);
        passengers.add(passenger);

        boolean isCurrentFloor = passengerUpdate() && currFNo == passenger.getSrcFNo();
        if (isCurrentFloor) {
//            DestFNo.add(0, passenger.getSrcFNo());
//            sendMessage(true);
//            DestFNo.remove(0);
            sendMessage(false);
        }

        if (state == ElevatorState.Idle && !changingState) {
            direction = directinoUpdate();
            if (isCurrentFloor)
                changeState(ElevatorState.Open);
            else
                decideNextMove();
        }
        appKickstarter.getLogger().fine(thread.getID() +" after: " +currFNo + " ");
        printDestF();
    }

    public void addDestF(int dstFNo, int index) {
        int sizeOfDestFNo = DestFNo.size();
        if (sizeOfDestFNo == 0)
            direction = (int)Math.signum(dstFNo - currFNo);
        if (index > sizeOfDestFNo)
            index = sizeOfDestFNo;
        DestFNo.add(index, dstFNo);
    }

    public boolean passengerUpdate() {
        List<Passenger> leavingPassenger = new ArrayList();
        AtomicBoolean hasUpdate = new AtomicBoolean(false);
        passengers.forEach(passenger -> {
            if (passenger.getRequiredFNo() == currFNo && canGetIn) {
                if (passenger.getState() == Passenger.PassengerState.Waitting) {
                    if (direction == 0 || direction == Math.signum(passenger.getDstFNo()-passenger.getSrcFNo())) {
                        passenger.enterElevator();
                        hasUpdate.set(true);
                    }
                } else {
                    hasUpdate.set(true);
                    leavingPassenger.add(passenger);
                }
            }
        });
        passengers.removeAll(leavingPassenger);
        return hasUpdate.get();
    }

    private void sendMessage(boolean isDep) {
        sendMessage(isDep, false);
    }

    private void sendMessage(boolean isDep, boolean updateToUI) {
        String msgText = isDep? "Elev_Dep ": "Elev_Arr ";
        msgText += eNo + " ";
        msgText += currFNo + " ";
        msgText += directionMessage();
        int i = 0;
        for (;i < DestFNo.size(); i ++)
            msgText += " " + DestFNo.get(i);
        if (i == 0)
            msgText += " " + Integer.MIN_VALUE;
        if (updateToUI)
            msgText += " updateToUI";
        appKickstarter.getThread("CentralControl").getMBox().send(new Msg(thread.getID(), thread.getMBox(), Msg.Type.SocketMsg, msgText));
    }

    public void printDestF() {
        String destFloors = "Elevator " + eNo + ": ";
        for (int i = 0; i < DestFNo.size(); i ++) {
            destFloors += DestFNo.get(i) + ", ";
        }
        appKickstarter.getLogger().fine(destFloors);
    }

    public void changeState(ElevatorState s) {
        if (changingState)
            appKickstarter.getLogger().finer(thread.getID() +": Im changing from " + state.toString() + " to " + s.toString());

        appKickstarter.getThread("CentralControl").getMBox().send(new Msg(thread.getID(), thread.getMBox(), Msg.Type.SocketMsg, "Elev_state " + eNo + " " + s.toString()));

        changingState = true;
        state = s;
        double time;
        switch (s) {
            case MovingDown:
                time =  ElevatorManager.DownOneFloor;
                break;
            case MovingUp:
                time =  ElevatorManager.UpOneFloor;
                break;
            case DecDown:
                time =  ElevatorManager.DecDown;
                break;
            case DecUp:
                time =  ElevatorManager.DecUp;
                break;
            case AccDown:
                time =  ElevatorManager.AccDown;
                break;
            case AccUp:
                time =  ElevatorManager.AccUp;
                break;
            case Open:
                time =  ElevatorManager.DoorOpen;
                break;
            case Close:
                time =  ElevatorManager.DoorClose;
                break;
            case WaitForClose:
                time =  ElevatorManager.DoorWait;
                break;
            case Off:
                DestFNo.clear();
                passengers.clear();
                time = -1;
                changingState = false;
                appKickstarter.getThread("CentralControl").getMBox().send(new Msg(thread.getID(), thread.getMBox(), Msg.Type.SocketMsg, "Elev_stop " + eNo));
                break;
            default:
                time =  -1;
                changingState = false;
        }

        if (time > 0)
            thread.waitForEnd(time);
    }

    public ElevatorState getState () {return state;}

    public boolean isCanGetIn() {
        return canGetIn;
    }

    boolean isMoreDestF() {return isMoreDestF(directionNum());}

    boolean isMoreDestF(int direction) {return nextDestF(direction) >= 0;}

    int nextDestF() {return nextDestF(directionNum());}

    int nextDestF(int direction) {
        if (direction > 0)
            for (int i = 0; i < DestFNo.size(); i ++) {
                int destFNo = DestFNo.get(i);
                if (destFNo > currFNo)
                    return destFNo;
            }
        else if (direction < 0)
            for (int i = DestFNo.size() - 1; i >= 0; i --) {
                int destFNo = DestFNo.get(i);
                if (destFNo  < currFNo)
                    return destFNo;
            }
        return -1;
    }

    public Elevator cloudElevatorData() {
        Elevator cloudObj = new Elevator(eNo, null);
        cloudObj.currFNo = currFNo;
        cloudObj.canGetIn = canGetIn;
        List<Integer> destFNoCloud = new ArrayList<>();
        for (int n = 0 ; n < DestFNo.size(); n ++)
            destFNoCloud.add(new Integer(DestFNo.get(n)));
        cloudObj.DestFNo = destFNoCloud;
        cloudObj.direction = direction;
        cloudObj.state = state;
        return cloudObj;
    }

    int directionNum() {
        return direction;
    }

    public boolean isMoving() {
        return (state == ElevatorState.MovingDown || state == ElevatorState.MovingUp || state == ElevatorState.AccDown || state == ElevatorState.AccDown || state == ElevatorState.DecDown || state == ElevatorState.DecUp);
    }

    public char directionMessage() {
        if (direction == 0)
            return 'S';
        else if (direction > 0)
            return 'U';
        else
            return 'D';
    }

    public void remove() {
        thread.getMBox().send(new Msg(thread.getID(), thread.getMBox(), Msg.Type.Terminate, "quit"));
    }

    public boolean isPlanedToOff() {return planedToOff;}
}




