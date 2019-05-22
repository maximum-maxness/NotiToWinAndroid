package ca.surgestorm.notitowin.backend;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import ca.surgestorm.notitowin.controller.networking.helpers.PacketType;
import ca.surgestorm.notitowin.ui.MainActivity;

public class DefaultNotification { //TODO Rewrite DefaultNotification Class so that it properly extends Notification

    private boolean isClearable, isRepliable;
    private String id, packageName, appName, title, text, time, dataLoadHash, requestReplyId;
    private Icon largeIcon, smallIcon;
    private long timeStamp, dataLoadSize;

    public DefaultNotification(String id, long timeStamp) {
        this.id = id;
        this.timeStamp = timeStamp;
        this.appName = "";
        this.packageName = "";
        this.title = "";
        this.text = "";
        this.requestReplyId = "";
        this.isRepliable = false;
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

    private static Bitmap iconToBitmap(Icon icon) {
        if (icon == null) return null;

        Drawable draw = icon.loadDrawable(MainActivity.getAppContext());
        if (draw == null) return null;

        Bitmap bitmap;
        if ((draw.getIntrinsicWidth() > 128 || draw.getIntrinsicWidth() > 128)
                || (draw.getIntrinsicWidth() <= 64 || draw.getIntrinsicHeight() <= 64)) {
            bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(draw.getIntrinsicWidth(), draw.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        draw.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        draw.draw(canvas);
        return bitmap;
    }

    private static byte[] bitmapToByteArray(Bitmap bmp) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        byte[] bmpData = os.toByteArray();
        return bmpData;
    }

    private String getLargeIconFileSize() {
        return DataLoad.humanReadable(bitmapToByteArray(getLargeIconBitmap()).length, false);
    }

    private String getSmallIconFileSize() {
        return DataLoad.humanReadable(bitmapToByteArray(getSmallIconBitmap()).length, false);
    }

    public byte[] getIconByte(){
        if(this.largeIcon != null){
            return bitmapToByteArray(getLargeIconBitmap());
        } else {
            return bitmapToByteArray(getSmallIconBitmap());
        }
    }

    //    @Override
    public Icon getLargeIcon() {
        return this.largeIcon;
    }

    public void setLargeIcon(Icon largeIcon) {
        this.largeIcon = largeIcon;
        if (this.largeIcon == null) {
            Log.w("DefaultNotification", "Large Icon is Missing/Null!");
        } else {
            Log.i("DefaultNotification", "Large Icon Filesize for App: " + getAppName() + " is: " + getLargeIconFileSize());
        }

    }

    public Bitmap getLargeIconBitmap() {
        return iconToBitmap(this.getLargeIcon());
    }

    private InputStream getLargeIconInputStream() {
        byte[] data = bitmapToByteArray(getLargeIconBitmap());
        DataLoad dl = new DataLoad(data);
        this.dataLoadSize = bitmapToByteArray(getLargeIconBitmap()).length;
        this.dataLoadHash = DataLoad.getChecksum(data);
        return dl.getInputStream();
    }

    private InputStream getSmallIconInputStream() {
        byte[] data = bitmapToByteArray(getSmallIconBitmap());
        DataLoad dl = new DataLoad(data);
        this.dataLoadSize = bitmapToByteArray(getSmallIconBitmap()).length;
        this.dataLoadHash = DataLoad.getChecksum(data);
        return dl.getInputStream();
    }

    private InputStream getIconInputStream() {
        InputStream inputStream;
        if (this.largeIcon != null) {
            inputStream = getLargeIconInputStream();
        } else {
            inputStream = getSmallIconInputStream();
        }
        return inputStream;
    }

    //    @Override
    public Icon getSmallIcon() {
        return this.smallIcon;
    }

    public String getTime() {
        return time;
    }

    public void setSmallIcon(Icon smallIcon) {
        this.smallIcon = smallIcon;
        if (this.smallIcon == null) {
            Log.w("DefaultNotification", "Small Icon is Missing/Null!");
        } else {
            Log.i("DefaultNotification", "Small Icon Filesize for App: " + getAppName() + " is: " + getSmallIconFileSize());
        }
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Bitmap getSmallIconBitmap() {
        return iconToBitmap(this.getSmallIcon());
    }

    @Override
    public String toString() {
            return populateJSON().serialize();
    }

    public JSONConverter populateJSON() {
        JSONConverter json = new JSONConverter(PacketType.NOTIFICATION);
        json.set("id", this.id);
        json.set("isClearable", this.isClearable);
        json.set("appName", this.appName == null ? this.packageName : this.appName);
        json.set("time", Long.toString(this.timeStamp));
        json.set("title", this.title);
        json.set("text", this.text);
        if (getIconInputStream() != null && this.dataLoadHash != null) {
            json.set("hasDataLoad", true);
            json.set("dataLoadHash", this.dataLoadHash);
            json.set("dataLoadSize", this.dataLoadSize);

        } else {
            json.set("hasDataLoad", false);
        }
        if (this.isRepliable) {
            json.set("isRepliable", this.isRepliable);
            json.set("requestReplyId", this.requestReplyId);
        } else {
            json.set("isRepliable", this.isRepliable);
        }
        return json;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getRequestReplyId() {
        return requestReplyId;
    }

    public void setRequestReplyId(String requestReplyId) {
        this.requestReplyId = requestReplyId;
        this.isRepliable = true;
    }

    public boolean isRepliable() {
        return isRepliable;
    }
}
