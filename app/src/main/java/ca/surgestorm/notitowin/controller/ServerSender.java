package ca.surgestorm.notitowin.controller;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.UnknownHostException;
import java.util.Arrays;

import ca.surgestorm.notitowin.backend.PacketType;

import static java.net.InetAddress.getByName;

public class ServerSender {
    private DatagramSocket socket;
    private InetAddress ip;
    private int port;

    public ServerSender(String ip, int port) throws SocketException, UnknownHostException {
        this.ip = getByName(ip);
        this.port = port;
        this.socket = new DatagramSocket(this.port);
    }

    public void sendJson(String json) throws IOException {
        byte[] sendData = PacketType.NOTI_REQUEST.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, this.ip, this.port);
        this.socket.send(packet);

        if (waitForServerReady()) {
            sendData = json.getBytes();
            packet = new DatagramPacket(sendData, sendData.length, this.ip, this.port);
            this.socket.send(packet);
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
}
