package paxosbase;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceLogger {
    Logger logger = Logger.getLogger(ServiceLogger.class.getName());
    public void log(String message){
        long systemTime = System.currentTimeMillis();
        logger.log(Level.INFO, "[Sys time:"+systemTime+"] "+message);
    }
}
