package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Coordinator extends Remote {
    public void setParticipants(List<String> participantHosts, List<Integer> participantPorts) throws RemoteException;
    public boolean prepareTransaction(String Transaction) throws RemoteException;
    public void commitTransaction() throws RemoteException;
    public void abortTransaction() throws RemoteException;
}
