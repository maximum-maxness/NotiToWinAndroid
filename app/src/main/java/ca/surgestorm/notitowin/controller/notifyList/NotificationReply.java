package ca.surgestorm.notitowin.controller.notifyList;

import android.app.PendingIntent;

import java.util.ArrayList;
import java.util.UUID;

public class NotificationReply {
    final ArrayList<android.app.RemoteInput> remoteInputs = new ArrayList<>();
    final String id = UUID.randomUUID().toString();
    PendingIntent pendingIntent;
    String packageName, tag;
}
