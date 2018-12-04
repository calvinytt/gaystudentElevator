package AppKickstarter.myThreads;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.AppThread;
import AppKickstarter.misc.Msg;
import AppKickstarter.timer.Timer;
import ElevatorSystem.Elevator;

public class ElevatorThread extends AppThread {

    private Elevator elevator;

    //------------------------------------------------------------
    // ElevatorThread
    public ElevatorThread(String id, AppKickstarter appKickstarter, Elevator elevator) {
        super(id, appKickstarter);
        this.elevator = elevator;
    } // ElevatorThread

    public void waitForEnd(double time) {
        Timer.setSimulationTimer(id, mbox, time);
    }

    //------------------------------------------------------------
    // run
    public void run() {

        for (boolean quit = false; !quit;) {
            Msg msg = mbox.receive();

            switch (msg.getType()) {
                case TimesUp:

                    elevator.stateFinish();

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
