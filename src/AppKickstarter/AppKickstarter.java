package AppKickstarter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.Hashtable;

import AppKickstarter.timer.Timer;
import AppKickstarter.misc.*;


//======================================================================
// AppKickstarter
public class AppKickstarter {
	public static AppKickstarter _instance;
    private String cfgFName = null;
    private Properties cfgProps = null;
    private Hashtable<String, AppThread> appThreads = null;
    private String id = null;
    private Logger log = null;
    private ConsoleHandler logConHd = null;
    private FileHandler logFileHd = null;
    private Timer timer = null;
    private List<Thread> threads = new ArrayList();


    //------------------------------------------------------------
    // main
    public static void main(String [] args) {
	AppKickstarter appKickstarter = new AppKickstarter("AppKickstarter", "etc/MyApp.cfg");
	appKickstarter.startApp();
	try {
	    Thread.sleep(30 * 1000);
	} catch (Exception e) {}
	appKickstarter.stopApp();
    } // main


    //------------------------------------------------------------
    // AppKickstarter
    private AppKickstarter(String id) {
	this(id, "etc/MyApp.cfg");
    } // AppKickstarter


    //------------------------------------------------------------
    // AppKickstarter
    public AppKickstarter(String id, String cfgFName) {
	this(id, cfgFName, false);
    } // AppKickstarter


    //------------------------------------------------------------
    // AppKickstarter
    private AppKickstarter(String id, String cfgFName, boolean append) {
    	_instance = this;
	this.id = id;
	this.cfgFName = cfgFName;
	logConHd = null;
	logFileHd = null;
	id = getClass().getName();

	// set my thread name
	Thread.currentThread().setName(this.id);

	// read system config from property file
	try {
	    cfgProps = new Properties();
	    FileInputStream in = new FileInputStream(cfgFName);
	    cfgProps.load(in);
	    in.close();
	    logConHd = new ConsoleHandler();
	    logConHd.setFormatter(new LogFormatter());
	    logFileHd = new FileHandler("etc/" + id + ".log", append);
	    logFileHd.setFormatter(new LogFormatter());
	} catch (FileNotFoundException e) {
	    System.out.println("Failed to open config file ("+cfgFName+").");
	    System.exit(-1);
	} catch (IOException e) {
	    System.out.println("Error reading config file ("+cfgFName+").");
	    System.exit(-1);
	}

	// get and configure logger
	log = Logger.getLogger(id);
	log.addHandler(logConHd);
	log.addHandler(logFileHd);
	log.setUseParentHandlers(false);
	log.setLevel(Level.FINER);
	logConHd.setLevel(Level.INFO);
	logFileHd.setLevel(Level.INFO);
	appThreads = new Hashtable<String, AppThread>();
    } // AppKickstarter


    //------------------------------------------------------------
    // startApp
    public void startApp() {
	// start our application
	log.info("");
	log.info("");
	log.info("============================================================");
	log.info(id + ": Application Starting...");

	// create threads
	timer = new Timer("timer", this);

	// start threads
	new Thread(timer).start();
    } // startApp


    //------------------------------------------------------------
    // stopApp
    private void stopApp() {
	log.info("");
	log.info("");
	log.info("============================================================");
	log.info(id + ": Application Stopping...");
	//threadA1.getMBox().send(new Msg(id, null, Msg.Type.Terminate, "Terminate now!"));
	//threadA2.getMBox().send(new Msg(id, null, Msg.Type.Terminate, "Terminate now!"));
	//threadB.getMBox().send(new Msg(id, null, Msg.Type.Terminate, "Terminate now!"));
	timer.getMBox().send(new Msg(id, null, Msg.Type.Terminate, "Terminate now!"));

//	for (int i = 0; i < threads.size(); i ++) {
//		threads[i].getMBox().send(new Msg(id, null, Msg.Type.Terminate, "Terminate now!"));
//	}
    } // stopApp

	public void startAndRegTread(AppThread appThread) {
    	Thread newThread = new Thread(appThread);
		threads.add(newThread);
		newThread.start();
		regThread(appThread);
	}

    //------------------------------------------------------------
    // regThread
    public void regThread(AppThread appThread) {
	log.fine(id + ": registering " + appThread.getID());
	synchronized (appThreads) { appThreads.put(appThread.getID(), appThread); }
    } // regThread


    //------------------------------------------------------------
    // unregThread
    public void unregThread(AppThread appThread) {
	log.fine(id + ": unregistering " + appThread.getID());
	synchronized (appThreads) { appThreads.remove(appThread.getID()); }
    } // unregThread


    //------------------------------------------------------------
    // getThread
    public AppThread getThread(String id) {
	synchronized (appThreads) { return appThreads.get(id); }
    } // getThread


    //------------------------------------------------------------
    // getLogger
    public Logger getLogger() {
	return log;
    } // getLogger


    //------------------------------------------------------------
    // getLogConHd
    public ConsoleHandler getLogConHd() {
	return logConHd;
    }
    // getLogConHd


    //------------------------------------------------------------
    // getLogFileHd
    public FileHandler getLogFileHd() {
	return logFileHd;
    } // getLogFileHd


    //------------------------------------------------------------
    // getProperty
    public String getProperty(String property) {
	String s = cfgProps.getProperty(property);

	if (s == null) {
	    log.severe(id + ": getProperty(" + property + ") failed.  Check the config file (" + cfgFName + ")!");
	}
	return s;
    } // getProperty


    //------------------------------------------------------------
    // getSimulationTime (in seconds)
    public long getSimulationTime() {
	return timer.getSimulationTime();
    } // getSimulationTime


    //------------------------------------------------------------
    // getSimulationTimeStr
    public String getSimulationTimeStr() {
	long t = timer.getSimulationTime();
	int s = (int) t % 60;
	int m = (int) (t/60) % 60;
	int h = (int) (t/3600) % 60;

	return String.format("%02d:%02d:%02d", h, m, s);
    } // getSimulationTimeStr
} // AppKickstarter
