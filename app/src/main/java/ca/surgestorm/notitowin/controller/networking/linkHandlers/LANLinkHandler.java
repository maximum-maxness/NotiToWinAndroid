package ca.surgestorm.notitowin.controller.networking.linkHandlers;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.controller.networking.helpers.PacketType;
import ca.surgestorm.notitowin.controller.networking.helpers.RSAHelper;

public class LANLinkHandler { // TODO Finish and Implement

    private final PairingHandlerCallback pairingHandlerCallback;
    private final Server server;
    private PairStatus pairStatus;
    private Timer pairingTimer;

    public LANLinkHandler(Server server, PairingHandlerCallback callback) {
        this.pairingHandlerCallback = callback;
        this.server = server;
        if (server.isPaired()) {
            pairStatus = PairStatus.Paired;
        } else {
            pairStatus = PairStatus.NotPaired;
        }
    }

    public void requestPairing() {
        Server.SendPacketStatusCallback callback =
                new Server.SendPacketStatusCallback() {

                    @Override
                    public void onSuccess() {
                        pairingTimer = new Timer();
                        pairingTimer.schedule(
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        pairingHandlerCallback.pairingFailed("Timed Out");
                                        pairStatus = PairStatus.NotPaired;
                                    }
                                },
                                30000);
                        pairStatus = PairStatus.Requested;
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        pairingHandlerCallback.pairingFailed("Cant send packet.");
                    }
                };
        server.sendPacket(createPairPacket(), callback);
    }

    public void unpair() {
        System.out.println("UnPairing Server ID: " + server.getServerID());
        pairStatus = PairStatus.NotPaired;
        JSONConverter json = new JSONConverter(PacketType.PAIR_REQUEST);
        json.set("pair", false);
        server.sendPacket(json);
    }

    public void acceptPairing() {
        System.out.println("Accepting the Pair for Server ID: " + server.getServerID());
        Server.SendPacketStatusCallback callback =
                new Server.SendPacketStatusCallback() {

                    @Override
                    public void onSuccess() {
                        pairingDone();
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        pairingHandlerCallback.pairingFailed("Not Reachable.");
                    }
                };
        server.sendPacket(createPairPacket(), callback);
    }

    public void rejectPairing() {
        System.out.println("Rejecting Pair for Server ID: " + server.getServerID());
        pairStatus = PairStatus.NotPaired;
        JSONConverter json = new JSONConverter(PacketType.PAIR_REQUEST);
        json.set("pair", false);
        server.sendPacket(json);
    }

    public boolean isPaired() {
        return pairStatus == PairStatus.Paired;
    }

    public boolean isPairRequested() {
        return pairStatus == PairStatus.Requested;
    }

    public boolean isPairRequestedByPeer() {
        return pairStatus == PairStatus.RequestedByPeer;
    }

    private JSONConverter createPairPacket() {
        JSONConverter json = new JSONConverter(PacketType.PAIR_REQUEST);
        json.set("pair", true);
        String pubKeyStr = null;
        try {
            PublicKey pubKey = RSAHelper.getPublicKey();
            pubKeyStr = pubKey.toString();
        } catch (Exception e) {
            e.printStackTrace();
            pubKeyStr = "";
        }
        String publicKeyFormatted =
                "-----BEGIN PUBLIC KEY-----\n" + pubKeyStr.trim() + "\n-----END PUBLIC KEY-----\n";
        json.set("publicKey", publicKeyFormatted);
        return json;
    }

    public void packageReceived(JSONConverter json) {
        System.out.println("Package Received for Server ID: " + server.getServerID());
        boolean wantsToPair = json.getBoolean("pair");

        if (wantsToPair == isPaired()) {
            if (pairStatus == PairStatus.Requested) {
                pairStatus = PairStatus.NotPaired;
                pairingHandlerCallback.pairingFailed("Nope");
            }
            return;
        }

        if (wantsToPair) {
            System.out.println("Server wants to Pair.");
            try {
                String publicKeyStr = json.getString("publicKey").replace("-----BEGIN PUBLIC KEY-----\n", "").replace("-----END PUBLIC KEY-----\n", "");
                byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
                server.publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            } catch (Exception ignored) {
            }

            if (pairStatus == PairStatus.Requested) {
                pairingDone();
            } else {
                if (server.isPaired()) {
                    acceptPairing();
                    return;
                }

                //TODO Implement Choosing Whether or not to accept pair

                pairStatus = PairStatus.RequestedByPeer;
                pairingHandlerCallback.incomingRequest();
            }
        } else {
            System.out.println("Unpair Request from server ID: " + server.getServerID());
            if (pairStatus == PairStatus.Requested) {
                pairingHandlerCallback.pairingFailed("Cancelled");
            } else if (pairStatus == PairStatus.Paired) {
                pairingHandlerCallback.unpaired();
            }

            pairStatus = PairStatus.NotPaired;
        }
    }

    private void pairingDone() {
//        if (server.publicKey != null) { //TODO Remember Previously Paired Servers
//            try {
//                String encodedPublicKey = Base64.getEncoder().encodeToString(server.publicKey.getEncoded());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            try {
//                String encodedCertificate =
//                        Base64.getEncoder().encodeToString(server.certificate.getEncoded());
//            } catch (NullPointerException n) {
//                System.err.println("No Certificate Exists!");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

        pairStatus = PairStatus.Paired;
        pairingHandlerCallback.pairingDone();
//        }
    }

    public enum PairStatus {
        NotPaired,
        Requested,
        RequestedByPeer,
        Paired
    }

    public interface PairingHandlerCallback {
        void incomingRequest();

        void pairingDone();

        void pairingFailed(String error);

        void unpaired();
    }
}
