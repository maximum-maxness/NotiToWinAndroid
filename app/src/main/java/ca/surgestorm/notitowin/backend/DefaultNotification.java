package ca.surgestorm.notitowin.backend;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.util.Log;

import ca.surgestorm.notitowin.ui.MainActivity;

public class DefaultNotification extends Notification {

    private boolean isClearable;
    private String id, packageName, appName, title, text, time;
    private Icon largeIcon, smallIcon;

    public DefaultNotification(String id) {
        this.id = id;
        this.appName = "";
        this.packageName = "";
        this.title = "";
        this.text = "";
    }


    public Icon getIcon() {
        if (largeIcon == null) return smallIcon;
        else return largeIcon;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
        Context context = MainActivity.getAppContext();
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            this.appName = pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ActiveNotiProcessor", "Couldn't get name of package: " + packageName);
        }
    }

    public String getAppName() {
        return appName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isClearable() {
        return isClearable;
    }

    public void setClearable(boolean clearable) {
        isClearable = clearable;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Icon getLargeIcon() {
        return this.largeIcon;
    }

    public void setLargeIcon(Icon largeIcon) {
        this.largeIcon = largeIcon;
    }

    public Icon getSmallIcon() {
        return this.smallIcon;
    }

    public void setSmallIcon(Icon smallIcon) {
        this.smallIcon = smallIcon;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
