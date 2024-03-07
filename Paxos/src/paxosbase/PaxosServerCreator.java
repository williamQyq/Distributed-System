package paxosbase;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

/**
 * The PaxosServerCreator class is responsible for creating and binding the Paxos servers
 * within the RMI registry. It also configures the acceptors and learners for each server.
 */
public class PaxosServerCreator {
    private static ServiceLogger logger = new ServiceLogger();

    private static final int numServers = 5;
    private static final int basePort = 5000;

    //control monitor thread
    private static volatile boolean running = true;
    private static Random random = new Random();

    //random trigger server failure and restart
    private static boolean randomFailTrigger = false;

    private static void startNewServer(int serverId, Server[] servers, Thread[] serverThreads) throws RemoteException {
        int port = basePort + serverId;
        servers[serverId] = new Server(serverId, numServers);
        serverThreads[serverId] = new Thread(() -> {
            Registry registry;
            try {
                try {
                    // Bind the server to the RMI registry
                    registry = LocateRegistry.createRegistry(port);
                } catch (RemoteException e) {
                    registry = LocateRegistry.getRegistry(port);
                }
                registry.rebind("KVServer" + serverId, servers[serverId]);

                System.out.println("Server " + serverId + " is ready at port " + port);

                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(3000);
                }

                //unexport server
                UnicastRemoteObject.unexportObject(servers[serverId], true);
                registry.unbind("KVServer" + serverId);

            } catch (InterruptedException e) {
                logger.log("Simulate Server dead: " + serverId);

            } catch (Exception e) {
                logger.log("Server exception: " + e.toString());
                e.printStackTrace();
            }
        });

        //start server registry thread
        serverThreads[serverId].start();
    }

    /**
     * The main method to launch the creation and binding process of the Paxos servers.
     *
     * @param args Command-line arguments (unused in this context).
     */
    public static void main(String[] args) {
        try {

            Server[] servers = new Server[numServers];
            Thread[] serverThreads = new Thread[numServers];

            // Create server threads and bind servers registry;
            for (int serverId = 0; serverId < numServers; serverId++) {
                startNewServer(serverId, servers, serverThreads);
            }


            // Set acceptors and learners for each server
            for (int serverId = 0; serverId < numServers; serverId++) {
                AcceptorInterface[] acceptors = new AcceptorInterface[numServers];
                LearnerInterface[] learners = new LearnerInterface[numServers];
                for (int i = 0; i < numServers; i++) {
                    if (i != serverId) {
                        acceptors[i] = servers[i];
                        learners[i] = servers[i];
                    }
                }
                servers[serverId].setAcceptors(acceptors);
                servers[serverId].setLearners(learners);
            }

            //Restart server and serverThread if dead server detected
            Thread monitorAndRandomInterruptServerThread = new Thread(() -> {
                while (running) {
                    try {
                        int aliveThreadNum = numServers;
                        for (int serverId = 0; serverId < numServers; serverId++) {
                            if (!serverThreads[serverId].isAlive()) {
                                aliveThreadNum--;
                                logger.log("[Dead Server " + serverId + "] Restarting new server...");
                                startNewServer(serverId, servers, serverThreads);
                            }

                            //create random fail controlled by trigger
                            if (randomFailTrigger && servers[serverId].shouldRandomFail()) {
                                logger.log("Simulate a random server failure to serverID: " + serverId);
                                serverThreads[serverId].interrupt();
                            }
                        }

                        logger.log("Alive Thread Count: " + aliveThreadNum);

                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            monitorAndRandomInterruptServerThread.start();

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
