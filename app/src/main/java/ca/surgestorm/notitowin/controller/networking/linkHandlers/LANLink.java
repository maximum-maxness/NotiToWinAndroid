package ca.surgestorm.notitowin.controller.networking.linkHandlers;

import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.backend.helpers.PacketType;
import ca.surgestorm.notitowin.backend.helpers.RSAHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
            ConnectionStarted connectionSource) {
        this.serverID = serverID;
        this.linkProvider = linkProvider;
        this.connectionSource = connectionSource;
        this.socket = socket;
        callback = linkProvider;
    }

    public InetAddress getIP() {
        return socket.getInetAddress();
    }

    public String getName() {
        return "LanLink";
    } //TODO Socket is always null, fix.

    public LANLinkHandler getPairingHandler(
            Server device, LANLinkHandler.PairingHandlerCallback callback) {
        return new LANLinkHandler(device, callback);
    }

    public Socket reset(Socket newSocket, ConnectionStarted connectionSource) throws IOException {
        Socket oldSocket = socket;
        socket = newSocket;

        this.connectionSource = connectionSource;

        if (oldSocket != null) {
            oldSocket.close();
        }

        new Thread(
                () -> {
                    try {
                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
                        while (true) {
                            String packet;
                            try {
                                packet = reader.readLine();
                            } catch (SocketTimeoutException e) {
                                continue;
                            }
                            if (packet == null) {
                                throw new IOException("End of Stream");
                            }
                            if (packet.isEmpty()) {
                                continue;
                            }
                            JSONConverter json = JSONConverter.unserialize(packet);
                            receivedNetworkPacket(json);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
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
            final ServerSocket server; //TODO Implement DataLoad Sending

            if (key != null) {
                System.out.println("Encrypting Packet...");
                json = RSAHelper.encrypt(json, key);
                System.out.println("Packet encrypted successfully!");
            }

            try {
                OutputStream writer = socket.getOutputStream();
                writer.write(json.serialize().getBytes());
                writer.flush();
            } catch (Exception e) {
                disconnect(); // main socket is broken, disconnect
                throw e;
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

    public boolean sendPacket(JSONConverter json, Server.SendPacketStatusCallback callback) {
        System.out.println("Sending Packet, no need to Encrypt.");
        return sendPacketProtected(json, callback, null);
    }

    public boolean sendPacket(JSONConverter json, Server.SendPacketStatusCallback callback, PublicKey key) {
        System.out.println("Sending Packet, going to Encrypt");
        return sendPacketProtected(json, callback, key);
    }

    private void receivedNetworkPacket(JSONConverter json) {
        System.out.println("Received Packet of Type: " + json.getType());
        if (json.getType().equals(PacketType.ENCRYPTED_PACKET)) {
            try {
                System.out.println("Trying to Decrypt Packet...");
                json = RSAHelper.decrypt(json, privKey);
                System.out.println("Packet decrypted successfully!");
            } catch (Exception e) {
                System.err.println("Error Decrypting Packet.");
                e.printStackTrace();
            }
        }
        packageReceived(json);
    }

    private void packageReceived(JSONConverter json) {
        System.out.println("Sending Packet to all Receivers!");
        for (PacketReceiver pr : receivers) {
            pr.onPacketReceived(json);
        }
    }

    public void disconnect() {
        System.out.println("Disconnecting Socket.");
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
    } //TODO Remedy Temp Fix (Should be false)

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
