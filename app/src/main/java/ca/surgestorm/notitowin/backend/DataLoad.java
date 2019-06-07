package ca.surgestorm.notitowin.backend;

import android.annotation.SuppressLint;
import android.util.Log;
import ca.surgestorm.notitowin.backend.helpers.DataHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DataLoad {
    private InputStream inputStream;
    private Socket inputSocket;
    private long size;

    public DataLoad(byte[] data) {
        this(new ByteArrayInputStream(data), data.length);
    }

    public DataLoad(InputStream stream, long length) {
        this.inputSocket = null;
        this.inputStream = stream;
        this.size = length;
    }

    public static String getChecksum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            return DataHelper.bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e("DataLoad", "Can't get checksum");
        }
        return null;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    @SuppressLint("DefaultLocale")
    public static String humanReadable(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public void close() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (inputSocket != null) {
                inputSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public long humanReadable() {
        return this.size;
    }

    public long getSize() {
        return this.size;
    }


}
