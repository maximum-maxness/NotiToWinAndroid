package ca.surgestorm.notitowin;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.controller.networking.helpers.NotificationHelper;
import ca.surgestorm.notitowin.controller.networking.helpers.RSAHelper;
import ca.surgestorm.notitowin.controller.networking.helpers.SSLHelper;
import ca.surgestorm.notitowin.controller.networking.linkHandlers.LANLink;
import ca.surgestorm.notitowin.controller.networking.linkHandlers.LANLinkProvider;
import ca.surgestorm.notitowin.controller.notifyList.ActiveNotiProcessor;
import ca.surgestorm.notitowin.ui.MainActivity;
import ca.surgestorm.notitowin.ui.NotiListActivity;
import ca.surgestorm.notitowin.ui.ServerListUpdater;

@SuppressLint("Registered")
public class BackgroundService extends Service {
    private final static ArrayList<InstanceCallback> callbacks = new ArrayList<>();
    private final static Lock mutex = new ReentrantLock(true);
    private final ArrayList<LANLinkProvider> linkProviders = new ArrayList<>();


    private ServerListUpdater updater;
    private final ConcurrentHashMap<String, Server> servers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DeviceListChangedCallback> serverListChangedCallbacks =
            new ConcurrentHashMap<>();
    private final Server.PairingCallback devicePairingCallback =
            new Server.PairingCallback() {
                @Override
                public void incomingRequest(Server server) {
                    System.err.println("Incoming from Server ID: " + server.getServerID());
                    Server.SendPacketStatusCallback callback = new Server.SendPacketStatusCallback() {
                        @Override
                        public void onSuccess() {
                            server.requestPairing();
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            server.unpair();
                        }
                    };
                }

                @Override
                public void pairingSuccessful(Server server) {
//          String decision = runner.Main.getDecision("Choose A message to send!");
//          JSONConverter json = new JSONConverter(PacketType.NOTIFICATION);
//          json.put("message", decision);
//          for (Server server : servers.values()) {
//            server.sendPacket(json);
//          }
                    startActivity(new Intent(MainActivity.getAppContext(), NotiListActivity.class));
                }

                @Override
                public void pairingFailed(String error) {
                    onServerListChanged();
                }

                @Override
                public void unpaired() {
                    onServerListChanged();
                }
            };
    private final LANLinkProvider.ConnectionReceiver deviceListener =
            new LANLinkProvider.ConnectionReceiver() {
                @Override
                public void onConnectionReceived(JSONConverter identityPacket, LANLink link) {
                    String serverID = identityPacket.getString("serverID");
                    Server server;
                    try {
                        server = servers.get(serverID);
                    } catch (NullPointerException ignored) {
                        server = null;
                    }
                    if (server != null) {
                        server.addLink(identityPacket, link);
                    } else {
                        server = new Server(BackgroundService.this, identityPacket, link);
                        if (server.isPaired()
                                || server.isPairRequested()
                                || server.isPairRequestedByPeer()
                                || link.linkShouldBeKeptAlive()) {
                            servers.put(serverID, server);
                            server.addPairingCallback(devicePairingCallback);
                        } else {
                            server.disconnect(); // TODO Implement decision making on whether to accept the pair or
                            // not, stop it from disconnecting regardless.
                        }
                    }
                    onServerListChanged();
                }

                @Override
                public void onConnectionLost(LANLink link) {
                    Server server = servers.get(link.getServerID());
                    if (server != null) {
                        server.removeLink(link);
                        if (!server.isReachable() && !server.isPaired()) {
                            servers.remove(server);
                            server.removePairingCallback(devicePairingCallback);
                        }
                    }
                    onServerListChanged();
                }
            };
    private ActiveNotiProcessor anp;

    public static void initSecurity() {
        RSAHelper.initKeys();
        SSLHelper.initCertificate();
    }

    private static void Start(Context c) {
        RunCommand(c, null);
    }

    public ServerListUpdater getUpdater() {
        return updater;
    }

    public void setUpdater(ServerListUpdater updater) {
        this.updater = updater;
    }

    public static void RunCommand(final Context c, final InstanceCallback callback) {
        new Thread(() -> {
            if (callback != null) {
                mutex.lock();
                try {
                    callbacks.add(callback);
                } finally {
                    mutex.unlock();
                }
            }
            Intent serviceIntent = new Intent(c, BackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                c.startForegroundService(serviceIntent);
            } else {
                c.startService(serviceIntent);
            }
        }).start();
    }

