package server;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class CoordinatorImpl extends UnicastRemoteObject implements Coordinator {

  private List<Participant> participants;
  public CoordinatorImpl()throws RemoteException{
    super();
  }
  public CoordinatorImpl(List<String> participantHosts, List<Integer> participantPorts) throws RemoteException {
    super();
    participants = new ArrayList<>();
    for (int i = 0; i < participantHosts.size(); i++) {
      try {
        Registry registry = LocateRegistry.getRegistry(participantHosts.get(i), participantPorts.get(i));
        Participant participant = (Participant) registry.lookup("StoreService");
        participants.add(participant);
      } catch (Exception e) {
        throw new RemoteException("Unable to connect to participant", e);
      }
    }
  }
  public void setParticipants(List<String> participantHosts, List<Integer>participantPorts) throws RemoteException{
    participants = new ArrayList<>();
    for (int i = 0; i < participantHosts.size(); i++) {
      try {
        Registry registry = LocateRegistry.getRegistry(participantHosts.get(i), participantPorts.get(i));
        Participant participant = (Participant) registry.lookup("StoreService");
        participants.add(participant);
      } catch (Exception e) {
        throw new RemoteException("Unable to connect to participant", e);
      }
    }
  }
  public boolean prepareTransaction(String transaction) throws RemoteException {
    for (Participant participant : participants) {
      if (!participant.prepare(transaction)) {
        return false;
      }
    }
    return true;
  }

  public void commitTransaction() throws RemoteException{
    for(Participant participant : participants){
      participant.commit();
    }
  }

  public void abortTransaction() throws RemoteException{
    for(Participant participant: participants){
      participant.abort();
    }
  }
}

