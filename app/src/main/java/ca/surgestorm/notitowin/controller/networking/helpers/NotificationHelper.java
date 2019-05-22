package ca.surgestorm.notitowin.controller.networking.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

public class NotificationHelper {

    public static NotificationChannel persistentChannel;

    public static void initChannels(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        persistentChannel = new NotificationChannel(
                "persistent",
                "Persistent Notification Channel",
                NotificationManager.IMPORTANCE_MIN);

        if (manager != null) {
            manager.createNotificationChannel(persistentChannel);


            manager.createNotificationChannel(new NotificationChannel(
                    "receive_notification",
                    "Notifications from Desktop",
                    NotificationManager.IMPORTANCE_DEFAULT)
            );
        }
    }
}
