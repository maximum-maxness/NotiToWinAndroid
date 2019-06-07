package ca.surgestorm.notitowin.controller.networking.linkHandlers;

import android.util.Log;
import ca.surgestorm.notitowin.backend.DataLoad;
import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.backend.helpers.PacketType;
import ca.surgestorm.notitowin.backend.helpers.RSAHelper;

import java.io.*;
import java.net.*;
import java.nio.channels.NotYetConnectedException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class LANLink {

    private final String serverID;
    private final ArrayList<PacketReceiver> receivers = new ArrayList<>();
    private final LinkDisconnectedCallback callback;
    private PrivateKey privKey;
    private ConnectionStarted connectionSource;
    private LANLinkProvider linkProvider;
    private volatile Socket socket = null;

    public LANLink(
            String serverID,
            LANLinkProvider linkProvider,
            Socket socket,
            ConnectionStarted connectionSource) throws IOException {
        this.serverID = serverID;
        this.linkProvider = linkProvider;
        this.connectionSource = connectionSource;
        callback = linkProvider;
        reset(socket, connectionSource);
    }

    public InetAddress getIP() {
        return socket.getInetAddress();
    }

    public String getName() {
        return "LanLink";
    }

    public LANLinkHandler getPairingHandler(
            Server device, LANLinkHandler.PairingHandlerCallback callback) {
        return new LANLinkHandler(device, callback);
    }

    public ConnectionStarted getConnectionSource() {
        return connectionSource;
    }

    public Socket reset(Socket newSocket, ConnectionStarted connectionSource) throws IOException {
        Log.e(getClass().getSimpleName(), "Reset Method Invoked!");

        Socket oldSocket = socket;
        socket = newSocket;

        this.connectionSource = connectionSource;

        if (oldSocket != null) {
            Log.e(getClass().getSimpleName(), "Closing old socket for server ID: " + serverID);
            oldSocket.close();
        }
        new Thread(
                () -> {
                    try {
                        DataInputStream reader =
                                new DataInputStream(newSocket.getInputStream());
                        while (true) {
                            String packet;
                            try {
                                packet = reader.readUTF();
                            } catch (SocketTimeoutException e) {
                                continue;
                            }
                            if (packet == null) {
                                throw new IOException("End of Stream");
                            }
                            if (packet.isEmpty()) {
                                continue;
                            }
                            Log.i(getClass().getSimpleName(), "Packet: " + packet + " Received.");
                            JSONConverter json = JSONConverter.unserialize(packet);
                            receivedNetworkPacket(json);
                        }
                    } catch (IOException e) {
                        Log.i(getClass().getSimpleName(), "Socket closed: " + newSocket.hashCode() + ". Reason: " + e.getMessage());
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ignored) {
                        } // Wait a bit because we might receive a new socket meanwhile
                        boolean thereIsaANewSocket = (newSocket != socket);
                        if (!thereIsaANewSocket) {
                            callback.linkDisconnected(LANLink.this);
                        }
                    }
                })
                .start();

        return oldSocket;
    }

    private boolean sendPacketProtected(JSONConverter json, final Server.SendPacketStatusCallback callback, PublicKey key) {
        if (socket == null) {
            callback.onFailure(new NotYetConnectedException());
            return false;
        }

        try {
            final ServerSocket server;
            if (json.getBoolean("hasDataLoad", false)) {
                server = LANLinkProvider.openTCPServerOnFreePort();
                Log.e(getClass().getSimpleName(), "Going to use port " + server.getLocalPort() + " for dataLoad!");
                json.set("dataLoadPort", server.getLocalPort());
                Log.e(getClass().getSimpleName(), String.valueOf(json.has("dataLoadPort")));
            } else {
                server = null;
            }

            if (key != null) {//FIXME We dont need to encrypt once we have sslsockets working properly.
                Log.i(getClass().getSimpleName(), "Encrypting Packet...");
                json = RSAHelper.encrypt(json, key);
                Log.i(getClass().getSimpleName(), "Packet encrypted successfully!");
            }

            try {
                DataOutputStream writer = new DataOutputStream(this.socket.getOutputStream());
                writer.writeUTF(json.serialize());
                writer.flush();
//                writer.close();
            } catch (Exception e) {
                disconnect(); // main socket is broken, disconnect
                e.printStackTrace();
            }

            if (server != null) {
                Socket dataLoadSocket = null;
                OutputStream outputStream = null;
                InputStream inputStream;
                try {
                    server.setSoTimeout(10000);

                    dataLoadSocket = server.accept();

//                    dataLoadSocket = SSLHelper.convertToSSLSocket(MainActivity.getAppContext(), dataLoadSocket, getServerID(), true, false);

                    outputStream = dataLoadSocket.getOutputStream();
                    inputStream = json.getDataLoad().getInputStream();

                    Log.i(getClass().getSimpleName(), "Sending DataLoad!");
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long size = json.getDataLoad().getSize();
                    long progress = 0;
                    long timeSinceLastUpdate = -1;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        progress += bytesRead;
                        outputStream.write(buffer, 0, bytesRead);
                        if (size > 0) {
                            if (timeSinceLastUpdate + 500 < System.currentTimeMillis()) {
                                long percent = ((100 * progress) / size);
                                callback.onProgressChanged((int) percent);
                                timeSinceLastUpdate = System.currentTimeMillis();
                            }
                        }
                    }
                    outputStream.flush();
                    Log.i(getClass().getSimpleName(), "Finished Sending DataLoad, " + progress + " bytes written.");
                } finally {
                    try {
                        server.close();
                    } catch (Exception ignored) {
                    }
                    try {
                        dataLoadSocket.close();
                    } catch (Exception ignored) {
                    }
                    json.getDataLoad().getInputStream().close();
                    try {
                        outputStream.close();
                    } catch (Exception ignored) {
                    }
                }
            }
            
            
            callback.onSuccess();
            return true;
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            }
            return false;
        }
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public boolean sendPacket(JSONConverter json, Server.SendPacketStatusCallback callback) {
        Log.i(getClass().getSimpleName(), "Sending Packet, no need to Encrypt.");
        return sendPacketProtected(json, callback, null);
    }

    public boolean sendPacket(JSONConverter json, Server.SendPacketStatusCallback callback, PublicKey key) {
        Log.i(getClass().getSimpleName(), "Sending Packet, going to Encrypt");
        return sendPacketProtected(json, callback, key);
    }

    private void receivedNetworkPacket(JSONConverter json) {
        Log.i(getClass().getSimpleName(), "Received Packet of Type: " + json.getType());
        if (json.getType().equals(PacketType.ENCRYPTED_PACKET)) {
            try {
                Log.i(getClass().getSimpleName(), "Trying to Decrypt Packet...");
                json = RSAHelper.decrypt(json, privKey);
                Log.i(getClass().getSimpleName(), "Packet decrypted successfully!");
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Error Decrypting Packet.");
                e.printStackTrace();
            }
        }

        if (json.getBoolean("hasDataLoad", false)) {
            Log.i(getClass().getSimpleName(), "Receiving DataLoad!");
            Socket dataLoadSocket = new Socket();
            try {
                int tcpPort = json.getInt("dataLoadPort");
                InetSocketAddress deviceAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
                Log.i(getClass().getSimpleName(), "Connecting to DataLoad server on port " + tcpPort);
                dataLoadSocket.connect(new InetSocketAddress(deviceAddress.getAddress(), tcpPort));
                Log.i(getClass().getSimpleName(), "Connected!");
//                dataLoadSocket = SSLHelper.convertToSSLSocket(MainActivity.getAppContext(), dataLoadSocket, getServerID(), true, true);
//                Log.i(getClass().getSimpleName(), "Converted!");
                json.setDataLoad(new DataLoad(dataLoadSocket.getInputStream(), json.getLong("dataLoadSize")));
            } catch (Exception e) {
                try {
                    dataLoadSocket.close();
                } catch (Exception ignored) {
                }
                e.printStackTrace();
            }

        }

        packageReceived(json);
    }

    private void packageReceived(JSONConverter json) {
        Log.i(getClass().getSimpleName(), "Sending Packet to all Receivers!");
        for (PacketReceiver pr : receivers) {
            pr.onPacketReceived(json);
        }
    }

    public void disconnect() {
        Log.e("LANLink", "Disconnecting Socket.");
        linkProvider.connectionLost(this);
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getServerID() {
        return this.serverID;
    }

    public void setPrivateKey(PrivateKey key) {
        this.privKey = key;
    }

    public LANLinkProvider getLinkProvider() {
        return this.linkProvider;
    }

    public boolean linkShouldBeKeptAlive() {
        return true;
    }

    public void addPacketReceiver(PacketReceiver pr) {
        receivers.add(pr);
    }

    public void removePacketReceiver(PacketReceiver pr) {
        receivers.remove(pr);
    }

    public enum ConnectionStarted {
        Locally,
        Remotely
    }

    public interface PacketReceiver {
        void onPacketReceived(JSONConverter json);
    }

    public interface LinkDisconnectedCallback {
        void linkDisconnected(LANLink broken);
    }
}
