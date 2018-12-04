package ElevatorSystem;

import AppKickstarter.AppKickstarter;

import java.util.ArrayList;
import java.util.List;

/**
 * Elevator Manager which can create, update and remove elevator.
 * It will handle all of queue algorithm for assigning elevator to passenger.
 * Also read config property and store the time of all state action.
 */
public class ElevatorManager {
    public static double UpOneFloor =  0.6;
    public static double DownOneFloor =  0.5;
    public static double AccUp=  1.2;
    public static double AccDown=1;
    public static double DecUp=  1.2;
    public static double DecDown=1;
    public static double DoorOpen=1;
    public static double DoorClose= 1.5;
    public static double DoorWait=5;

    /**
     * Unique Object of self
     */
    public static ElevatorManager _instance;

    /**
     * List of elevator object
     */
    private List<Elevator> elevatorList = new ArrayList();
    /**
     * Used to assign id to elevator - automatically add one when assigning id to an elevator
     */
    private int idIndex = 0;
    /**
     * AppKickstarter reference
     */
    private AppKickstarter appKickstarter;

    /**
     * Create elevator manager and number of elevators
     * @param appKickstarter pass the appKickstarter object to this
     * @param eleNum the number of elevator
     */
    public ElevatorManager(AppKickstarter appKickstarter, int eleNum) {
        UpOneFloor = Double.parseDouble(AppKickstarter._instance.getProperty("Elev.Time.UpOneFloor"));
        DownOneFloor = Double.parseDouble(AppKickstarter._instance.getProperty("Elev.Time.DownOneFloor"));
        AccUp = Double.parseDouble(AppKickstarter._instance.getProperty("Elev.Time.AccUp"));
        AccDown = Double.parseDouble(AppKickstarter._instance.getProperty("Elev.Time.AccDown"));
        DecUp = Double.parseDouble(AppKickstarter._instance.getProperty("Elev.Time.DecUp"));
        DecDown = Double.parseDouble(AppKickstarter._instance.getProperty("Elev.Time.DecDown"));
        DoorOpen = Double.parseDouble(AppKickstarter._instance.getProperty("Elev.Time.DoorOpen"));
        DoorClose = Double.parseDouble(AppKickstarter._instance.getProperty("Elev.Time.DoorClose"));
        DoorWait = Double.parseDouble(AppKickstarter._instance.getProperty("Elev.Time.DoorWait"));

        this.appKickstarter = appKickstarter;
        _instance = this;
        createElevator(eleNum);
    }

    public void createElevator() {
        createElevator(1);
    }

    public void createElevator(int num) {
        for (int i = 0; i < num; i ++) {
            elevatorList.add(idIndex, new Elevator((char)(idIndex+65)));
            idIndex ++;
        }
    }

    public void powerElevator(int num, boolean power) {
        System.out.println(num  + " " + power);
        if (num < elevatorList.size() && num >= 0)
            elevatorList.get(num).turnOnElevator(power);
    }

    public void restart() {
        int numberOfEle = elevatorList.size();
        removeAllElevator();
        createElevator(numberOfEle);
    }

    public void removeAllElevator() {
        idIndex = 0;
        for (int i = 0; i < elevatorList.size(); i ++) {
            elevatorList.get(i).remove();
        }
        elevatorList.clear();
    }

    public void addPassenger(String message, String pID, int srcFNo, int dstFNo) {
        addPassenger(message,pID,dstFNo ,srcFNo,SocketManager.ClientType.PassengerStream);
    }

    public void addPassenger(String message, String pID, int srcFNo, int dstFNo , SocketManager.ClientType clientType) {
        int targetDirection = (int)Math.signum(dstFNo - srcFNo);

        double minTimeCost = Integer.MAX_VALUE;
        int eleIndex = -1;
        int numOfElevator = elevatorList.size();
        int index1 = -1, index2 = -1;

        for (int i = 0; i < numOfElevator; i ++) {
            Elevator eleObj = elevatorList.get(i);
            if (eleObj.isPlanedToOff())
                continue;
//            Integer lockId = eleObj.tryToAccessData();//try to lock and access the data of elevator
//            if (lockId < 0)
//                appKickstarter.getLogger().warning("Elevator " + eleObj.eNo + ": Cannot lock and access the data");

            Elevator cloudEleObj = eleObj.cloudElevatorData();

            //eleObj.finishedAccessData(lockId);

            if (cloudEleObj.getState() == Elevator.ElevatorState.Off) {
                appKickstarter.getLogger().fine("\tEle " + cloudEleObj.eNo + ": is out off service");
                return;
            }
            int[] results = elevatorTimeCost(cloudEleObj, srcFNo, dstFNo, targetDirection); //get the elevator score for passenger
            if (results == null)
                System.out.println(cloudEleObj.eNo +" NULL");
            appKickstarter.getLogger().fine("\tEle " + cloudEleObj.eNo + ": " + results[0]);
            if (results[0] < minTimeCost) { //choose the elevator with minimum score
                minTimeCost = results[0];
                eleIndex = results[3];
                index1 = results[1];
                index2 = results[2];
            }
        }

        if (eleIndex >= 0 && eleIndex < elevatorList.size()) {
            Elevator assignedElevator =  elevatorList.get(eleIndex);
            Integer lockId = assignedElevator.tryToAccessData();//try to lock and access the data of elevator
            if (lockId < 0)
                appKickstarter.getLogger().warning("Elevator " + assignedElevator.eNo + ": Cannot lock and access the data");

            CentralControl._instance.replyElevator(message + assignedElevator.eNo, clientType);
            assignedElevator.addPassenger(pID, srcFNo, dstFNo, index1, index2);

            assignedElevator.finishedAccessData(lockId);
        } else {
            appKickstarter.getLogger().warning("Cannot find elevator: " + (char)(eleIndex+65));
        }
    }

