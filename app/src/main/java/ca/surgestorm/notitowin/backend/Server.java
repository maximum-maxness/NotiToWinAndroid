package ca.surgestorm.notitowin.backend;

public class Server {
    private String ip;
    private int connectionMethod, previewImage;
    private String serverName;
    private String os;


    /*

     * @param ip The IP for the server on the network
     * @param connectionMethod 0 for Local Network, 1 For External Network (Only 0 For now)
     * @param serverName The server's name
     * @param os The server's OS (Only WIN10 for now)

     */
    public Server(String ip, int connectionMethod, int previewImage, String serverName, String os) {
        this.ip = ip;
        this.connectionMethod = connectionMethod;
        this.previewImage = previewImage;
        this.serverName = serverName;
        this.os = os;
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

    //@return returns the servers OS (Should be "WIN10")
    public String getOs() {
        return "Windows" + " v." + this.os;
    }

    public int getPreviewImage() {
        return previewImage;
    }
}
