package ca.surgestorm.notitowin.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import ca.surgestorm.notitowin.BackgroundService;
import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.Server;

import java.util.Objects;

public class ServerListFragment extends Fragment implements RecyclerViewClickListener {
    public static Handler fragmentHandler;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View rootView;
    private Activity mainActivity;
    private RecyclerView recyclerView;

    private BackgroundService.DeviceListChangedCallback deviceListChangedCallback = this::dataSetChanged;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentHandler = new Handler();
        Objects.requireNonNull(mainActivity.getActionBar()).setTitle("Servers on LAN");
        rootView = inflater.inflate(R.layout.serverlist_activity, container, false);
        swipeRefreshLayout = rootView.findViewById(R.id.refresh_list_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshServerList);
        recyclerView = rootView.findViewById(R.id.serverList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.getAppContext()));
        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            service.addServerListChangedCallback("serverFrag", deviceListChangedCallback);
            service.setUpdater(new ServerListUpdater(MainActivity.getAppContext(), service.getDevices().values(), this));
            recyclerView.setAdapter(service.getUpdater());
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
        Log.i("MainActivity", "Refresh Activated!");
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
                System.out.println("Server Name: " + server.getName() + " clicked!");
                if (!server.isPaired()) {
                    System.out.println("Server is not paired.");
                    if (server.isPairRequestedByPeer()) {
                        System.out.println("Server's Connection is requested by peer.");
                        server.acceptPairing();
                    } else {
                        System.out.println("Server's Connection is not requested by peer.");
                        server.requestPairing();
                    }
                }
            }
        });

    }
}
