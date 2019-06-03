package ca.surgestorm.notitowin.controller.networking.linkHandlers;

import android.util.Log;
import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.backend.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
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
        this.socket = socket;
        this.connectionSource = connectionSource;
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
        System.err.println("Reset Method Invoked!");

        Socket oldSocket = socket;
        socket = newSocket;

        this.connectionSource = connectionSource;

        if (oldSocket != null) {
            Log.e("LANLINK", "Closing old socket for server ID: " + serverID);
            oldSocket.close();
        }

        new Thread(
                () -> {
                    try {
                        DataInputStream reader =
                                new DataInputStream(newSocket.getInputStream());
                        int errCount = 0;
                        while (true) {
                            String packet;
                            try {
                                packet = reader.readUTF();
                            } catch (SocketTimeoutException | EOFException e) {
                                continue;
                            }
                            if (packet == null) {
                                errCount++;
                                if (errCount >= 4) {
                                    throw new IOException("End of Stream");
                                } else {
                                    System.err.println("Packet null, retrying for the " + errCount + " time.");
                                    continue;
                                }
                            }
                            if (packet.isEmpty()) {
                                continue;
                            }
                            Log.i("LANLink", "Packet: " + packet + " Received.");
                            JSONConverter json = JSONConverter.unserialize(packet);
                            errCount = 0;
                            receivedNetworkPacket(json);
                        }
                    } catch (IOException e) {
                        Log.i("LanLink", "Socket closed: " + newSocket.hashCode() + ". Reason: " + e.getMessage());
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
//            final ServerSocket server; //TODO Implement DataLoad Sending
//
//            if (key != null) {
//                Log.i("LANLink", "Encrypting Packet...");
//                json = RSAHelper.encrypt(json, key);
//                Log.i("LANLink", "Packet encrypted successfully!");
//            }

            try {
                DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
                writer.writeUTF(json.serialize());
                writer.flush();
//                writer.close();
            } catch (Exception e) {
                disconnect(); // main socket is broken, disconnect
                e.printStackTrace();
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
        Log.i("LANLink", "Sending Packet, no need to Encrypt.");
        return sendPacketProtected(json, callback, null);
    }

    public boolean sendPacket(JSONConverter json, Server.SendPacketStatusCallback callback, PublicKey key) {
        Log.i("LANLink", "Sending Packet, going to Encrypt");
        return sendPacketProtected(json, callback, key);
    }

    private void receivedNetworkPacket(JSONConverter json) {
        Log.i("LANLink", "Received Packet of Type: " + json.getType());
//        if (json.getType().equals(PacketType.ENCRYPTED_PACKET)) {
//            try {
//                Log.i("LANLink", "Trying to Decrypt Packet...");
//                json = RSAHelper.decrypt(json, privKey);
//                Log.i("LANLink", "Packet decrypted successfully!");
//            } catch (Exception e) {
//                System.err.println("Error Decrypting Packet.");
//                e.printStackTrace();
//            }
//        }
        packageReceived(json);
    }

    private void packageReceived(JSONConverter json) {
        Log.i("LANLink", "Sending Packet to all Receivers!");
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
