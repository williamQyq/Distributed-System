package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StoreService extends Remote {
    public String get(String key) throws RemoteException;

    public String put(String key, String value) throws RemoteException;

    public String delete(String key) throws RemoteException;
}
