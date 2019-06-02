package ca.surgestorm.notitowin.backend;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import ca.surgestorm.notitowin.BackgroundService;
import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.helpers.PacketType;
import ca.surgestorm.notitowin.backend.helpers.RSAHelper;
import ca.surgestorm.notitowin.backend.helpers.SSLHelper;
import ca.surgestorm.notitowin.controller.networking.linkHandlers.LANLink;
import ca.surgestorm.notitowin.controller.networking.linkHandlers.LANLinkHandler;
import ca.surgestorm.notitowin.ui.MainActivity;
import ca.surgestorm.notitowin.ui.ServerListFragment;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server implements LANLink.PacketReceiver {

    private final CopyOnWriteArrayList<LANLink> links = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PairingCallback> pairingCallback = new CopyOnWriteArrayList<>();
    private final Map<String, LANLinkHandler> pairingHandlers = new HashMap<>();
    private final SendPacketStatusCallback defaultCallback =
            new SendPacketStatusCallback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(Throwable e) {
                    e.printStackTrace();
                }
            };
    private final String clientID;
    private final SharedPreferences settings;
    private final Context context;
    public PublicKey publicKey;
    private String osName;
    private String osVer;
    private String networkIP;
    private Certificate certificate;
    private String name;
    private PairStatus pairStatus;
    private List<Notification> notifications = new ArrayList<>();

    public Server(Context context, JSONConverter json, LANLink link) {
        this.context = context;
        this.clientID = json.getString("clientID", "Unknown");
        this.name = "Unknown";
        this.osName = "Unknown";
        this.osVer = "0.0";
        this.pairStatus = PairStatus.NotPaired;
        this.publicKey = null;
        settings = context.getSharedPreferences(clientID, Context.MODE_PRIVATE);
        addLink(json, link);
    }


    public Server(Context context, String deviceId) {
        settings = context.getSharedPreferences(deviceId, Context.MODE_PRIVATE);
        this.context = context;
        this.clientID = deviceId;
        this.name = settings.getString("deviceName", "Unknown");
        this.osName = settings.getString("osName", "Unknown");
        this.osVer = settings.getString("osVersion", "xx");
        this.pairStatus = PairStatus.Paired;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVer() {
        return osVer;
    }

    public String getIP() {
        if (networkIP == null) {
            String network = "";
            for (LANLink link : links) {
                network = link.getIP().getHostAddress();
            }
            networkIP = network;
        }
        return networkIP;
    }

    public int getPreviewImage() {
        switch (osName.toLowerCase()) {
            case "windows":
                if (osVer.equals("10")) {
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

    public String getName() {
        return name;
    }

    public PairStatus getPairStatus() {
        return pairStatus;
    }

    @Override
    public void onPacketReceived(JSONConverter json) {
        System.out.println("Received Packet of Type: " + json.getType());
        if (PacketType.PAIR_REQUEST.equals(json.getType())) {
            System.out.println("Pair Packet!");
            for (LANLinkHandler llh : pairingHandlers.values()) {
                try {
                    llh.packageReceived(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (isPaired()) {
            //Case while paired, will implement later.
        } else {
            unpair();
        }
    }

    public String getServerID() {
        return this.clientID;
    }

    public boolean isPaired() {
        return this.pairStatus == PairStatus.Paired;
    }

    public boolean isReachable() {
        return !links.isEmpty();
    }

    public boolean isConnected() {
        boolean b = false;
        for (LANLink link : links) {
            System.out.println("Link is connected?" + (b = link.isConnected()));
        }
        return b;
    }

    public void acceptPairing() {
        for (LANLinkHandler llh : pairingHandlers.values()) {
            llh.acceptPairing();
        }
    }

    public void requestPairing() {


        if (isPaired()) {
            for (PairingCallback cb : pairingCallback) {
                cb.pairingFailed("Already Paired!");
            }
            return;
        }

        if (!isReachable()) {
            for (PairingCallback cb : pairingCallback) {
                cb.pairingFailed("Not Reachable.");
            }
            return;
        }

        for (LANLinkHandler llh : pairingHandlers.values()) {
            llh.requestPairing();
        }

    }

    public void unpair() {

        for (LANLinkHandler llh : pairingHandlers.values()) {
            llh.unpair();
        }
        unpairInternal(); // Even if there are no pairing handlers, unpair
    }

    public boolean deviceShouldBeKeptAlive() {
        for (LANLink l : links) {
            if (l.linkShouldBeKeptAlive()) {
                return true;
            }
        }
        return false;
    }

    public void addLink(JSONConverter identityPacket, LANLink link) {
        System.out.println("Adding Link to Client ID: " + clientID);
        if (identityPacket.has("clientName")) {
            this.name = (identityPacket.getString("clientName"));
        }
        if (identityPacket.has("osName")) {
            this.osName = identityPacket.getString("osName");
        }
        if (identityPacket.has("certificate")) {
            System.out.println("Packet has a certificate!");
            String certificateString = identityPacket.getString("certificate");
            try {
                System.out.println("Parsing Certificate...");
                byte[] certificateBytes = Base64.getDecoder().decode(certificateString);
                certificate = SSLHelper.parseCertificate(certificateBytes);
            } catch (Exception e) {
                System.err.println("Error Parsing Certificate.");
                e.printStackTrace();
            }
        }

        links.add(link);

        try {
            System.out.println("Setting Private Key..");
            PrivateKey privateKey = RSAHelper.getPrivateKey(context);
            link.setPrivateKey(privateKey);
            System.out.println("Set Private Key Successfully!");
        } catch (Exception e) {
            System.err.println("Error setting private key!");
            e.printStackTrace();
        }

        if (!pairingHandlers.containsKey(link.getName())) {
            LANLinkHandler.PairingHandlerCallback callback =
                    new LANLinkHandler.PairingHandlerCallback() {
                        @Override
                        public void incomingRequest() {
                            for (PairingCallback pb : pairingCallback) {
                                pb.incomingRequest(Server.this);
                            }
                        }

                        @Override
                        public void pairingDone() {
                            Server.this.pairingDone();
                        }

                        @Override
                        public void pairingFailed(String error) {
                            for (PairingCallback pb : pairingCallback) {
                                pb.pairingFailed(error);
                            }
                        }

                        @Override
                        public void unpaired() {
                            unpairInternal();
                        }
                    };
            pairingHandlers.put(link.getName(), link.getPairingHandler(this, callback));
            link.addPacketReceiver(this);
        }

//        link.addPacketReceiver(this);
//        requestThread = new Thread(() -> {
//            String sendTestString = Main.getDecision("Request Pairing? Y/N");
//            sendTestString = sendTestString.toUpperCase();
//            if (sendTestString.equals("Y")) {
//                requestPairing();
//            } else {
//                unpair();
//            }
//        });
//        requestThread.start();
        ServerListFragment.fragmentHandler.post(() -> {
            BackgroundService.RunCommand(MainActivity.getAppContext(), (service) -> {
                service.getUpdater().notifyDataSetChanged();
            });
        });
    }

    public void removeLink(LANLink link) {
        System.out.println("Removing link: " + link.getName());
        boolean linkExists = false;
        for (LANLink llink : links) {
            if (llink.getName().equals(link.getName())) {
                linkExists = true;
                break;
            }
        }
        if (!linkExists) {
            pairingHandlers.remove(link.getName());
        }

        link.removePacketReceiver(this);
        links.remove(link);
    }

    private void pairingDone() {
        System.out.println("Pairing was a success!!!");
        pairStatus = (PairStatus.Paired);
        for (PairingCallback pb : pairingCallback) {
            pb.pairingSuccessful(this);
        }
    }

    private void unpairInternal() {
        System.out.println("Forcing an Unpair..");
        pairStatus = (PairStatus.NotPaired);
        for (PairingCallback pb : pairingCallback) {
            pb.unpaired();
        }
    }

    public void disconnect() {
        for (LANLink link : links) {
            link.disconnect();
        }
    }

    public void sendPacket(JSONConverter json) {
        sendPacket(json, defaultCallback);
    }

    public void sendPacket(final JSONConverter json, final SendPacketStatusCallback callback) {
        new Thread(() -> sendPacketBlocking(json, callback)).start();
    }

    public boolean sendPacketBlocking(final JSONConverter json, final SendPacketStatusCallback callback) {
        System.out.println("Sending Packet to all Applicable Links!");
        boolean shouldUseEncryption = (!json.getType().equals(PacketType.PAIR_REQUEST) && isPaired());
        boolean success = false;
        for (final LANLink link : links) {
            if (link == null) continue;
            if (shouldUseEncryption) {
                success = link.sendPacket(json, callback, publicKey);
            } else {
                success = link.sendPacket(json, callback);
            }
            if (success) break;
        }
        System.out.println("Packet send success? " + success);
        return success;
    }

    public boolean isPairRequested() {
        boolean pairRequested = false;
        for (LANLinkHandler llh : pairingHandlers.values()) {
            pairRequested = pairRequested || llh.isPairRequested();
        }
        System.out.println("Is pair Requested? " + pairRequested);
        return pairRequested;
    }

    public boolean isPairRequestedByPeer() {
        boolean pairRequestedByPeer = false;
        boolean paired = false;
        for (LANLinkHandler llh : pairingHandlers.values()) {
            pairRequestedByPeer = pairRequestedByPeer || llh.isPairRequestedByPeer();
            paired = paired || llh.isPaired();
        }
        System.out.println("Is pair requested by peer? " + pairRequestedByPeer + ", is Paired? " + paired);
        return pairRequestedByPeer;
    }

    public void addPairingCallback(PairingCallback callback) {
        pairingCallback.add(callback);
    }

    public void removePairingCallback(PairingCallback callback) {
        pairingCallback.remove(callback);
    }

    public enum PairStatus {
        NotPaired,
        Paired
    }

    public interface PairingCallback {
        void incomingRequest(Server server);

        void pairingSuccessful(Server server);

        void pairingFailed(String error);

        void unpaired();
    }

    public abstract static class SendPacketStatusCallback {
        public abstract void onSuccess();

        public abstract void onFailure(Throwable e);

        public void onProgressChanged(int percent) {
        }
    }
}
