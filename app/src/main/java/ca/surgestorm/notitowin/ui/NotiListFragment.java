package ca.surgestorm.notitowin.ui;

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
import ca.surgestorm.notitowin.backend.DefaultNotification;
import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.controller.notifyList.ActiveNotiProcessor;

import java.util.ArrayList;

public class NotiListFragment extends Fragment implements RecyclerViewClickListener {

    public static Handler fragmentHandler;
    public static boolean refreshButtonPressed = false;
    private RecyclerView recyclerView;
    public static ArrayList<DefaultNotification> defaultNotifications;
    private NotiListUpdater updater;
    private ActiveNotiProcessor anp;
    private View rootView;
    private SwipeRefreshLayout swipeRefreshLayout;


    public static void updateNotiArray(ArrayList<DefaultNotification> defaultNotification) {
        defaultNotifications = defaultNotification;
    }

    @Override
    public void onResume() {
        super.onResume();
//        anp.onDestroy();
        Log.i(getClass().getSimpleName(), "Resuming View!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentHandler = new Handler();
        rootView = inflater.inflate(R.layout.notilist_activity, container, false);
        swipeRefreshLayout = rootView.findViewById(R.id.refresh_notilist_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshAction);
        Log.i(getClass().getSimpleName(), "Creating View!");
        anp = new ActiveNotiProcessor();
        initializeView();
        return rootView;
    }

    private void refreshAction() {
        swipeRefreshLayout.setRefreshing(true);
        Log.i(getClass().getSimpleName(), "Refresh Activated!");
        anp.updateTimes();
        updater.notifyDataSetChanged();
        refreshButtonPressed = true;
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            swipeRefreshLayout.setRefreshing(false);
        }).start();
    }

    public void initializeView() {
        boolean success = anp.onCreate();
        if (!success) {
            anp.getErrorDialog(this.getActivity()).show();
            anp.onCreate();
        }


        defaultNotifications = anp.activeNotis;
        if (defaultNotifications == null) {
            defaultNotifications = new ArrayList<>();
        }
        recyclerView = rootView.findViewById(R.id.recyclerView2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.getAppContext()));
        updater = new NotiListUpdater(MainActivity.getAppContext(), defaultNotifications, this);
        recyclerView.setAdapter(updater);

    }

    @Override
    public void recyclerViewListClicked(View v, int position) {
//        MainActivity.serverConnector.setJson(json);
//        MainActivity.serverConnector.sendJSONToServer();
//        try {
        JSONConverter json = defaultNotifications.get(position).populateJSON();
        String s = json.serialize();
        Log.i(getClass().getSimpleName(), "JSON Export: " + s);
        BackgroundService.RunCommand(MainActivity.getAppContext(), service -> {
            service.sendGlobalPacket(json);
        });
        //            CharSequence data = s;
//            CharSequence description = "JSON Export";
//            ClipData cd = ClipData.newPlainText(description, data);
//            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//            clipboard.setPrimaryClip(cd);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        Toast.makeText(MainActivity.getAppContext(), "JSON for Notification " + (position + 1) + " Copied to Clipboard", Toast.LENGTH_SHORT).show();

    }
}
