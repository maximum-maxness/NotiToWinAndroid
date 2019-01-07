package ca.surgestorm.notitowin.controller;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.ui.MainActivity;

public class ServerDetector {

    private static DatagramSocket socket;
    private static ArrayList<Server> serverList;

    public static void findServer(){ //TODO Rewrite to work with android + different thread
        // Find the server using UDP broadcast
        try {
            //Open a random port to send the package
            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] sendData = "PAIR_NOTISERVER_REQUEST".getBytes();

            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
                socket.send(sendPacket);
                System.out.println(ServerDetector.class.getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception e) {
            }

            // Broadcast the message over all the network interfaces
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                        socket.send(sendPacket);
                    } catch (Exception e) {
                    }

                    System.out.println(ServerDetector.class.getName() + ">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            System.out.println(ServerDetector.class.getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

            //Wait for a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(receivePacket);

            //We have a response
            System.out.println(ServerDetector.class.getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
//            if (message.equals("PAIR_NOTISERVER_RESPONSE")) {
//                Server server = new  Server(
//                        receivePacket.getAddress().getHostAddress(),
//                        receivePacket.getPort(),
//                        0,
//                        R.drawable.windows10,
//                        "Server",
//                        "Windows 10"
//                );
//                serverList.add(server);
//
//            }
            MainActivity.updateList(serverList);
            //Close the port!
            socket.close();
        } catch (IOException ex) {
            Log.e("ServerDetector", "Error Finding Server");
            ex.printStackTrace();
        }
    }
}
