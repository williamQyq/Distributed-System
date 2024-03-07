package paxosbase;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Random;

public class Client {
    private KVStoreInterface server;
    private static final int basePort = 5000;
    private static final int numServers = 5;

    public Client(String host, int port,int serverId) throws Exception {
        String url = "rmi://" + host + ":" + port + "/KVServer" + serverId;
        server = (KVStoreInterface) Naming.lookup(url);
    }

    public void put(String key, String value) {
        try {
            server.put(key, value);
            System.out.println("PUT operation successful!");
        } catch (RemoteException e) {
            System.err.println("PUT operation failed: " + e.getMessage());
        }
    }

    public void delete(String key) {
        try {
            server.delete(key);
            System.out.println("DELETE operation successful!");
        } catch (RemoteException e) {
            System.err.println("DELETE operation failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        //random select server
        Random random = new Random();
        int serverId = random.nextInt(numServers);

        int port = basePort + serverId;
        try {
            Client client = new Client("localhost", port,serverId); // example host and port
            client.put("key1", "value1");
            client.delete("key2");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Client exception: " + e.toString());
        }
    }
}

