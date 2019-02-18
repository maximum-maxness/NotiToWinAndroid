package ca.surgestorm.notitowin.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.controller.ServerDetector;
import ca.surgestorm.notitowin.controller.ServerSender;

public class MainActivity extends AppCompatActivity implements RecyclerViewClickListener {

    List<Server> serverList;
    RecyclerView recyclerView;
    private static ServerListUpdater updater;
    private Executor exec;
    public static ServerDetector serverDetector;
    private static Context appContext;
    public static final int RESULT_NEEDS_RELOAD = Activity.RESULT_FIRST_USER;

    public static Context getAppContext() {
        return appContext;
    }

    public static void updateList(Server server) {
        updater.getServerList().add(server);
        updater.notifyDataSetChanged();
    }

    //    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serverlist_activity);

        appContext = getApplicationContext();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        exec = Executors.newFixedThreadPool(1);
        serverDetector = ServerDetector.getInstance();
        exec.execute(serverDetector);

        configureRefreshButton();
//        configureNextButton();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        serverList = new ArrayList<>();
        updater = new ServerListUpdater(this, serverList, this);
        recyclerView.setAdapter(updater);
    }

    private void configureRefreshButton() { //TODO Different Threads for different activities
        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onClick(View v) {
                serverList.clear();
                if (!ServerDetector.isRunning()) {
                    exec.execute(serverDetector);
                } else {
                    serverDetector.stop();
                    exec.execute(serverDetector);
                }
            }
        });
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {

        try {
            ServerDetector.setIp(InetAddress.getByName(serverList.get(position).getIp()));
        } catch (UnknownHostException e) {
            Log.e("MainActivity", "Unknown Host");
        }
        startActivity(new Intent(MainActivity.this, NotiListActivity.class));
    }
}
