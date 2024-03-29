package ca.surgestorm.notitowin.controller.networking.linkHandlers;

import android.content.Context;
import android.util.Log;
import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.backend.helpers.PacketType;

import javax.net.SocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LANLinkProvider implements LANLink.LinkDisconnectedCallback {

    static final int DATALOAD_TRANSFER_MIN_PORT = 1936;
    private static final int MAX_PORT = 1958;
    private static final int MIN_PORT = 1938;
    private final HashMap<String, LANLink> visibleServers = new HashMap<>();
    private final CopyOnWriteArrayList<ConnectionReceiver> connectionReceivers = new CopyOnWriteArrayList<>();

    private ServerSocket tcpServer;
    private DatagramSocket udpServer;
    private Context context;

    private boolean listening = false;

    public LANLinkProvider(Context context) {
        this.context = context;
    }

    static ServerSocket openTCPServerOnFreePort() throws IOException {
        Log.i(LANLinkProvider.class.getSimpleName(), "Opening TCP Server On Free Port.");
        int tcpPort = LANLinkProvider.MIN_PORT;
        while (tcpPort <= MAX_PORT) {
            try {
                ServerSocket possibleServer = new ServerSocket();
                possibleServer.bind(new InetSocketAddress(tcpPort));
                Log.i(LANLinkProvider.class.getSimpleName(), "Using port " + tcpPort);
                return possibleServer;
            } catch (IOException e) {
                tcpPort++;
                if (tcpPort == MAX_PORT) {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Bad.");
    }

    public void addConnectionReceiver(ConnectionReceiver cr) {
        connectionReceivers.add(cr);
    }

    public boolean removeConnectionReceiver(ConnectionReceiver cr) {
        return connectionReceivers.remove(cr);
    }

    @Override
    public void linkDisconnected(LANLink broken) {
        Log.i(getClass().getSimpleName(), "Link Disconnected for Server ID: " + broken.getServerID());
        String serverID = broken.getServerID();
        visibleServers.remove(serverID);
        connectionLost(broken);
    }

    private void TCPPacketReceived(Socket socket) {
        Log.i(getClass().getSimpleName(), "TCP Packet Has Been Received!");
        JSONConverter json;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message = reader.readLine();
            Log.i(getClass().getSimpleName(), "Read message: " + message);
            json = JSONConverter.unserialize(message);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        assert json != null;
        Log.i(getClass().getSimpleName(), "JSON Type is.. " + json.getType());
        if (!json.getType().equals(PacketType.IDENTITY_PACKET)) {
            return;
        }
        Log.i(getClass().getSimpleName(), "Identity package received from a TCP connection from " + json.getString("clientName"));
        identityPacketReceived(json, socket, LANLink.ConnectionStarted.Locally);
    }

    private void UDPPacketReceived(DatagramPacket packet) {
        Log.i(getClass().getSimpleName(), "UDP Packet Received!");
        final InetAddress address = packet.getAddress();
        try {
            String message = new String(packet.getData());
//            Log.i(getClass().getSimpleName(), "Received Message: " + message);
            final JSONConverter json = JSONConverter.unserialize(message);
            assert json != null;
            Log.i(getClass().getSimpleName(), "JSON Type is: " + json.getType());
            final String serverID = json.getString("clientID");
            if (!json.getType().equals(PacketType.IDENTITY_PACKET)) {
                return;
            } else {
                String ownID = PacketType.getDeviceID();
                if (ownID.equals(serverID)) {
                    return;
                }
            }

            int tcpPort = json.getInt("tcpPort", MIN_PORT);

            SocketFactory sf = SocketFactory.getDefault();
            Log.i(getClass().getSimpleName(), "Attempting to create a TCP Connection with Address: " + address);
            Socket socket = sf.createSocket(address, tcpPort);
            configureSocket(socket);

            OutputStream out = socket.getOutputStream();
            JSONConverter ownIdentity = PacketType.makeIdentityPacket();
            Log.i(getClass().getSimpleName(), "Sending Identity Packet to address: " + address);
            out.write(ownIdentity.serialize().getBytes());
            out.flush();

            identityPacketReceived(json, socket, LANLink.ConnectionStarted.Remotely);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureSocket(Socket socket) {
        try {
            socket.setKeepAlive(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> processingIDS = new ArrayList<>();

    private void identityPacketReceived(final JSONConverter json, final Socket socket, final LANLink.ConnectionStarted connectionStarted) {


        String myID = PacketType.getDeviceID();
        final String serverID = json.getString("clientID");
        Log.i(getClass().getSimpleName(), "Identity Packet Received from Server ID: " + serverID);
        Log.i(getClass().getSimpleName(), "My ID is: " + myID);
        if (serverID.equals(myID)) {
            Log.e(getClass().getSimpleName(), "The server ID matches my own ID!");
            return;
        }

        if (processingIDS.contains(serverID)) {
            Log.e(getClass().getSimpleName(), "Already processing client ID: " + serverID);
            return;
        } else {
            processingIDS.add(serverID);
        }

        final boolean serverMode = connectionStarted == LANLink.ConnectionStarted.Locally;


        try {
//            SharedPreferences preferences = context.getSharedPreferences("trusted_devices", Context.MODE_PRIVATE);
//            boolean isDeviceTrusted = preferences.getBoolean(serverID, false);
//
//
//            if (isDeviceTrusted) {
////                    && !SSLHelper.isCertificateStored(context, serverID)) {
//                BackgroundService.RunCommand(context, service -> {
//                    Server server = service.getServer(serverID);
//                    if (server == null) return;
//                    if (server.isConnected()) return;
//                    server.unpair();
//                    Log.i(getClass().getSimpleName(), "Retrying as unpaired!");
//                    identityPacketReceived(json, socket, connectionStarted);
//                });
//            }

            //FIXME SSL Handshake not completing
//            final SSLSocket sslSocket = SSLHelper.convertToSSLSocket(context, socket, serverID, isDeviceTrusted, serverMode);
//            sslSocket.addHandshakeCompletedListener(event -> {
//                String mode = serverMode ? "client" : "server";
//                try {
//                    Certificate certificate = event.getPeerCertificates()[0];
//                    json.set("certificate", Base64.getEncoder().encodeToString(certificate.getEncoded()));
//                    Log.i(getClass().getSimpleName(), "Handshake as " + mode + " successful with " + json.getString("clientName") + " secured with " + event.getCipherSuite());
            processingIDS.remove(serverID);
            addLink(json, socket, connectionStarted);
//                } catch (Exception e) {
//                    Log.e(getClass().getSimpleName(), "Handshake as " + mode + " failed with " + json.getString("clientName"));
//                    e.printStackTrace();
//                    processingIDS.remove(serverID);
//                    BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
//                        Server server = service.getServer(serverID);
//                        if (server != null) {
//                            server.unpair();
//                        }
//                    });
//                }
//            });
//            new Thread(() -> {
//                try {
//                    synchronized (this) {
//                        Log.i(getClass().getSimpleName(), "Starting SSL Handshake...");
//                        sslSocket.startHandshake();
//                    }
//                } catch (Exception e) {
//                    Log.e(getClass().getSimpleName(), "Handshake failed with " + json.getString("clientName"));
//                    e.printStackTrace();
//                }
//            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            processingIDS.remove(serverID);
        }
    }

    private void addLink(final JSONConverter json, Socket socket, LANLink.ConnectionStarted connectionStarted) throws IOException {
        String serverID = json.getString("clientID");
        LANLink currentLink = visibleServers.get(serverID);
        if (currentLink != null) {
            Log.i(getClass().getSimpleName(), "Re Using Same Link for Server ID: " + serverID);
//            if (currentLink.isConnected() && (currentLink.getConnectionSource() != connectionStarted)) {
//                Log.i(getClass().getSimpleName(), "Already connected to server, reverse connection not needed.");
//                return;
//            }
            final Socket oldSocket = currentLink.reset(socket, connectionStarted);
        } else {
            Log.i(getClass().getSimpleName(), "Creating a new Link for Server ID: " + serverID);
            LANLink link = new LANLink(serverID, this, socket, connectionStarted);
            visibleServers.put(serverID, link);
            connectionAccepted(json, link);
        }
    }

    private void setupUDPListener() {
        Log.i(getClass().getSimpleName(), "Creating the UDP Listener Server...");
        try {
            udpServer = new DatagramSocket(MIN_PORT);
            udpServer.setReuseAddress(true);
            udpServer.setBroadcast(true);
        } catch (SocketException e) {
            Log.e(getClass().getSimpleName(), "Error Creating the UDP Server.");
            e.printStackTrace();
            return;
        }
        new Thread(() -> {
            while (listening) {
                final int bufferSize = 524288;
                byte[] data = new byte[bufferSize];
                DatagramPacket packet = new DatagramPacket(data, bufferSize);
                try {
                    udpServer.receive(packet);
                    UDPPacketReceived(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.i(getClass().getSimpleName(), "Stopping UDP Listener...");
        }).start();
    }

    private void setupTCPListener() {
        Log.i(getClass().getSimpleName(), "Creating the TCP Listener Server...");
        try {
            tcpServer = openTCPServerOnFreePort();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        new Thread(() -> {
            while (listening) {
                try {
                    Socket socket = tcpServer.accept();
                    configureSocket(socket);
                    TCPPacketReceived(socket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.i(getClass().getSimpleName(), "Stopping TCP Listener...");
        }).start();
    }

    private void broadcastUdpPacket() {
        Log.i(getClass().getSimpleName(), "Broadcasting Identity Packet to 255.255.255.255");
        new Thread(() -> {
//            String broadcast = "255.255.255.255";
            try {
                MulticastSocket socket = new MulticastSocket(8657);
//            socket.setBroadcast(true);
                InetAddress intIP = InetAddress.getByName("230.1.1.1");
                socket.joinGroup(intIP);

                JSONConverter passportPacket = PacketType.makeIdentityPacket();
                byte[] sendData = passportPacket.serialize().getBytes();
                int port = (tcpServer == null || !tcpServer.isBound()) ? MIN_PORT : tcpServer.getLocalPort();
                passportPacket.set("tcpPort", port);


                Enumeration interfaces;
                interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                    if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.getDisplayName().contains("radio")) {
                        continue; // Don't want to broadcast to the loopback interface
                    }

                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast == null) {
                            continue;
                        }

                        // Send the broadcast package!
                        try {
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, MIN_PORT);
                            socket.send(sendPacket);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Log.i(getClass().getSimpleName(), "Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                    }
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

//            DatagramSocket socket = null;
//            byte[] bytes = null;
//            try {
//                socket = new DatagramSocket();
//                socket.setReuseAddress(true);
//                socket.setBroadcast(true);
//                bytes = passportPacket.serialize().getBytes();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            if (bytes != null) {
//                try {
//                    InetAddress server = InetAddress.getByName(broadcast);
//                    socket.send(new DatagramPacket(bytes, bytes.length, server, MIN_PORT));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }


        }).start();
    }

    void connectionAccepted(JSONConverter json, LANLink link) {
        for (ConnectionReceiver cr : connectionReceivers) {
            cr.onConnectionReceived(json, link);
        }
    }

    void connectionLost(LANLink link) {
        for (ConnectionReceiver cr : connectionReceivers) {
            cr.onConnectionLost(link);
        }
    }

    public void onStart() {
        Log.i(getClass().getSimpleName(), "Starting LANLINK PROVIDER");
        if (!listening) {
            listening = true;
            setupTCPListener();
            setupUDPListener();
            broadcastUdpPacket();
        }
    }

    public void onStop() {
        Log.e("LANLINKPROVIDER", "Stopping LANLINK PROVIDER");
        listening = false;
        try {
            tcpServer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            udpServer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onNetworkChange() {
        try {
            throw new Exception("LLP ON NETWORK CHANGE");
        } catch (Exception e) {
            e.printStackTrace();
        }
        broadcastUdpPacket();
    }

    public String getName() {
        return "LanLinkProvider";
    }

    public interface ConnectionReceiver {
        void onConnectionReceived(JSONConverter identityPacket, LANLink link);

        void onConnectionLost(LANLink link);
    }
}
