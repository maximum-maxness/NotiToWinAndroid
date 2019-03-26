package ca.surgestorm.notitowin.backend;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import ca.surgestorm.notitowin.R;

public class Server {
    private String ip;
    private int connectionMethod, port;
    private String serverName;
    private String osName, osVersion;


    /*

     * @param ip The IP for the server on the network
     * @param connectionMethod 0 for Local Network, 1 For External Network (Only 0 For now)
     * @param serverName The server's name
     * @param os The server's OS (Only WIN10 for now)

     */
    public Server(String ip, int port, int connectionMethod, String serverName, String osName, String osVersion) {
        this.ip = ip;
        this.port = port;
        this.connectionMethod = connectionMethod;
        this.serverName = serverName;
        this.osName = osName;
        this.osVersion = osVersion;
    }

    //@return returns the server's IP
    public String getIp() {
        return this.ip;
    }

    //@return returns the connection method used for the server (Should be 0 for LAN)
    public String getConnectionMethod() {
        if (this.connectionMethod == 0) {
            return "LAN";
        } else {
            return "Other";
        }
    }

    //@return returns the server's name
    public String getServerName() {
        return this.serverName;
    }

    @NonNull
    public static Server jsonConverter(JSONObject json) throws JSONException {
        String ip, serverName, osName, osVersion;
        int port, connectionMethod, previewImage;
        ip = (String) json.get("ip");
        port = (int) json.get("port");
        connectionMethod = (int) json.get("connectionMethod");
        serverName = (String) json.get("serverName");
        osName = (String) json.get("osName");
        osVersion = (String) json.get("osVersion");
        return new Server(ip, port, connectionMethod, serverName, osName, osVersion);
    }

    //@return returns the servers OS (Should be "WIN10")
    public String getOs() {
        return this.osName + " " + this.osVersion;
    }

    public int getPreviewImage() {
        switch (osName.toLowerCase()) {
            case "windows":
                if (osVersion.equals("10")) {
                    return R.drawable.windows10;
                } else {
                    return R.drawable.windows7;
                }
            case "linux":
                return R.drawable.linux;
            case "mac":
                return R.drawable.apple;
            default:
                return R.drawable.windows7;
        }
    }

    public int getPort() {
        return this.port;
    }

    public JSONObject populateJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("ip", this.ip);
        json.put("port", this.port);
        json.put("connectionMethod", this.connectionMethod);
        json.put("serverName", this.serverName);
        json.put("osName", this.osName);
        json.put("osVersion", this.osVersion);
        return json;
    }

    public String toString() {
        try {
            return populateJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
