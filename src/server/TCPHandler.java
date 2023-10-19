package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

public class TCPHandler implements Server{
    public int port;
    public ServerLogger logger;
    private Map<String, String> store;
    private Properties properties;
    public TCPHandler(int port) {
        try (FileReader reader = new FileReader("./src/config.properties")) {
            this.port = port;
            properties = new Properties();
            properties.load(reader);

            this.logger = new ServerLogger(TCPHandler.class.getName());
            this.store = new HashMap<String, String>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() throws IOException {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            logger.log(Level.INFO, "TCP Server Running " + ss.getInetAddress() + " on port " + ss.getLocalPort() + "...");

            //listening to client connection and handle requests
            while (true) {
                Socket clientSocket = ss.accept();
                clientSocket.setSoTimeout(Integer.parseInt(properties.getProperty("timeout")));
                try {
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    String clientRequest = dis.readUTF();

                    String[] parts = decode(clientRequest);
                    logger.log(Level.FINE, "Decode msg parts: " + Arrays.toString(parts));

                    String requestType = "";
                    if (parts.length > 0 && !parts[0].isEmpty()) {
                        requestType = parts[0];
                    }

                    switch (requestType) {
                        case "PUT":
                            handlePutRequest(parts, clientSocket);
                            break;
                        case "GET":
                            handleGetRequest(parts, clientSocket);
                            break;
                        case "DELETE":
                            handleDeleteRequest(parts, clientSocket);
                            break;
                        default:
                            //handle exception
                            logger.log(Level.SEVERE, "Received malformed Client Request from"
                                    + "<" + clientSocket.getInetAddress() + ">" + ":" + clientSocket.getPort());
                    }
                    dis.close();
                } catch (SocketTimeoutException e) {
                    logger.log(Level.SEVERE, "Socket read from client timeout.");

                } finally {
                    if (!clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void handlePutRequest(String[] parts, Socket client) {
        logger.log(Level.INFO, "From Client " + client.getInetAddress() + " Received \"PUT\"");
        try {
            if (parts.length == 3) {
                String key = parts[1];
                String value = parts[2];
                store.put(key, value);
                ackToClient(properties.getProperty("request.put"), key,"", client);
            } else {
                logger.log(Level.SEVERE, "PUT Fail, "+"invalid decode msg parts");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "PUT Fail, " + client.getInetAddress());
        }
    }

    private void handleGetRequest(String[] parts, Socket client) {
        logger.log(Level.INFO, "From Client " + client.getInetAddress() + " Received \"GET\"");
        try {
            String key = parts[1];
            if (store.containsKey(key)) {
                String value = store.get(key);
                ackToClient(properties.getProperty("request.get"), key,value, client);
            } else {
                msgToClient("Key does not exist: " + key,client);
                logger.log(Level.SEVERE, "GET Fail " +"Key does not exist: " + key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteRequest(String[] parts, Socket client) {
        logger.log(Level.INFO, "From Client " + client.getInetAddress() + " Received \"DELETE\"");
        try {
            String key = parts[1];
            if (store.containsKey(key)) {
                store.remove(key);
                ackToClient(properties.getProperty("request.delete"), key,"", client);
            } else {
                String errMsg = "DELETE Fail, Delete Key does not exist: " + key;
                msgToClient(errMsg,client);
                logger.log(Level.SEVERE, errMsg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ackToClient(String reqType, String key,String value, Socket client) throws IOException {
        logger.log(Level.INFO, "Acknowledging to the client...");
        DataOutputStream dos = new DataOutputStream(client.getOutputStream());
        if(reqType.equals(properties.getProperty("request.get"))){
            dos.writeUTF("Ack - handled request: " + reqType + " key:" + key + " value:"+value);
        }else{
            dos.writeUTF("Ack - handled request: " + reqType + " key:" + key);
        }
        dos.close();
    }

    private void msgToClient(String msg, Socket client) throws IOException {
        logger.log(Level.INFO, "Sending Msg to Client...");
        DataOutputStream dos = new DataOutputStream(client.getOutputStream());
        dos.writeUTF(msg);
        dos.close();
    }

    private String[] decode(String encodeMsg) {
        return encodeMsg.split("\\|");
    }
}
