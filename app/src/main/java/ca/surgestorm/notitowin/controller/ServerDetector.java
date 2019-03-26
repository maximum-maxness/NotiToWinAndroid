package ca.surgestorm.notitowin.controller;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import ca.surgestorm.notitowin.backend.IPGetter;
import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.backend.PacketType;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.ui.MainActivity;

public class ServerDetector implements Runnable {//TODO Rewrite this and combine with server sender

    private static MulticastSocket socket;
    private static final int port = 8657;
    private static InetAddress currentIP;
    private static int currentPort;
    private static InetAddress ip;
    private static boolean running = false;

    @SuppressLint("HandlerLeak")
    private Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            String s = (String) bundle.get("jsonString");
            try {
                JSONObject json = new JSONObject(s);
                Server server = Server.jsonConverter(json);
                MainActivity.updateList(server);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public static boolean isRunning() {
        return running;
    }

    public static ServerDetector getInstance() {
        return ServerDetectorHolder.INSTANCE;
    }

    @Override
    public void run() {
        try {

            running = true;
            socket = new MulticastSocket(8657);
//            socket.setBroadcast(true);
            InetAddress intIP = InetAddress.getByName("230.1.1.1");
            socket.joinGroup(intIP);

            byte[] sendData = PacketType.makeDiscoveryPacket().getBytes();
            //Try the 255.255.255.255 first
//            try {
//                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), port);
//                socket.send(sendPacket);
//                Log.i("ServerDetector", "Sending Packet to 255.255.255.255");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            // Broadcast the message over all the network interfaces
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.getDisplayName().contains("radio")) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, port);
                        socket.send(sendPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Log.i("ServerDetector", "Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            Log.i("ServerDetector", "Sent all packets. Waiting for a reply.");

            //Wait for a response
            while (!socket.isClosed()) {
                DatagramPacket receivePacket = receiveMessage();
                String message = new String(receivePacket.getData()).trim();
                JSONConverter json = JSONConverter.unserialize(message);
                Log.i("ServerDetector", "Message: " + message);


                //Notify upon response

                //Check if the message is correct
                Server server;
                if (json.getType().equals(PacketType.SERVER_PAIR_RESPONSE)) {
                    server = new Server(
                            receivePacket.getAddress().getHostAddress(),
                            receivePacket.getPort(),
                            0,
//                        R.drawable.windows10,
                            json.getString("deviceName"),
                            json.getString("osName"),
                            json.getString("osVersion")
                    );
                    Log.i("ServerDetector", "Created Server Object!");
                    if (!MainActivity.hasServer(server)) {
                        Bundle bundle = new Bundle();
                        bundle.putString("jsonString", server.toString());
                        Message messageSender = new Message();
                        messageSender.setData(bundle);
                        handle.sendMessage(messageSender);
                    } else {
                        Log.i("ServerDetector", "Already have server.");
                    }
                }
            }
        } catch (SocketException e1) {
            Log.e("ServerDetector", "Socket Closed");
        } catch (IOException ex) {
            Log.e("ServerDetector", "Error Finding Server, " + ex.getLocalizedMessage());
            ex.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void sendConfirm(InetAddress ip, int port) throws IOException {
        byte[] replyMessage = new JSONConverter(PacketType.CLIENT_PAIR_CONFIRM).serialize().getBytes();
        DatagramPacket confirmPacket = new DatagramPacket(replyMessage, replyMessage.length, ip, port);
        socket.send(confirmPacket);
        Log.i("ServerDetector", "Send Ready Packet!");
    }

    private DatagramPacket receiveMessage() throws IOException {
        byte[] recvBuf = new byte[15000];
        DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
        socket.receive(receivePacket);
        Log.i("ServerDetector", "Response from server: " + receivePacket.getAddress().getHostAddress());
        if (addressMatchesInternal(receivePacket.getAddress().getHostAddress())) {
            return receiveMessage();
        }
        return receivePacket;
    }

    private boolean addressMatchesInternal(String addr) {
        for (String s : IPGetter.getInternalIP(true)) {
            if (s.equals(addr)) {
                return true;
            }
        }
        return false;
    }

    public void stop() {
        try {
            InetAddress add = InetAddress.getByName("230.1.1.1");
            socket.leaveGroup(add);
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket.close();
        running = false;
    }

    private static class ServerDetectorHolder {
        private static final ServerDetector INSTANCE = new ServerDetector();
    }
}
