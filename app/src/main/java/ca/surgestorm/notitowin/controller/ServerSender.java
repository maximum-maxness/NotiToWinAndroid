package ca.surgestorm.notitowin.controller;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.backend.PacketType;

public class ServerSender {
    private final int port = 9856;
    private Socket socket;
    private InputStream inputStream;
    private DataInputStream dataInputStream;
    private OutputStream outputStream;
    private DataOutputStream dataOutputStream;
    private InetAddress ip;

    public ServerSender() {
    }

    public boolean connect() {
        int count = 0;
        int maxTries = 5;
        this.socket = new Socket();
        while (true) {
            try {
                this.socket.connect(new InetSocketAddress(ip, port));
                openStreams();
                break;
            } catch (IOException e) {
                // handle exception
                if (++count == maxTries) {
                    Log.e("ServerSender", "Couldn't connect to server after " + count + " tries.");
                    return false;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return waitForServerReady();
    }

    public void sendMessage(String message) throws IOException {
        this.dataOutputStream.writeUTF(message);
        this.dataOutputStream.flush();
        Log.i("ServerSender", "Sent message: " + message);
    }

    public String receiveMessage() throws IOException {
        Log.i("ServerSender", "Waiting for Packet...");
        String message = this.dataInputStream.readUTF();
        Log.i("ServerSender", "Received Message: " + message);
        return message;
    }

    public InetAddress getIP() {
        return this.ip;
    }

    public void setIP(InetAddress ip) {
        this.ip = ip;
    }

    Socket getSocket() {
        return this.socket;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public int getPort() {
        return this.port;
    }

    public void disconnect() throws IOException {
        sendMessage(PacketType.UNPAIR_CMD);
        stop();
    }

    public void stop() throws IOException {
        closeStreams();
        this.socket.close();
        Thread.currentThread().interrupt();
    }


    public void openStreams() throws IOException {
        this.inputStream = this.socket.getInputStream();
        this.outputStream = this.socket.getOutputStream();
        this.dataInputStream = new DataInputStream(this.inputStream);
        this.dataOutputStream = new DataOutputStream(this.outputStream);
    }

    public void closeStreams() throws IOException {
        this.dataInputStream.close();
        this.dataOutputStream.close();
        this.inputStream.close();
        this.outputStream.close();
    }

    public void sendJson(String json) throws IOException {
        if (this.socket.isConnected()) {
            sendMessage(new JSONConverter(PacketType.NOTI_REQUEST).serialize());
            System.out.println("Sent Request!");
            if (waitForServerReady()) {
                System.out.println("Got Reply!");
                sendMessage(json);
                Log.i("ServerSender", "Sent: " + json);
                if (!waitForServerReady()) {
                    Log.e("ServerSender", "Server Did Not Reply Ready after sending json!");
                } else {
                    Log.i("ServerSender", "Sent JSON Successfully!");
                }
            } else {
                Log.e("ServerSender", "Server did not reply ready after sending noti request!");
            }
        } else {
            closeStreams();
            socket.close();

        }
    }


    private boolean waitForServerReady() {
        Log.i("ServerSender", "Waiting for Server Ready...");
        String message = null;
        try {
            message = receiveMessage();
        } catch (IOException e) {
            Log.e("ServerSender", "Couldn't Receive Message. Error: " + e.getLocalizedMessage());
        }
        if (message == null) {
            Log.e("ServerSender", "Message Received is null!");
            return false;
        }
        JSONConverter json = JSONConverter.unserialize(message);
        Log.i("ServerSender", "Received: " + message);
        return json.getType().equals(PacketType.READY_RESPONSE);
    }
}
