package ca.surgestorm.notitowin.views.notifyList;

import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.Fragment;

public class ActiveNotiProcessor extends Fragment {

    private NotificationManager manager;
    private StatusBarNotification[] activeNotis;

    public ActiveNotiProcessor() {
        this.manager = (NotificationManager) getActivity().getSystemService(
                Context.NOTIFICATION_SERVICE);
    }


}
