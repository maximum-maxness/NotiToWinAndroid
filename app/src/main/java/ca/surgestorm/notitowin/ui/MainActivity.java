package ca.surgestorm.notitowin.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import ca.surgestorm.notitowin.BackgroundService;
import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.IPGetter;
import ca.surgestorm.notitowin.backend.Server;

public class MainActivity extends AppCompatActivity implements RecyclerViewClickListener {

    RecyclerView recyclerView;
    private static Context appContext;
    public static final int RESULT_NEEDS_RELOAD = Activity.RESULT_FIRST_USER;

    public static Context getAppContext() {
        return appContext;
    }

    //    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serverlist_activity);

        appContext = getApplicationContext();

//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);

        for (String s : IPGetter.getInternalIP(true))
            Log.i("INTERNAL IP", s);

        configureRefreshButton();
//        configureNextButton();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Intent i = new Intent(this, BackgroundService.class);
        appContext.startForegroundService(i);
        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            service.changePersistentNotificationVisibility(true);
            service.setUpdater(new ServerListUpdater(this, service.getDevices().values(), this));
            recyclerView.setAdapter(service.getUpdater());
        });
    }

    private void configureRefreshButton() { //TODO Different Threads for different activities
        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> {
            Log.i("MainActivity", "Refresh Button Clicked!");
            BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
                service.cleanDevices();
                service.getUpdater().notifyDataSetChanged();
            });
        });
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {

        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            final Server server = (Server) service.getDevices().values().toArray()[position];
            if (server != null)
                if (!server.isPaired()) {
                    if (server.isPairRequestedByPeer()) {
                        server.acceptPairing();
                    } else {

                        server.requestPairing();
                    }
                }
        });

    }
}
