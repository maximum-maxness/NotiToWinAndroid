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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import ca.surgestorm.notitowin.backend.PacketType;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.ui.MainActivity;

public class ServerDetector implements Runnable {//TODO Rewrite this and combine with server sender

    private static DatagramSocket socket;
    private static final int port = 8657;
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

    public static InetAddress getIp() {
        return ip;
    }

    public static void setIp(InetAddress ip) {
        ServerDetector.ip = ip;
    }

    @Override
    public void run() {
        try {
            running = true;
            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] sendData = PacketType.CLIENT_PAIR_REQUEST.getBytes();

            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), port);
                socket.send(sendPacket);
                Log.i("ServerDetector", "Sending Packet to 255.255.255.255");
            } catch (Exception e) {
                e.printStackTrace();
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
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(receivePacket);

            //Notify upon response
            Log.i("ServerDetector", "Response from server: " + receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
            Server server;
            if (message.equals(PacketType.SERVER_PAIR_RESPONSE)) {
                server = new Server(
                        receivePacket.getAddress().getHostAddress(),
                        receivePacket.getPort(),
                        0,
//                        R.drawable.windows10,
                        "Server",
                        "Windows 10"
                );
                Bundle bundle = new Bundle();
                bundle.putString("jsonString", server.toString());
                Message messageSender = new Message();
                messageSender.setData(bundle);
                handle.sendMessage(messageSender);
                byte[] replyMessage = PacketType.CLIENT_PAIR_CONFIRM.getBytes();
                DatagramPacket confirmPacket = new DatagramPacket(replyMessage, replyMessage.length, InetAddress.getByName(receivePacket.getAddress().getHostAddress()), receivePacket.getPort());
                socket.send(confirmPacket);
            }
        } catch (IOException ex) {
            Log.e("ServerDetector", "Error Finding Server");
            ex.printStackTrace();
        }

    }

    public void sendJson(String json) throws IOException {
        byte[] sendData = PacketType.NOTI_REQUEST.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, this.ip, this.port);
        this.socket.send(packet);
        System.out.println("Sent Request!");
        if (waitForServerReady()) {
            System.out.println("Got Reply!");
            byte[] newSendData = json.getBytes();
            DatagramPacket newPacket = new DatagramPacket(newSendData, newSendData.length, this.ip, this.port);
            this.socket.send(newPacket);
            Log.i("ServerSender", "Sent: " + json);
            if (!waitForServerReady()) {
                Log.e("ServerSender", "Server Did Not Reply Ready after sending json!");
            } else {
                Log.i("ServerSender", "Sent JSON Successfully!");
            }
        } else {
            Log.e("ServerSender", "Server did not reply ready after sending noti request!");
        }
    }

    private boolean waitForServerReady() throws IOException {
        byte[] replyBuff = new byte[15000];
        DatagramPacket serverReply = new DatagramPacket(replyBuff, replyBuff.length);
        this.socket.receive(serverReply);
        String message = new String(serverReply.getData());
        message = message.trim();
        Log.w("ServerReplyMessage", message);
        return message.equals(PacketType.READY_RESPONSE);
    }

    public void stop(){
        socket.close();
        running = false;
    }

    private static class ServerDetectorHolder {
        private static final ServerDetector INSTANCE = new ServerDetector();
    }
}
