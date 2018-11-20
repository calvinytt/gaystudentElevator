package com.company;

import AppKickstarter.AppKickstarter;
import AppKickstarter.myThreads.ElevatorThread;

import java.util.ArrayList;
import java.util.List;

public class Elevator {

    int currFNo = 0;
    public char eNo;
    private List<Integer> DestFNo = new ArrayList<>();
    private ElevatorState state = ElevatorState.Idle;
    private List<Passenger> passengers;

    private ElevatorThread thread;

    public Elevator(char eNo, AppKickstarter appKickstarter) {
        this.eNo = eNo;
        thread = new ElevatorThread("eleThread_"+eNo, appKickstarter, this);
        new Thread(thread).start();
    }

    public void addPassenger(String pID, int srcFNo, int dstFNo) {
        addDestF(dstFNo);
        passengers.add(new Passenger(pID, srcFNo, dstFNo));
    }

    public void changeState(ElevatorState s) {
        if (state == ElevatorState.Idle && (s == ElevatorState.GoingDown || s == ElevatorState.GoingUp)) {
            thread.startTravel();
        }
        state = s;
    }

    public void removePasenger(String pID) {
        //remove the passenger in <passengers> and destFNo
    }

    public void addDestF(int dstFNo) {
        if (dstFNo == currFNo) return;

        DestFNo.add(dstFNo);
        if (state == ElevatorState.Idle) {
            if (dstFNo > currFNo)
                changeState(ElevatorState.GoingUp);
            else
                changeState(ElevatorState.GoingDown);
        }
    }

    public int goNextFloor() {
        int direction = directionNum();

        if (direction != 0 ) {
            currFNo += direction;

            if (!isMoreDestF(direction)) {
                if (isMoreDestF(direction * -1))
                    changeState(oppositeDirection(state));
                else
                    changeState(ElevatorState.Idle);
            }

            if (state == ElevatorState.GoingDown || state == ElevatorState.GoingUp)
                thread.startTravel();

            DestFNo.remove(new Integer(currFNo));
        }

        if (currFNo == 9) { // debug
            this.addDestF(4);
            this.addDestF(0);
            this.addDestF(10);
        }

        return currFNo;
    }

    boolean isMoreDestF(int direction) {
        for (int i = 0; i < DestFNo.size(); i ++) {
            if (DestFNo.get(i) * direction > currFNo * direction)
                return true;
        }
        return false;
    }

    static ElevatorState oppositeDirection(ElevatorState s) {
        if (s == ElevatorState.GoingUp)
            return ElevatorState.GoingDown;
        else if (s == ElevatorState.GoingDown)
            return ElevatorState.GoingUp;
        else
            return ElevatorState.Idle;
    }

    int directionNum() {
        if (state == ElevatorState.GoingDown)
            return -1;
        else if (state == ElevatorState.GoingUp)
            return 1;
        else
            return 0;
    }

    public char directionMessage() {
        if (state == ElevatorState.GoingDown)
            return 'D';
        else if (state == ElevatorState.GoingUp)
            return 'U';
        else
            return 'S';
    }
}



enum ElevatorState{
    Off, Idle, Open, GoingUp, GoingDown
}




