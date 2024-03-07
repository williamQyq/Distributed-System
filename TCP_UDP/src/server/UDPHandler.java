package server;

import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

public class UDPHandler implements Server {
    public int port;
    private ServerLogger logger;
    private Map<String, String> store;
    private Properties properties;

    public UDPHandler(int port) {
        try (FileReader reader = new FileReader("./src/config.properties")) {
            properties = new Properties();
            properties.load(reader);
            this.port = port;
            this.logger = new ServerLogger(UDPHandler.class.getName());
            this.store = new HashMap<String, String>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() throws IOException {
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket(port);
            logger.log(Level.INFO, "UDP Server Running " + ds.getLocalAddress() + " on port " + ds.getLocalPort() + "...");
            byte[] buffer = new byte[100];
            //listening to client connection and handle requests
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                ds.receive(packet);
                String clientRequest = new String(packet.getData(),0,packet.getLength());
                logger.log(Level.FINE, "Receive message: " + clientRequest);

                String[] parts = decode(clientRequest);
                logger.log(Level.FINE, "Decode msg parts: " + Arrays.toString(parts));

                String requestType = "";
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    requestType = parts[0];
                }

                switch (requestType) {
                    case "PUT":
                        handlePutRequest(parts, ds, packet);
                        break;
                    case "GET":
                        handleGetRequest(parts, ds, packet);
                        break;
                    case "DELETE":
                        handleDeleteRequest(parts, ds, packet);
                        break;
                    default:
                        //handle exception
                        logger.log(Level.SEVERE, "Received a malformed request of length: " + packet.getLength() + " from "
                                + packet.getAddress() + ": " + packet.getPort());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handlePutRequest(String[] parts, DatagramSocket client, DatagramPacket packet) {
        logger.log(Level.INFO, "From Client " + packet.getAddress() + " Received \"PUT\"");
        try {
            if (parts.length == 3) {
                String key = parts[1];
                String value = parts[2];
                store.put(key, value);
                ackToClient(properties.getProperty("request.put"), key, client,packet);
            } else {
                String failureMsg = "received malformed request of length: " +
                 packet.getLength() + " from " +packet.getAddress() +": "+packet.getPort();
                ackFailureToClient(failureMsg,client,packet);
                logger.log(Level.SEVERE, "PUT Fail, " + "invalid decode msg parts");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "PUT Fail, " + client.getInetAddress());
        }
    }

    private void handleGetRequest(String[] parts, DatagramSocket client, DatagramPacket packet) {
        logger.log(Level.INFO, "From Client " + packet.getAddress() + " Received \"GET\"");
        try {
            String key = parts[1];
            if (store.containsKey(key)) {
                String value = store.get(key);
                ackToClient(properties.getProperty("request.get"), key, client,packet);
            } else {
                String failureMsg = "received malformed request of length: " +
                        packet.getLength() + " from " +packet.getAddress() +": "+packet.getPort();
                ackFailureToClient(failureMsg,client,packet);
                logger.log(Level.SEVERE, "GET Fail " + "Key does not exist: " + key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteRequest(String[] parts, DatagramSocket client,DatagramPacket packet) {
        logger.log(Level.INFO, "From Client " + packet.getAddress() + " Received \"DELETE\"");
        try {
            String key = parts[1];
            if (store.containsKey(key)) {
                store.remove(key);
                ackToClient(properties.getProperty("request.delete"), key, client,packet);
            } else {
                String errMsg = "DELETE Fail, Delete Key does not exist: " + key;
                ackFailureToClient(errMsg, client,packet);
                logger.log(Level.SEVERE, errMsg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ackToClient(String reqType, String key, DatagramSocket client,DatagramPacket packet) throws IOException {
        logger.log(Level.INFO, "Acknowledging to the client...");
        byte[] ackBuffer = new byte[1000];

        byte[] buffer = ("Ack - handled request: " + reqType + " key:" + key).getBytes();
        DatagramPacket reply = new DatagramPacket(buffer,buffer.length,packet.getAddress(), packet.getPort());
        client.send(reply);
    }

    private void ackFailureToClient(String failureMsg, DatagramSocket client, DatagramPacket packet) throws IOException {
        logger.log(Level.SEVERE, "Acknowledging Failure to the client...");
        byte[] ackBuffer = new byte[1000];

        byte[] buffer = failureMsg.getBytes();
        DatagramPacket reply = new DatagramPacket(buffer,buffer.length,packet.getAddress(), packet.getPort());
        client.send(reply);
    }

    private String[] decode(String encodeMsg) {
        return encodeMsg.split("\\|");
    }
}