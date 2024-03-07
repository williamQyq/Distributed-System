package client;

import com.sun.java.accessibility.util.TopLevelWindowListener;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.logging.Level;

public class TCPClient {
    private ClientLogger logger = new ClientLogger(TCPClient.class.getName());
    private String hostIP;
    private int port;
    private Properties properties;

    public TCPClient(String[] args) {
        if (args.length < 2) {
            logger.log(Level.SEVERE,"Usage: java TCPClient <Host name IP> <Port number>");
            System.exit(1);
        }
        try (FileReader reader = new FileReader("./src/config.properties")) {
            properties = new Properties();
            properties.load(reader);

            hostIP = args[0];
            port = Integer.parseInt(args[1]);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPutRequest(String key, String value) throws IOException {
        logger.log(Level.INFO, "Sending PUT request...");
        Socket client = null;
        try {
            client = new Socket(hostIP, port);
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            String req = encode(properties.getProperty("request.put"), key, value);
            dos.writeUTF(req);
            ackFromServer(client);
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        }
    }

    public void sendGetRequest(String key) throws IOException {
        logger.log(Level.INFO, "Sending GET request...");
        Socket client = null;
        try {
            client = new Socket(hostIP, port);
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            String req = encode(properties.getProperty("request.get"), key);
            dos.writeUTF(req);
            ackFromServer(client);
            dos.close();
        }catch(EOFException e){
          logger.log(Level.SEVERE,"Server Not responding, possible invalid request.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        }
    }

    public void sendDeleteRequest(String key) throws IOException {
        logger.log(Level.INFO, "Sending DELETE request...");
        Socket client = null;
        try {
            client = new Socket(hostIP, port);
            client.setSoTimeout(5000);
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            String req = encode(properties.getProperty("request.delete"), key);

            dos.writeUTF(req);
            ackFromServer(client);
            dos.close();
        }catch(EOFException e){
            logger.log(Level.SEVERE,"Server Not responding...");
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
    private String encode(String reqType, String key) {return reqType+"|"+key;}
    private void ackFromServer(Socket client) throws IOException {
        try {
            client.setSoTimeout(Integer.parseInt(properties.getProperty("timeout")));
            DataInputStream dis = new DataInputStream(client.getInputStream());
            String response = dis.readUTF();
            logger.log(Level.FINE, "Ack from Server: " + response);
        } catch (SocketTimeoutException e) {
                logger.log(Level.SEVERE, "Request reached timeout.");
        }
    }

    public static void main(String[] args) {

        try {
            TCPClient client = new TCPClient(args);

            //5 puts
            client.sendPutRequest("1","a");
            client.sendPutRequest("2","b");
            client.sendPutRequest("3","c");
            client.sendPutRequest("4","d");
            client.sendPutRequest("5","d");

            //5 gets
            client.sendGetRequest("1");
            client.sendGetRequest("1");
            client.sendGetRequest("1");
            client.sendGetRequest("1");
            client.sendGetRequest("1");

            //5 delete
            client.sendDeleteRequest("1");
            client.sendDeleteRequest("2");
            client.sendDeleteRequest("3");
            client.sendDeleteRequest("3");
            client.sendDeleteRequest("4");


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
