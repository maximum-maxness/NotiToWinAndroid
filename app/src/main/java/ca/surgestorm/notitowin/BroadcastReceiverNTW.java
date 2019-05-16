package ca.surgestorm.notitowin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.Objects;

public class BroadcastReceiverNTW extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        assert action != null;
        switch (action) {
            case Intent.ACTION_PACKAGE_REPLACED:
                Log.i("BroadcastReceiverNTW", "Update broadcast received!");
                if (!Objects.requireNonNull(intent.getData()).getSchemeSpecificPart().equals(context.getPackageName())) {
                    Log.i("BroadcastReceiverNTW", "Other app is being updated!");
                    return;
                }
                BackgroundService.RunCommand(context, service -> {

                });
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                Log.i("BroadcastReceiverNTW", "BroadcastReceiverNTW Started!");
                BackgroundService.RunCommand(context, service -> {

                });
                break;
            case WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION:
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
            case ConnectivityManager.CONNECTIVITY_ACTION:
                Log.i("BroadcastReceiverNTW", "Connection state changed, trying to connect");
                BackgroundService.RunCommand(context, service -> {
                    service.onServerListChanged();
                    service.onNetworkChange();
                });
                break;
            case Intent.ACTION_SCREEN_ON:
                BackgroundService.RunCommand(context, BackgroundService::onNetworkChange);
                break;
            default:
                Log.i("BroadcastReceiver", "Ignoring broadcast event: " + intent.getAction());
                break;
        }
    }
}
