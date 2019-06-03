package ca.surgestorm.notitowin.backend.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

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
                    "default",
                    "Misc Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT)
            );


            manager.createNotificationChannel(new NotificationChannel(
                    "receive_notification",
                    "Notifications from Desktop",
                    NotificationManager.IMPORTANCE_DEFAULT)
            );
        }
    }

    public static void setPersistentNotificationEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("persistentNotification", enabled).apply();
    }

    public static boolean isPersistentNotificationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return true;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("persistentNotification", false);
    }

    public static void notifyCompat(NotificationManager notificationManager, int notificationId, Notification notification) {
        try {
            notificationManager.notify(notificationId, notification);
        } catch (Exception ignored) {

        }
    }
}
