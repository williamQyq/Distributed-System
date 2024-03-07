package server;

import shared.ServiceLogger;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
    public static void main(String[] args) {
        int coordinatorPort, participantNum;
        String coordinatorHost;
        List<String> participantHosts = new ArrayList<>();
        List<Integer> participantPorts = new ArrayList<>();

        if (args.length < 4) {
            System.out.println("Usage: java Server [CoordinatorHost] [CoordinatorPort] [numberOfParticipant] [ParticipantHost 1] [ParticipantPort 1] [Participant Host 2] [Participant Port 2]...");
            return;
        }

        //init coordinator and participants
        try {
            coordinatorHost = args[0];
            coordinatorPort = Integer.parseInt(args[1]);
            participantNum = Integer.parseInt(args[2]); //number of participants
            //list of participants and its ports
            for (int i = 3; i < args.length; i++) {
                if ((i-3) % 2 == 0) {
                    participantHosts.add(args[i]);
                } else {
                    participantPorts.add(Integer.parseInt(args[i]));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Usage: java Server [CoordinatorHost] [CoordinatorPort] [numberOfParticipant] [ParticipantHost]...");
            return;
        }

        ServiceLogger logger = new ServiceLogger();

        try {
            // Start the coordinator
            CoordinatorImpl coordinator = new CoordinatorImpl();
            Registry coordinatorRegistry = LocateRegistry.createRegistry(coordinatorPort);
            coordinatorRegistry.bind("Coordinator", coordinator);

            // Start the participants
            for (int i = 0; i < participantHosts.size(); i++) {
                ParticipantImpl participant = new ParticipantImpl(coordinatorHost, coordinatorPort);
                Registry participantRegistry = LocateRegistry.createRegistry(participantPorts.get(i));
                participantRegistry.bind("StoreService", participant);
            }

            coordinator.setParticipants(
                    participantHosts,
                    participantPorts
            );

            logger.log("Servers ready");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }
}