    //returns: time, srcFNoIndex, dstFNoIndex
    private int[] elevatorTimeCost(Elevator eleObj, int srcFNo, int dstFNo, int targetDirection) {
        List<Integer> destFNoList = eleObj.DestFNo;
        int sizeOfDestFNo = destFNoList.size();

        int[] results = new int[4]; //time, srcFNoIndex, dstFNoIndex
        results[3] = (int)eleObj.eNo - 65;

        //no destFloor case
        if (sizeOfDestFNo==0) {
            int currToSrcDriection = (int)Math.signum(srcFNo - eleObj.currFNo);
            results[0] = Math.abs(srcFNo-eleObj.currFNo)*oneFloorTime(currToSrcDriection) + Math.abs(dstFNo-srcFNo) * oneFloorTime(targetDirection);
            results[0] += startLiftTime(targetDirection) + stopLiftTime(targetDirection) * 2;
            if (srcFNo == eleObj.currFNo)
                results[1] = -1;
            else
                results[1] = 0;
            results[2] = 1;
            return results;
        }

        results[1] = -1000;
        results[2] = -1000;

        int[] targetFNo = new int[2];
        targetFNo[0] = srcFNo;
        targetFNo[1] = dstFNo;
        int targetIndex = 0;
        int lastFloor = eleObj.currFNo;
        int lastDirection = eleObj.directionNum();
        if (lastDirection == 0);
        int timeCost = 0;

        boolean debug = true;

        if (targetFNo[0] == lastFloor && eleObj.isCanGetIn() && lastDirection == targetDirection) {
            results[targetIndex+1] = -1;
            targetIndex ++;
            appKickstarter.getLogger().finer(targetDirection + "\t" + lastFloor + "\t" + "Same First Floor");
        }

        for (int i = 0; i < sizeOfDestFNo; i ++) { //loop all
            int targetFloor = targetFNo[targetIndex];
            int nextFloor = destFNoList.get(i);
            int distance = Math.abs(nextFloor - lastFloor);
            int currDirection = (int) Math.signum(nextFloor - lastFloor);
            if (debug) appKickstarter.getLogger().finer(currDirection + "\t" + lastFloor + "\t" + targetFloor + "\t" + nextFloor + "\t");
            if (targetFloor == nextFloor && targetDirection == currDirection || currDirection != lastDirection && lastFloor == targetFloor) {
                // same floor and same direction
                results[targetIndex+1] = -10;
                targetIndex ++;
                lastFloor = nextFloor;
                i --;
                appKickstarter.getLogger().fine("Same");
            } else if (false && lastDirection != currDirection && targetFloor * lastDirection > lastFloor * lastDirection) {
                //schedule direction change and the target floor is more far
                currDirection = lastDirection;
                distance = Math.abs(targetFloor - lastFloor);
                lastFloor = targetFNo[targetIndex];
                results[targetIndex+1] = i;
                i --;
                targetIndex ++;
                appKickstarter.getLogger().fine("different");
            } else if (targetFloor * currDirection > lastFloor * currDirection && targetFloor * currDirection < nextFloor * currDirection
                    && targetDirection == currDirection) {
                //between the last floor and next floor in same direction
                distance = Math.abs(targetFloor - lastFloor);
                lastFloor = targetFNo[targetIndex];
                results[targetIndex+1] = i;
                i --;
                targetIndex ++;
                appKickstarter.getLogger().fine("inside");
            } else {
                if (targetIndex > 0 && (currDirection != lastDirection)) {
                    timeCost -= DoorWait;
                    timeCost -= startLiftTime(targetDirection);
                    timeCost -= stopLiftTime(targetDirection);
                    targetIndex --;
                    results[targetIndex+1] = -1000;
                }
                //none of above, gonna check next floor in schedule
                lastFloor = nextFloor;
                appKickstarter.getLogger().fine("Skip");
            }

            //time cost add
            if (timeCost>0) {
                timeCost += DoorWait;
                timeCost += startLiftTime(currDirection);
            }
            timeCost += distance * oneFloorTime(currDirection);
            timeCost += stopLiftTime(currDirection);

            if (targetIndex>1) break; //break when both target is assigned position of schedule
            lastDirection = currDirection;
        }

        if (results[1] <= -1000) {
            if (lastFloor == targetFNo[0]) {
                results[1] = -10;
                appKickstarter.getLogger().fine("Same Last Floor");
            } else {
                results[1] = sizeOfDestFNo;
                appKickstarter.getLogger().fine("Unset srcFNo: push to tail");
                timeCost += DoorWait;
                timeCost += startLiftTime(targetDirection);
                timeCost += Math.abs(lastFloor - srcFNo) * oneFloorTime(targetDirection);
                lastFloor = srcFNo;
            }
        }
        if (results[2] <= -1000) {
            results[2] = sizeOfDestFNo;
            appKickstarter.getLogger().fine("unset detFNo: push to tail");
            timeCost += Math.abs(lastFloor - dstFNo) * oneFloorTime(targetDirection);
        }

//        if (eleObj.directionNum() != targetDirection)
//            timeCost*=1.5;

        if (results[1] >= 0){
            results[2]++; //shift position when src floor is assigned position
        }

        results[0] = timeCost;

        return results;
    }

    private int oneFloorTime(int direction) {
        return (int) ((direction>0?UpOneFloor:DownOneFloor)*100);
    }
    private int stopLiftTime(int direction) {
        return (int) ((direction>0?DecDown+DoorOpen:DecUp+DoorOpen)*100);
    }
    private int startLiftTime(int direction) {
        return (int) ((direction>0?AccUp+DoorClose:AccDown+DoorClose)*100);
    }
    public int getEleNum() {return elevatorList.size();}
}
