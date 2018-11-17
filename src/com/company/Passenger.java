package com.company;

public class Passenger {
    private String pID;
    private int srcFNo;
    private int dstFNo;

    Passenger(String pID, int srcFNo, int dstFNo){
        pID = this.pID;
        srcFNo = this.srcFNo;
        dstFNo = this.dstFNo;

    }

    public  String getPID(){
        return pID;
    }
    public  int getDstFNo(){
        return dstFNo;
    }

}
