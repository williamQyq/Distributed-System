package paxosbase;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of a Server class that represents a node in a Paxos distributed consensus system.
 * This server plays the role of Proposer, Acceptor, and Learner in the Paxos algorithm, and it also handles key-value store operations.
 */
public class Server extends UnicastRemoteObject implements ProposerInterface, AcceptorInterface, LearnerInterface, KVStoreInterface {
    private ConcurrentHashMap<String, String> kvStore = new ConcurrentHashMap<>();
    private ServiceLogger logger = new ServiceLogger();
    private AcceptorInterface[] acceptors;
    private LearnerInterface[] learners;
    private int numServers;
    private int serverId;

    private int highestProposeId = -1;
    private int proposalSequenceNum = 0;

    private Object lastProposalValue;

    private Random random = new Random();
    private boolean randomFailTrigger = true;
    /**
     * Constructor to create a Server instance.
     *
     * @param serverId   The unique ID of this server.
     * @param numServers The total number of servers in the system.
     */
    public Server(int serverId, int numServers) throws RemoteException {
        super();
        this.numServers = numServers;
        this.serverId = serverId;
    }

    /**
     * Set the acceptors for this server.
     *
     * @param acceptors Array of acceptors.
     */
    public void setAcceptors(AcceptorInterface[] acceptors) {
        this.acceptors = acceptors;
    }

    /**
     * Set the learners for this server.
     *
     * @param learners Array of learners.
     */
    public void setLearners(LearnerInterface[] learners) {
        this.learners = learners;
    }

    public String get(String key) throws RemoteException {
        return applyOperation(new Operation("GET", key));
    }

    @Override
    public synchronized void put(String key, String value) throws RemoteException {
        proposeOperation(new Operation("PUT", key, value));
    }

    @Override
    public synchronized void delete(String key) throws RemoteException {
        proposeOperation(new Operation("DELETE", key, null));
    }

    /**
     * Propose an operation to be applied.
     *
     * @param operation The operation to be proposed.
     * @throws RemoteException If a remote error occurs.
     */
    private void proposeOperation(Operation operation) throws RemoteException {
        int proposalId = generateProposalId();
        logger.log("Propose Operation begin: "+ proposalId + ": "+operation.type);

        propose(proposalId, operation);
    }

    /**
     * Prepare the proposal associated with the proposalId
     *
     * @param proposalId The unique ID of the proposal.
     * @return highestProposeId The highest proposal id.
     * @throws RemoteException
     */
    @Override
    public synchronized int prepare(int proposalId) throws RemoteException {
        if(randomFailTrigger && shouldRandomFail()){
            throw new RemoteException("["+serverId+"]"+"Acceptor random failure.");
        }
        // Implement Paxos prepare logic here
        if (proposalId > highestProposeId) {
            highestProposeId = proposalId;
            return highestProposeId;
        }
        return -1;
    }

    /**
     * Accept proposalValue associated with the proposalId that is the highest ever seen.
     *
     * @param proposalId    The unique ID of the proposal.
     * @param proposalValue The value of the proposal.
     * @return
     * @throws RemoteException
     */
    @Override
    public synchronized boolean accept(int proposalId, Object proposalValue) throws RemoteException {
        if(randomFailTrigger && shouldRandomFail()){
            throw new RemoteException("["+serverId+"]"+"Acceptor random failure.");
        }
        // Implement Paxos accept logic here
        if (proposalId >= highestProposeId) {
            highestProposeId = proposalId;
            lastProposalValue = proposalValue;
            return true;
        }
        return false;
    }

    /**
     * Propose the proposalValue to all Acceptors. If more than half of the acceptors accept the proposal,
     * ask all learner to learn the proposalValue.
     *
     * @param proposalId    The unique identifier for the proposal.
     * @param proposalValue The value being proposed.
     * @throws RemoteException
     */
    @Override
    public synchronized void propose(int proposalId, Object proposalValue) throws RemoteException {
        // Implement Paxos propose logic here
        int proposeOkCount = 0;
        int acceptOkCount = 0;
        //1. ask all acceptors to prepare
        for (AcceptorInterface acceptor : acceptors) {

            if(acceptor != null) {

                try {
                    int promiseId = acceptor.prepare(proposalId);
                    if (promiseId == proposalId) {
                        proposeOkCount++;
                    }
                } catch (RemoteException e) {
                    logger.log("Prepare phase failure. ProposalId: " + proposalId);
                }
            }
        }

        //2. accept proposalValue
        if (proposeOkCount > numServers / 2) {
            logger.log("["+proposalId+"]"+"Accept phase starting, [proposeOkCount/numServers]: "+proposeOkCount+"/"+numServers);
            for (AcceptorInterface acceptor : acceptors) {
                if(acceptor != null) {
                    try {
                        boolean isAccepted = acceptor.accept(proposalId, proposalValue);
                        if (isAccepted) acceptOkCount++;
                    } catch (RemoteException e) {
                        logger.log("Accept phase failure. ProposalId: " + proposalId);
                    }
                }
            }
        }

        //3. ask all learner to learn proposalValue
        if (acceptOkCount > numServers / 2) {
            logger.log("["+proposalId+"]"+"Learn phase starting, [acceptOkCount/numServers]: "+acceptOkCount+"/"+numServers);
            for (LearnerInterface learner : learners) {
                if(learner != null) {
                    try {
                        learner.learn(proposalId, proposalValue);
                    } catch (RemoteException e) {
                        logger.log("Learning phase failure. ProposalId: " + proposalId);
                    }
                }
            }
        }

    }

    @Override
    public synchronized void learn(int proposalId, Object acceptedValue) throws RemoteException {
        // Implement Paxos learn logic here
        Operation acceptedOperation = (Operation) acceptedValue;
        String reply = applyOperation(acceptedOperation);
        logger.log(reply);
    }

    /**
     * Generates a unique proposal ID.
     * By default, the highest serverId will be the leader which has the highest weight in proposal ID generation
     * @return A unique proposal ID.
     */
    private int generateProposalId() {
        // Placeholder code to generate a unique proposal ID
        return serverId * 1000 + proposalSequenceNum++;
    }

    /**
     * generate 10% random failure chance
     * @return shouldFail True or False
     */
    public boolean shouldRandomFail(){
        //10% change to fail.
        return random.nextInt(10) == 1;
    }
    /**
     * Apply the given operation to the key-value store.
     *
     * @param operation The operation to apply.
     */
    private String applyOperation(Operation operation) {
        if (operation == null) return "";
        String reply = "";
        switch (operation.type) {
            case "PUT":
                kvStore.put(operation.key, operation.value);
                reply = "["+serverId+"]"+"Successfully put: "+operation.key;
                break;
            case "DELETE":
                kvStore.remove(operation.key);
                reply = "["+serverId+"]"+"Successfully removed: "+operation.key;
                break;
            case "GET":
                reply = kvStore.get(operation.key);
                break;
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operation.type);
        }
        return reply;
    }

    /**
     * Static class representing an operation on the key-value store.
     */
    private static class Operation {
        String type;
        String key;
        String value;

        Operation(String type, String key, String value) {
            this.type = type;
            this.key = key;
            this.value = value;
        }

        Operation(String type, String key) {
            this(type, key, null);
        }
    }

    // Other methods as needed
}
