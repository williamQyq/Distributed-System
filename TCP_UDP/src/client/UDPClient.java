package client;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.logging.Level;

public class UDPClient {
    private ClientLogger logger = new ClientLogger(client.TCPClient.class.getName());
    private InetAddress hostIP;
    private int port;
    private Properties properties;

    public UDPClient(String[] args) {
        if (args.length < 2) {
            logger.log(Level.SEVERE, "Usage: java UDPClient <Host name IP> <Port number>");
            System.exit(1);
        }
        try (FileReader reader = new FileReader("./src/config.properties")) {
            properties = new Properties();
            properties.load(reader);

            hostIP = InetAddress.getByName(args[0]);
            port = Integer.parseInt(args[1]);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPutRequest(String key, String value) {
        logger.log(Level.INFO, "Sending PUT request...");
        DatagramSocket client = null;
        try {
            client = new DatagramSocket();
            String msg = encode(properties.getProperty("request.put"), key, value);
            byte[] buffer = msg.getBytes();
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, hostIP, port);
            client.send(request);
            ackFromServer(client);
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "Socket Exception: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        }
    }

    public void sendGetRequest(String key) {
        logger.log(Level.INFO, "Sending GET request...");
        DatagramSocket client = null;
        try {
            client = new DatagramSocket();
            String req = encode(properties.getProperty("request.get"), key);
            byte[] buffer = req.getBytes();
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, hostIP, port);
            client.send(request);
            ackFromServer(client);
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "Socket Exception: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        }
    }

    public void sendDeleteRequest(String key) {
        logger.log(Level.INFO, "Sending DELETE request...");
        DatagramSocket client = null;
        try {
            client = new DatagramSocket();
            String msg = encode(properties.getProperty("request.delete"), key);
            byte[] buffer = msg.getBytes();
            DatagramPacket req = new DatagramPacket(buffer, buffer.length, hostIP, port);
            client.send(req);
            ackFromServer(client);
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "Socket Exception: " + e);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        }
    }

    private String encode(String reqType, String key, String value) {
        return reqType + "|" + key + "|" + value;
    }

    private String encode(String reqType, String key) {
        return reqType + "|" + key;
    }

    private void ackFromServer(DatagramSocket client) throws IOException {
        try {
            client.setSoTimeout(Integer.parseInt(properties.getProperty("timeout")));
            byte[] ackBuffer = new byte[100];
            DatagramPacket reply = new DatagramPacket(ackBuffer, ackBuffer.length);
            client.receive(reply);
            logger.log(Level.INFO, "Ack from Server: " + new String(reply.getData(),0,reply.getLength()));
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "Request reached timeout.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        UDPClient client = null;
        client = new UDPClient(args);
        //5 puts
        client.sendPutRequest("1", "a");
        client.sendPutRequest("2", "b");
        client.sendPutRequest("3", "c");
        client.sendPutRequest("4", "d");
        client.sendPutRequest("5", "d");

        //5 gets
        client.sendGetRequest("1");
        client.sendGetRequest("2");
        client.sendGetRequest("3");
        client.sendGetRequest("4");
        client.sendGetRequest("6"); //test failure case

        //5 delete
        client.sendDeleteRequest("1");
        client.sendDeleteRequest("2");
        client.sendDeleteRequest("3");
        client.sendDeleteRequest("3");
        client.sendDeleteRequest("4");


    }
}
