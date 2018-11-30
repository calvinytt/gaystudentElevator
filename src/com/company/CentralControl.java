package com.company;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.misc.Msg;
import AppKickstarter.timer.Timer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class CentralControl extends AppThread {
    SocketManager serverManager;
    List<Integer> floor;



    public CentralControl(AppKickstarter appKickstarter){
        super("center", appKickstarter);

        floor = Arrays.asList(1, 2, 3, 4, 5);
        appKickstarter.startApp();

        Elevator ele = new Elevator('A', appKickstarter);
        ele.addDestF(3);
        ele.addDestF(6);
        ele.addDestF(9);
    }

    public static void main(String[] args) {
        AppKickstarter appKickstarter = new AppKickstarter("AppKickstarter", "etc/MyApp.cfg");
        new CentralControl(appKickstarter);
        //Scanner s = new Scanner(new InputStream().in)
    }

    public void run() {

        for (boolean quit = false; !quit; ) {
            Msg msg = mbox.receive();

            log.info(id + ": message received: [" + msg + "].");

            switch (msg.getType()) {
                case TimesUp:

                    //send message
//                    AppThread thdB = appKickstarter.getThread("ThreadB");
//                    MBox thdBMBox = thdB.getMBox();
//                    thdBMBox.send(new Msg(id, mbox, Msg.Type.Hello, "Hello, this is Thread A!  (mCnt: " + ++mCnt + ")"));

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
}
