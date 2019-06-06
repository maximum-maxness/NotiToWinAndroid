package ca.surgestorm.notitowin.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import ca.surgestorm.notitowin.BackgroundService;
import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.Server;

import java.util.Objects;
import java.util.Set;

public class ServerListFragment extends Fragment implements RecyclerViewClickListener {
    public static Handler fragmentHandler;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View rootView;
    private Button clear_trusted;
    private Activity mainActivity;
    private RecyclerView recyclerView;

    private BackgroundService.DeviceListChangedCallback deviceListChangedCallback = this::dataSetChanged;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(getClass().getSimpleName(), "ON CREATE CALLED");
        fragmentHandler = new Handler();
//        Objects.requireNonNull(mainActivity.getActionBar()).setTitle("Servers on LAN");
        rootView = inflater.inflate(R.layout.serverlist_activity, container, false);
        swipeRefreshLayout = rootView.findViewById(R.id.refresh_serverlist_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshServerList);
        recyclerView = rootView.findViewById(R.id.serverList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.getAppContext()));
        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            service.addServerListChangedCallback("serverFrag", deviceListChangedCallback);
            service.setUpdater(new ServerListUpdater(MainActivity.getAppContext(), service.getDevices().values(), this));
            recyclerView.setAdapter(service.getUpdater());
        });
        clear_trusted = rootView.findViewById(R.id.clear_trusted);
        clear_trusted.setOnClickListener((event) -> {
            clearTrustedAction();
        });
        return rootView;
    }


    public void dataSetChanged() {
        fragmentHandler.post(() -> {
            BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
                service.getUpdater().notifyDataSetChanged();
            });
        });
    }


    private void refreshServerList() {
        swipeRefreshLayout.setRefreshing(true);
        Log.i(getClass().getSimpleName(), "Refresh Activated!");
        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            service.onServerListChanged();
            service.onNetworkChange();
        });
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            swipeRefreshLayout.setRefreshing(false);
        }).start();
        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = getActivity();
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {

        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            final Server server = (Server) service.getDevices().values().toArray()[position];
            if (server != null) {
                Log.i(getClass().getSimpleName(), "Server Name: " + server.getName() + " clicked!");
                if (!server.isPaired()) {
                    Log.i(getClass().getSimpleName(), "Server is not paired.");
                    if (server.isPairRequestedByPeer()) {
                        Log.i(getClass().getSimpleName(), "Server's Connection is requested by peer.");
                        server.acceptPairing();
                    } else {
                        Log.i(getClass().getSimpleName(), "Server's Connection is not requested by peer.");
                        server.requestPairing();
                    }
                }
            }
        });

    }

    @SuppressLint("ApplySharedPref")
    private void clearTrustedAction() {
        SharedPreferences sharedPreferences = MainActivity.getAppContext().getSharedPreferences("trusted_devices", Context.MODE_PRIVATE);
        Set<String> trustedServers = sharedPreferences.getAll().keySet();
        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            for (String clientID : trustedServers) {
                Server server = service.getDevices().remove(clientID);
                Objects.requireNonNull(server).unpair();
            }
        });
        sharedPreferences.edit().clear().commit();
        Intent mStartActivity = new Intent(MainActivity.getAppContext(), MainActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.getAppContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) mainActivity.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }
}
