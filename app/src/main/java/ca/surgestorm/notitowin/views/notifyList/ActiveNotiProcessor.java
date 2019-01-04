package ca.surgestorm.notitowin.views.notifyList;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ca.surgestorm.notitowin.runner.MainWindow;

public class ActiveNotiProcessor implements NotificationCollector.NotificationListener {

    private static final int REQUEST_CODE = 8567;
    private static final int NOTIFICATION_GROUP_SUMMARY_ID = 1;

    private PendingIntent intent;
    private boolean isReady = false;

    private void sendCurrentNotifications(NotificationCollector manager) {
        StatusBarNotification[] activeNotis = manager.getActiveNotifications(); //Get all of the active notifications at the time
        for (StatusBarNotification barNotification : activeNotis) { //Iterate through the notifications one at a time
            sendNotification(barNotification); //Send each notification to be processed further
        }
    }

    public boolean onCreate() {
        return false;
    }

    public void onDestroy() {

    }

    public void sendNotification(StatusBarNotification statusBarNotification) {
        Notification notification = statusBarNotification.getNotification(); //get the actual notification

        //Filter out notifications that we don't want
        if ((notification.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0
                || (notification.flags & Notification.FLAG_LOCAL_ONLY) != 0
                || (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0
                || (notification.flags & NotificationCompat.FLAG_GROUP_SUMMARY) != 0) {
            return; //If the notification is of any of the types we do not want, then skip over it
        }

        String key = statusBarNotification.getKey(); //Get Notification Key
        String packageName = statusBarNotification.getPackageName(); //Get the package name that made the notification

        //Getting the Actual (non-package) name of the program that created the notification
        String appName = "";
        Context context = MainWindow.getAppContext();
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            appName = pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ActiveNotiProcessor", "Couldn't get name of package: " + packageName);
        }


    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {

    }

    @Override
    public void onListenerConnected(NotificationCollector service) {
        isReady = true; //Change status to ready, once the listener for notifications is connected
        sendCurrentNotifications(service); //send the current notifications at the time
    }
}
