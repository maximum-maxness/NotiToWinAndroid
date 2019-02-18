package ca.surgestorm.notitowin.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.DefaultNotification;
import ca.surgestorm.notitowin.backend.IPGetter;
import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.controller.notifyList.ActiveNotiProcessor;

public class NotiListActivity extends AppCompatActivity implements RecyclerViewClickListener {

    public static boolean refreshButtonPressed = false;
    private RecyclerView recyclerView;
    public static ArrayList<DefaultNotification> defaultNotifications;
    private NotiListUpdater updater;
    private ActiveNotiProcessor anp;

    public static void updateNotiArray(ArrayList<DefaultNotification> defaultNotification) {
        defaultNotifications = defaultNotification;
    }

    @Override
    public void onResume() {
        super.onResume();
        anp.onDestroy();
        initializeView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notilist_activity);

        initializeView();
    }

    private void initializeView() {
        anp = new ActiveNotiProcessor();
        Log.i("Noti Activity", "Initialized anp!");

        boolean success = anp.onCreate();
        if (!success) {
            anp.getErrorDialog(this).show();
            anp.onCreate();
        }

        configureGoToMainButton(anp);

        defaultNotifications = anp.activeNotis;
        if (defaultNotifications == null) {
            defaultNotifications = new ArrayList<>();
        }
        Log.e("INTERNAL IP", IPGetter.getInternalIP(true));
        recyclerView = findViewById(R.id.recyclerView2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        updater = new NotiListUpdater(MainActivity.getAppContext(), defaultNotifications, this);
        recyclerView.setAdapter(updater);
        configureRefreshButton(anp);
    }

    private void configureGoToMainButton(ActiveNotiProcessor anp) {
        Button backButton = findViewById(R.id.gotomainbutton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anp.onDestroy();
                startActivity(new Intent(NotiListActivity.this, MainActivity.class));
            }
        });
    }

    private void configureRefreshButton(ActiveNotiProcessor anp) {
        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anp.updateTimes();
                updater.notifyDataSetChanged();
                refreshButtonPressed = true;
            }
        });
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {
        JSONConverter json = defaultNotifications.get(position).populateJSON();
//        MainActivity.serverConnector.setJson(json);
//        MainActivity.serverConnector.sendJSONToServer();
        try {
            String s = json.serialize();
            Log.i("NotiToWin", "JSON Export: " + s);
            MainActivity.serverDetector.sendJson(s);
//            CharSequence data = s;
//            CharSequence description = "JSON Export";
//            ClipData cd = ClipData.newPlainText(description, data);
//            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//            clipboard.setPrimaryClip(cd);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        Toast.makeText(MainActivity.getAppContext(), "JSON for Notification " + (position + 1) + " Copied to Clipboard", Toast.LENGTH_SHORT).show();

    }
}
