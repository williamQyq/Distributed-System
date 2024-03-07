package server;

import java.io.IOException;
import java.util.logging.Level;

public class ServerApp {
    private static final ServerLogger logger = new ServerLogger(ServerApp.class.getName());

    public static void main(String[] args) {
        if (args.length < 2) {
            logger.log(Level.SEVERE, "Usage: java Server <Port number> <Protocol>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        String protocol = args[1];
        try {
            if (protocol.equalsIgnoreCase("TCP")) {
                Server tcpServer = new TCPHandler(port);
                tcpServer.run();    //run tcp server
            } else if (protocol.equalsIgnoreCase("UDP")) {
                Server udpServer = new UDPHandler(port);
                udpServer.run();    //run udp server
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}