package ca.surgestorm.notitowin.controller.notifyList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NotificationCollector extends NotificationListenerService {
    private final ArrayList<NotificationListener> listeners = new ArrayList<>();
    private boolean connected;

    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        for (NotificationListener listener : listeners) {
            listener.onNotificationPosted(statusBarNotification);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        for (NotificationListener listener : listeners) {
            listener.onNotificationRemoved(statusBarNotification);
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        for (NotificationListener listener : listeners) {
            listener.onListenerConnected(this);
        }
        connected = true;
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    public interface NotificationListener {
        void onNotificationPosted(StatusBarNotification statusBarNotification);

        void onNotificationRemoved(StatusBarNotification statusBarNotification);

        void onListenerConnected(NotificationCollector service);
    }

    private final static ArrayList<InstanceCallback> callbacks = new ArrayList<>();
    private final static Lock mutex = new ReentrantLock(true);

    public static void Start(Context c) {
        RunCommand(c, null);
    }

    public static void RunCommand(Context c, final InstanceCallback callback) {
        if (callback != null) {
            mutex.lock();
            try {
                callbacks.add(callback);
            } finally {
                mutex.unlock();
            }
        }
        Intent serviceIntent = new Intent(c, NotificationCollector.class);
        c.startService(serviceIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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

    public interface InstanceCallback {
        void onServiceStart(NotificationCollector service);
    }

}

