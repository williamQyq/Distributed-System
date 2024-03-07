package server;

import shared.ServiceLogger;
import shared.StoreService;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ParticipantImpl extends UnicastRemoteObject implements Participant, StoreService {

    private String transaction;
    private final ReentrantLock transactionLock = new ReentrantLock();
    private Coordinator coordinator;
    private boolean isLockAcquired; //can only be updated by reentrantLock.trylock()

    private Map<String, String> store = new ConcurrentHashMap<>();
    protected ServiceLogger logger = new ServiceLogger();
    public ParticipantImpl(String coordinatorHost, int coordinatorPort) throws RemoteException {
        super();

        //prepopulate data
        prepopulateStore();

        try {
            Registry registry = LocateRegistry.getRegistry(coordinatorHost, coordinatorPort);
            coordinator = (Coordinator) registry.lookup("Coordinator");
        } catch (Exception e) {
            throw new RemoteException("Unable to connect to coordinator", e);
        }
    }

    private void prepopulateStore(){
        Random random = new Random();

        for (int i = 0; i < 5; i++) {
            int rand = random.nextInt(50,100);
            String data = String.valueOf(rand);
            store.put(data, data);
        }
    }

    @Override
    public String get(String key) throws RemoteException {
        logger.log("GET received: "+ key);
        return store.get(key);
    }

    @Override
    public String put(String key, String value) throws RemoteException {
        String transaction = "put:" + key + ":" + value;
        logger.log("Start PUT transaction: " + transaction);
        boolean canCommit = coordinator.prepareTransaction(transaction);

        //notify coordinator to commit all or abort all
        if (canCommit && !store.containsKey(key)) {
            coordinator.commitTransaction();
            return "PUT committed: " + key + ": " + value;
        } else {
            coordinator.abortTransaction();
            return "PUT aborted: " + key + ": " + value;
        }
    }

    @Override
    public String delete(String key) throws RemoteException {
        String transaction = "delete:" + key;

        logger.log("Start DELETE transaction: "+transaction);
        //lock resources and prepare transaction
        boolean canCommit = coordinator.prepareTransaction(transaction);
        String ackToClient;

        //all commit or all abort
        if (canCommit && store.containsKey(key)) {
            coordinator.commitTransaction();
            ackToClient = "Delete transaction committed:" + key;
        } else {
            coordinator.abortTransaction();
            ackToClient = "Delete transaction aborted:" + key;
        }
        return ackToClient;
    }

    public boolean prepare(String transaction) throws RemoteException {
        //acquire the store lock
        boolean isOk = false;
        try {
            isLockAcquired = transactionLock.tryLock(10, TimeUnit.SECONDS);
            if (isLockAcquired) {
                long threadId = Thread.currentThread().threadId();
                logger.log("In Prepare phase, lock is granted by " + threadId + ": " + transaction);
                this.transaction = transaction; //prepare transaction after lock acquired
                isOk = true;  // "ok" to coordinator
            }
        } catch (InterruptedException e) {
            System.out.println("Thread is interrupted before acquiring the lock.");
        } finally {
            if(!isOk && isLockAcquired){
                transactionLock.unlock();
            }
        }
        return isOk;
    }

    public String commit() throws RemoteException {
        if (this.transaction != null) {
            String[] parts = this.transaction.split(":");

            //commit put or delete to store
            if (parts.length > 2) {
                String operation = parts[0];
                String key = parts[1];
                String value = parts.length == 3 ? parts[2] : null;

                //handle put, delete
                switch (operation) {
                    case "put":
                        store.putIfAbsent(key, value);
                        break;
                    case "delete":
                        store.remove(key);
                        break;
                    default:
                        System.out.println("Default operation commit");
                }
            }
        }
        if (transactionLock.isHeldByCurrentThread()) {
            transactionLock.unlock();
            long threadId = Thread.currentThread().threadId();
            logger.log("Unlock by thread: " + threadId + " for transaction: "+ this.transaction);
        } else {
           logger.log("Commit Error: Lock not being held by current thread.");
        }
        return "Committing: " + this.transaction;
    }

    public String abort() throws RemoteException {
        this.transaction = null;
        if (transactionLock.isHeldByCurrentThread()) {
            transactionLock.unlock();
        }
        logger.log("Abort transaction: "+ this.transaction);
        return "Abort transaction";
    }
}

