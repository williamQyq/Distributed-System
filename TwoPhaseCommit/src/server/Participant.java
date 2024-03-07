package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Participant extends Remote {
    public boolean prepare(String transaction) throws RemoteException;
    public String commit() throws RemoteException;

    public String abort() throws RemoteException;
}
