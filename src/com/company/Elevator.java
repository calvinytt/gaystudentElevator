package com.company;

import java.util.List;

public class Elevator {
    int currFNo;
    int eNo;
    private List<Integer> DestFNo;
    private ElevatorState state;
    private List<Passenger> passengers;

    public Elevator(int eNo) {
        this.eNo = eNo;
    }

    public void addPassenger(String pID, int srcFNo, int dstFNo) {
        passengers.add(new Passenger(pID, srcFNo, dstFNo));
    }

    public void changeState(ElevatorState s) {state = s;}


}



enum ElevatorState{
    Off, Idle, Open, GoingUp, GoingDown
}




