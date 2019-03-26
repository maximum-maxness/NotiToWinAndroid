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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.IPGetter;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.controller.ServerDetector;
import ca.surgestorm.notitowin.controller.ServerSender;

public class MainActivity extends AppCompatActivity implements RecyclerViewClickListener {

    List<Server> serverList;
    RecyclerView recyclerView;
    private static ServerListUpdater updater;
    private Executor exec;
    public static ServerDetector serverDetector;
    public static ServerSender serverSender;
    private static Context appContext;
    public static final int RESULT_NEEDS_RELOAD = Activity.RESULT_FIRST_USER;

    public static Context getAppContext() {
        return appContext;
    }

    public static boolean hasServer(Server server) {
        boolean match = false;
        for (Server server1 : updater.getServerList()) {
            if (server1.getIp().equals(server.getIp())) {
                match = true;
            }
        }
        return match;
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
        serverSender = new ServerSender();
        exec.execute(serverDetector);

        for (String s : IPGetter.getInternalIP(true))
            Log.e("INTERNAL IP", s);

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
                updater.notifyDataSetChanged();
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
            InetAddress ip = InetAddress.getByName(serverList.get(position).getIp());
            serverDetector.sendConfirm(ip, serverList.get(position).getPort());
            serverSender.setIP(InetAddress.getByName(ip.getHostAddress()));
            if (serverSender.connect()) {
                Log.i("MainActivity", "ServerSender is connected!");
                startActivity(new Intent(MainActivity.this, NotiListActivity.class));
            } else {
                Log.e("MainActivity", "Unable to connect...");
            }
        } catch (UnknownHostException e) {
            Log.e("MainActivity", "Unknown Host");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
