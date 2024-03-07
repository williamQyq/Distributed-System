package client;

import shared.StoreService;

import java.rmi.RemoteException;

public class TwoPhaseCommitTest {
    public void testConcurPut(StoreService stub){
        try{
            Thread t1 = new Thread(stub.put("1","1"));
            Thread t2 = new Thread(stub.put("2","2"));

            t1.start();
            t2.start();

            t1.join();
            t2.join();

            String value1 = stub.get("1");
            String value2 = stub.get("2");

            System.out.println("value 1: "+ value1);
            System.out.println("value 2: "+ value2);

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
