package client;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ClientLogger {
    private final Logger logger;

    public ClientLogger(String name) {
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
        ClientLogger customLogger = new ClientLogger("MyCustomLogger");

        customLogger.log(Level.FINE, "This is a log message with a timestamp.");
    }
}
