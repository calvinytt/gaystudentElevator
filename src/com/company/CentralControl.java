package com.company;

import AppKickstarter.AppKickstarter;
import AppKickstarter.timer.Timer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class CentralControl {
    SocketManager serverManager;
    List<Integer> floor;

    public CentralControl(){
        floor = Arrays.asList(1, 2, 3, 4, 5);

        AppKickstarter appKickstarter = new AppKickstarter("AppKickstarter", "etc/MyApp.cfg");
        appKickstarter.startApp();

        Elevator ele = new Elevator('A', appKickstarter);
        ele.addDestF(3);
        ele.addDestF(6);
        ele.addDestF(9);
    }

    public static void main(String[] args) {
        new CentralControl();
        //Scanner s = new Scanner(new InputStream().in)
    }
}
