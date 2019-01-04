package ca.surgestorm.notitowin.backend;

import android.app.Application;

public class Notification {

    private Application app;

    public String getAppName() {
        String s = app.getPackageName().toString();
        return s;
    }
}
