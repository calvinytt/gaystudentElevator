package AppKickstarter.myThreads;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.misc.MBox;
import AppKickstarter.misc.Msg;
import AppKickstarter.timer.Timer;
import com.company.Elevator;

public class ElevatorThread extends AppThread {

    private Elevator elevator;
    private static final int travelTime = 100;

    //------------------------------------------------------------
    // ThreadA
    public ElevatorThread(String id, AppKickstarter appKickstarter, Elevator elevator) {
        super(id, appKickstarter);
        this.elevator = elevator;
    } // ElevatorThread

    public void startTravel() {
        log.info(id + " " + elevator.eNo);
        Timer.setSimulationTimer(id, mbox, travelTime);
    }

    //------------------------------------------------------------
    // run
    public void run() {

        for (boolean quit = false; !quit;) {
            Msg msg = mbox.receive();

            log.info(id + ": message received: [" + msg + "].");

            switch (msg.getType()) {
                case TimesUp:

                    int currF = elevator.goNextFloor();
                    log.info(elevator.eNo + ": " + currF);

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

        // declaring our departure
        appKickstarter.unregThread(this);
        log.info(id + ": terminating...");
    } // run
}
