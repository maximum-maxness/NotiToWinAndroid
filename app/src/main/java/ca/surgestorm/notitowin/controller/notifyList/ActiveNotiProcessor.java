package ca.surgestorm.notitowin.controller.notifyList;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.util.Log;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.surgestorm.notitowin.BackgroundService;
import ca.surgestorm.notitowin.backend.DefaultNotification;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.ui.MainActivity;
import ca.surgestorm.notitowin.ui.NotiListActivity;

public class ActiveNotiProcessor implements NotificationCollector.NotificationListener {

    private static final int REQUEST_CODE = 8567;
    private static final int NOTIFICATION_GROUP_SUMMARY_ID = 1;
    private final String TITLE_KEY = "android.title";
    private final String TEXT_KEY = "android.text";
    public ArrayList<DefaultNotification> activeNotis;
    private Map<String, NotificationReply> pendingIntents;
    private boolean isReady = false;
    private Set<String> currentNotis;
    private Server server;

    private static String getStringFromExtra(Bundle extras, String search) { //TODO Add Repliable Notification Support, as Well as Media Player Support (Spotify, Google Music)
        Object extra = extras.get(search);
        if (extra == null) {
            return null;
        } else if (extra instanceof String) {
            return (String) extra;
        } else if (extra instanceof SpannableString) {
            return extra.toString();
        } else {
            return null;
        }
    }

    private static String getDateFromTimestamp(long timestamp) {
        PrettyTime prettyTime = new PrettyTime();
        return prettyTime.format(new Date(timestamp));
    }

    private void sendCurrentNotifications(NotificationCollector manager) {
        StatusBarNotification[] activeNotis = manager.getActiveNotifications(); //Get all of the active notifications at the time
        for (StatusBarNotification barNotification : activeNotis) { //Iterate through the notifications one at a time
            sendNotification(barNotification); //Send each notification to be processed further
        }
    }

    public AlertDialog getErrorDialog(final Activity deviceActivity) {

        return new AlertDialog.Builder(deviceActivity)
                .setTitle("Notification Sync")
                .setMessage("You need to grant permission to access notifications")
                .setPositiveButton("Open Settings", (dialogInterface, i) -> {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    deviceActivity.startActivityForResult(intent, MainActivity.RESULT_NEEDS_RELOAD);
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    //Do nothing
                })
                .create();

    }

    private boolean hasPermission() {
        Context context = MainActivity.getAppContext();
        String notificationListenerList = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return (notificationListenerList != null && notificationListenerList.contains(context.getPackageName()));
    }

    public boolean onCreate() {
        if (!hasPermission()) {
            Log.e("ActiveNotiProcessor", "No Permissions Found!");
            return false;
        }
        this.pendingIntents = new HashMap<>();
        this.currentNotis = new HashSet<>();
        this.activeNotis = new ArrayList<>();
        Log.i("ActiveNotiProcessor", "OnCreate Started!");
        Context context = MainActivity.getAppContext();
        NotificationCollector.RunCommand(context, service -> {

            service.addListener(ActiveNotiProcessor.this);

            isReady = service.isConnected();

            if (isReady) {
                Log.e("ActiveNotiProcessor", "Notification Listener is Connected!");
                sendCurrentNotifications(service);
            }
        });
        return true;
    }

    public void onDestroy() {
        Context context = MainActivity.getAppContext();
        NotificationCollector.RunCommand(context, service -> service.removeListener(ActiveNotiProcessor.this));
    }

    public void sendNotification(StatusBarNotification statusBarNotification) {
//        Log.i("ActiveNotiProcessor", "Send Notification Started!");
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
        long timeStamp = statusBarNotification.getPostTime();
        String formattedTime = getDateFromTimestamp(timeStamp);
//        Log.e("NotiTimeFormatter", ("TimeStamp is: " + timeStamp + ", so formatted time is: " + formattedTime));


        DefaultNotification dn = new DefaultNotification(key, timeStamp);
        dn.setLargeIcon(notification.getLargeIcon());
        dn.setSmallIcon(notification.getSmallIcon());
        dn.setPackageName(packageName);
        dn.setClearable(statusBarNotification.isClearable());
        dn.setTitle(getInfoFromNoti(notification, TITLE_KEY));
        dn.setText(getInfoFromNoti(notification, TEXT_KEY));
        dn.setTime(getDateFromTimestamp(timeStamp));

        NotificationReply nr = checkIfRepliable(statusBarNotification);
        if (nr.pendingIntent != null) {
            dn.setRequestReplyId(nr.id);
            this.pendingIntents.put(nr.id, nr);
        }

        if (!currentNotis.contains(key)) {
            currentNotis.add(key);
            activeNotis.add(dn);
        }
        if (!NotiListActivity.refreshButtonPressed) {
            NotiListActivity.updateNotiArray(activeNotis);
        }
        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            service.sendGlobalPacket(dn.populateJSON());
        });
    }

    public void updateTimes() {
        if (activeNotis != null) {
            for (DefaultNotification dn : activeNotis) {
                long timeStamp = dn.getTimeStamp();
                String formattedTime = getDateFromTimestamp(timeStamp);
//            Log.e("NotiTimeUpdater", ("TimeStamp is: " + timeStamp + ", so formatted time is: " + formattedTime));
                dn.setTime(formattedTime);
            }
        } else {

        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        sendNotification(statusBarNotification);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        Notification notification = statusBarNotification.getNotification();
        String key = statusBarNotification.getKey();
        boolean beenRemoved = false;
        for (DefaultNotification dn : activeNotis) {
            if (dn.getId().equals(key)) {
                activeNotis.remove(dn);
                currentNotis.remove(key);
                beenRemoved = true;
            }
        }
        if (!beenRemoved) Log.e("NotiRemover", "Nothing has been removed.");


    }

    @Override
    public void onListenerConnected(NotificationCollector service) {
        isReady = true; //Change status to ready, once the listener for notifications is connected
        sendCurrentNotifications(service); //send the current notifications at the time
    }

    private String getInfoFromNoti(Notification notification, String type) {
        String info = "";
        if (notification != null) {
            Bundle extras = notification.extras;
            info = getStringFromExtra(extras, type);

        }
        return info;
    }

    private NotificationReply checkIfRepliable(StatusBarNotification sbn) {
        NotificationReply notificationReply = new NotificationReply();

        if (sbn != null) {
            try {
                if (sbn.getNotification().actions != null) {
                    for (Notification.Action act : sbn.getNotification().actions) {
                        if (act != null && act.getRemoteInputs() != null) {
                            notificationReply.remoteInputs.addAll(Arrays.asList(act.getRemoteInputs()));
                            notificationReply.pendingIntent = act.actionIntent;
                            break;
                        }
                    }
                    notificationReply.packageName = sbn.getPackageName();
                    notificationReply.tag = sbn.getTag();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return notificationReply;
    }
}
