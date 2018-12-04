package ElevatorSystem;

public class Passenger {
    private String pID;
    private int srcFNo;
    private int dstFNo;
    private PassengerState state = PassengerState.Waitting;

    enum PassengerState {
        Waitting, InElevator
    }

    public Passenger(String pID, int srcFNo, int dstFNo){
        this.pID = pID;
        this.srcFNo = srcFNo;
        this.dstFNo = dstFNo;
    }

    public String getPID(){
        return pID;
    }
    public int getSrcFNo(){
        return srcFNo;
    }
    public int getDstFNo(){
        return dstFNo;
    }
    public PassengerState getState() { return state; }
    public int getRequiredFNo() {
        if (state == PassengerState.Waitting)
            return srcFNo;
        else if (state == PassengerState.InElevator)
            return dstFNo;
        else
            return -1;
    }

    public void changeState(PassengerState s) {state = s;}
    public void enterElevator() {changeState(PassengerState.InElevator);}

}
