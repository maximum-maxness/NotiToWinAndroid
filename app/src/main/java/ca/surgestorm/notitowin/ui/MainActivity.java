package ca.surgestorm.notitowin.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import ca.surgestorm.notitowin.BackgroundService;
import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.helpers.IPHelper;
import ca.surgestorm.notitowin.backend.helpers.NotificationHelper;

public class MainActivity extends FragmentActivity {

    RecyclerView recyclerView;
    private static Context appContext;
    public static final int RESULT_NEEDS_RELOAD = Activity.RESULT_FIRST_USER;
    private final ServerListFragment serverListFragment = new ServerListFragment();

    public static Context getAppContext() {
        return appContext;
    }

    //    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        setContentFragment(serverListFragment);
        appContext = getApplicationContext();

//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);

        for (String s : IPHelper.getInternalIP(true))
            Log.i("INTERNAL IP", s);

        setContentFragment(new ServerListFragment());
        NotificationHelper.setPersistentNotificationEnabled(this, true);
        Intent i = new Intent(this, BackgroundService.class);
        appContext.startForegroundService(i);
        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            service.changePersistentNotificationVisibility(true);
        });
    }

    private void setContentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

}
