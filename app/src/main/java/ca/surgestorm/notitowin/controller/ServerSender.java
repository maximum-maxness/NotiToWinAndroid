package ca.surgestorm.notitowin.controller;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import ca.surgestorm.notitowin.backend.PacketType;

public class ServerSender {
    private final int port = 9856;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader bufferedReader;
    private InetAddress ip;
    private PrintWriter printWriter;

    public ServerSender() {
    }

    public void connect() throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(ip, port));
        openStreams();
    }

    public void sendMessage(String message) {
        this.printWriter.write(message);
    }

    public String receiveMessage() throws IOException {
        return this.bufferedReader.readLine();
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

    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    public int getPort() {
        return this.port;
    }

    public void stop() throws IOException {
        closeStreams();
        this.socket.close();
        Thread.currentThread().interrupt();
    }

    private void openReader() {
        this.bufferedReader = new BufferedReader(new InputStreamReader(this.inputStream));
    }

    private void closeReader() throws IOException {
        if (this.bufferedReader != null)
            this.bufferedReader.close();
    }

    private void openWriter() {
        this.printWriter = new PrintWriter(this.outputStream);
    }

    private void closeWriter() {
        if (this.printWriter != null)
            this.printWriter.close();

    }

    public void openStreams() throws IOException {
        this.inputStream = this.socket.getInputStream();
        openReader();
        this.outputStream = this.socket.getOutputStream();
        openWriter();
    }

    public void closeStreams() throws IOException {
        closeReader();
        this.inputStream.close();
        closeWriter();
        this.outputStream.close();
    }

    public void sendJson(String json) throws IOException {
        sendMessage(PacketType.NOTI_REQUEST);
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
    }


    private boolean waitForServerReady() throws IOException {
        String message = receiveMessage();
        return message.equals(PacketType.READY_RESPONSE);
    }
}
