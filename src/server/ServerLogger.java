package server;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ServerLogger {
    private final Logger logger;

    public ServerLogger(String name) {
        logger = Logger.getLogger(name);
        logger.setUseParentHandlers(false);

        // Create a custom console handler
        ConsoleHandler consoleHandler = new ConsoleHandler() {
            @Override
            public void publish(LogRecord record) {
                record.setMessage("SystemTime: "+System.currentTimeMillis() + ": " + record.getMessage());
                super.publish(record);
            }
        };
        consoleHandler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);
    }

    public void log(Level level, String message) {
        logger.log(level, message);
    }

    // Other log methods (e.g., log warning, error, etc.) can be added here

    public static void main(String[] args) {
        ServerLogger customLogger = new ServerLogger("MyCustomLogger");

        customLogger.log(Level.FINE, "This is a log message with a timestamp.");
    }
}
