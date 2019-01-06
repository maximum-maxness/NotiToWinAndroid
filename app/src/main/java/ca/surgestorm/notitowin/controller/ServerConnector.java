package ca.surgestorm.notitowin.controller;

import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import ca.surgestorm.notitowin.backend.JSONConverter;

public class ServerConnector { //TODO Properly Implement Sending JSON/Notifications
    private Socket socket;
    private JSONConverter json;
    private String ip;
    private int port;

    public ServerConnector(String ip, int port) {
        this.ip = ip;
        this.port = port;
        Log.i("ServerConnector", "IP and Port Set!");
    }

    public boolean connectToSocket() {
        try {
            this.socket = new Socket(this.ip, this.port);
            return true;
        } catch (IOException e) {
            Log.e("ServerConnector", "Couldn't Connect to host!");
        }
        return false;
    }

    public void setJson(JSONConverter json) {
        this.json = json;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void sendJSONToServer() {
        if (this.socket.isConnected()) {
            if (this.json != null) {
                try (OutputStreamWriter out = new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8)) {
                    out.write(this.json.serialize());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() {
        if (this.socket != null) {
            if (this.socket.isConnected()) {
                try {
                    this.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
