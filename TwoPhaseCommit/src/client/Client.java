package client;

import shared.ServiceLogger;
import shared.StoreService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {
    private static ServiceLogger logger = new ServiceLogger();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Client [ServiceHost 1] [ServicePort1] ...");
            return;
        }
        List<String> hosts = new ArrayList<>();
        List<Integer> ports = new ArrayList<>();

        //list of participant hosts and ports
        try {
            for (int i = 0; i < args.length; i++) {
                if (i % 2 == 0) {
                    hosts.add(args[i]);
                } else {
                    ports.add(Integer.parseInt(args[i]));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Usage: java Client [ServiceHost 1] [ServicePort1] ...");
            return;
        }

        try {
            //random select server
            Random random = new Random();
            int pick = random.nextInt(hosts.size());

            // server registry
            Registry registry = LocateRegistry.getRegistry(hosts.get(pick), ports.get(pick));  // Change the port for different servers

            // Looking up the registry for the remote object
            StoreService stub = (StoreService) registry.lookup("StoreService");

            // Start the 5 GET, 5 PUT, 5 DELETE transaction
//            PutGetDeleteTest(stub);
            // Concur PUT test
            PutTest(stub);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    public static void PutTest(StoreService stub){
//        try{
//            String resp = stub.put("1","1");
//            logger.log(resp);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        try {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    String key = String.valueOf(i);
                    String value = String.valueOf(i);
                    try {
                        String resp = stub.put(key, value);
                        logger.log(resp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    String key = String.valueOf(i);
                    String value = String.valueOf(i);
                    try {
                        String resp = stub.put(key, value);
                        logger.log(resp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();
            System.out.println("Finished Put concur 2 threads.");
        }catch (InterruptedException e){
            System.out.println("Interrupted concur put req");
        }

    }
    public static void PutGetDeleteTest(StoreService stub){
        //5 Threads used for PUT,GET,DELETE
        ExecutorService pool = Executors.newFixedThreadPool(5);

        //Concurrently start 5 Put,GET,Delete
        for (int i = 0; i < 5; i++) {
            String key = String.valueOf(i);
            String value = String.valueOf(i);

            //PUT
            pool.submit(() -> {
                try {
                    String resp = stub.put(key, value);
                    logger.log(resp);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });

            //GET
            pool.submit(() -> {
                try {
                    String resp = stub.get(key);
                    logger.log(resp);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
            //DELETE
            pool.submit(()->{
                try{
                    String resp = stub.delete(key);
                    logger.log(resp);
                }catch(RemoteException e){
                    e.printStackTrace();
                }
            });
        }

        try {
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }catch (InterruptedException e){
            System.out.println("Interrupted");
        }
    }
}