    public void cleanDevices() {
        new Thread(() -> {
            for (Server server : servers.values()) {
                if (!server.isPaired() && !server.isPairRequested() && !server.isPairRequestedByPeer() && !server.deviceShouldBeKeptAlive()) {
                    server.disconnect();
                }
            }
        }).start();
    }

    public void changePersistentNotificationVisibility(boolean visible) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (visible) {
            Objects.requireNonNull(nm).notify(1, createForegroundNotification());
        } else {
            stopForeground(true);
            Start(this);
        }
    }

    private Notification createForegroundNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder notification = new Notification.Builder(this, NotificationHelper.persistentChannel.getId());
        notification
                .setSmallIcon(R.drawable.ic_launcher_foreground) //TODO Make actual icon
                .setOngoing(true)
                .setContentIntent(pi)
                .setShowWhen(false)
                .setAutoCancel(false);
        notification.setGroup("BackgroundService");

        ArrayList<String> connectedServers = new ArrayList<>();
        for (Server server : getDevices().values()) {
            if (server.isReachable() && server.isPaired()) {
                connectedServers.add(server.getName());
            }
        }

        if (connectedServers.isEmpty()) {
            notification.setContentText("No Servers Connected!");
        } else {
            notification.setContentText("Connected to Servers:" + TextUtils.join(", ", connectedServers));
        }

        return notification.build();
    }

    private void loadRememberedDevicesFromSettings() {
        //Log.e("BackgroundService", "Loading remembered trusted devices");
        SharedPreferences preferences = getSharedPreferences("trusted_devices", Context.MODE_PRIVATE);
        Set<String> trustedDevices = preferences.getAll().keySet();
        for (String deviceId : trustedDevices) {
            //Log.e("BackgroundService", "Loading device "+deviceId);
            if (preferences.getBoolean(deviceId, false)) {
                Server server = new Server(this, deviceId);
                servers.put(deviceId, server);
                server.addPairingCallback(devicePairingCallback);
            }
        }
    }

    public ConcurrentHashMap<String, Server> getDevices() {
        return servers;
    }

    public void onNetworkChange() {
        for (LANLinkProvider a : linkProviders) {
            a.onNetworkChange();
        }
    }

    private void registerLinkProviders() {
        linkProviders.add(new LANLinkProvider());
    }

    public Server getServer(String id) {
        return servers.get(id);
    }

    void onServerListChanged() {
        for (DeviceListChangedCallback callback : serverListChangedCallbacks.values()) {
            callback.onDeviceListChanged();
        }
        updater.notifyDataSetChanged();
    }

    public void addConnectionListener(LANLinkProvider.ConnectionReceiver cr) {
        for (LANLinkProvider llp : linkProviders) {
            llp.addConnectionReceiver(cr);
        }
    }

    public void removeConnectionListener(LANLinkProvider.ConnectionReceiver cr) {
        for (LANLinkProvider llp : linkProviders) {
            llp.removeConnectionReceiver(cr);
        }
    }

    public void addServerListChangedCallback(String key, DeviceListChangedCallback callback) {
        serverListChangedCallbacks.put(key, callback);
    }

    public void removeServerListChangedCallback(String key) {
        serverListChangedCallbacks.remove(key);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        BackgroundService instance = this;

        // Register screen on listener
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new BroadcastReceiverNTW(), filter);

        Log.i("BackgroundService", "Service not started yet, initializing...");

        initSecurity();
        NotificationHelper.initChannels(BackgroundService.this);
        loadRememberedDevicesFromSettings();
        registerLinkProviders();

        //Link Providers need to be already registered
        addConnectionListener(deviceListener);

        for (LANLinkProvider a : linkProviders) {
            a.onStart();
        }
    }

    public void sendGlobalPacket(JSONConverter json) {
        for (Server server : servers.values()) {
            server.sendPacket(json);
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        for (LANLinkProvider a : linkProviders) {
            a.onStop();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //This will be called for each intent launch, even if the service is already started and it is reused
        mutex.lock();
        try {
            for (InstanceCallback c : callbacks) {
                c.onServiceStart(this);
            }
            callbacks.clear();
        } finally {
            mutex.unlock();
        }

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public interface DeviceListChangedCallback {
        void onDeviceListChanged();
    }

    public interface InstanceCallback {
        void onServiceStart(BackgroundService service);
    }
}
