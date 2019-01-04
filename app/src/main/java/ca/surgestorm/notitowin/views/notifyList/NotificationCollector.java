package ca.surgestorm.notitowin.views.notifyList;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;

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


}
