package ca.surgestorm.notitowin.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import ca.surgestorm.notitowin.BackgroundService;
import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.backend.helpers.IPHelper;
import ca.surgestorm.notitowin.backend.helpers.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    RecyclerView recyclerView;
    private static Context appContext;
    private Fragment serverListFragment, notiListFragment;
    private Toolbar toolbar;
    public static final int RESULT_NEEDS_RELOAD = Activity.RESULT_FIRST_USER;
    public static Context getAppContext() {
        return appContext;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
        switch (item.getItemId()) {
            case R.id.server_list_view:
                setContentFragment("serverList");
                return true;
            case R.id.notification_list_view:
                setContentFragment("notiList");
                return true;
            case R.id.settings_view:
//                        setContentFragment("settings");
                return true;
        }
        return false;
    };

    //    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) { //TODO Remember What Device was selected
        setTheme(R.style.Theme_AppCompat_DayNight_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        appContext = getApplicationContext();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);

        for (String s : IPHelper.getInternalIP(true))
            Log.i("INTERNAL IP", s);

        initFragments();

        NotificationHelper.setPersistentNotificationEnabled(this, true);
        Intent i = new Intent(this, BackgroundService.class);
        appContext.startForegroundService(i);
        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            service.changePersistentNotificationVisibility(true);
        });
    }

    public void initFragments() {
        FragmentManager manager = getSupportFragmentManager();

        serverListFragment = new ServerListFragment();
        notiListFragment = new NotiListFragment();

        manager.beginTransaction().add(R.id.container, serverListFragment, "serverList").commit();
        manager.beginTransaction().add(R.id.container, notiListFragment, "notiList").commit();
        manager.beginTransaction().hide(notiListFragment).commit();
        manager.beginTransaction().show(serverListFragment).commit();
    }

    public Fragment getVisibleFragment() {
        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible())
                return fragment;
        }
        return null;
    }

    private String onPairResultFromNotification(String serverID, String pairStatus) {
        assert (serverID != null);

        if (!pairStatus.equals("pending")) {
            BackgroundService.RunCommand(this, service -> {
                Server server = service.getServer(serverID);
                if (server == null) {
                    Log.w("rejectPairing", "Device no longer exists: " + serverID);
                    return;
                }

                if (pairStatus.equals("accepted")) {
                    server.acceptPairing();
                } else if (pairStatus.equals("rejected")) {
                    server.rejectPairing();
                }
            });
        }

        if (pairStatus.equals("rejected") || pairStatus.equals("pending")) {
            return serverID;
        } else {
            return null;
        }
    }

    private void setContentFragment(String tag) {
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().hide(getVisibleFragment()).commit();
        manager.beginTransaction().show(Objects.requireNonNull(manager.findFragmentByTag(tag))).commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
}
