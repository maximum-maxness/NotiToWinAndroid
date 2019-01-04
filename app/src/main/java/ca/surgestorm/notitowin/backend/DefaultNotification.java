package ca.surgestorm.notitowin.backend;

import android.app.Application;

public class DefaultNotification {

    private Application app;

    public String getAppName() {
        String s = app.getPackageName();
        return s;
    }
}
