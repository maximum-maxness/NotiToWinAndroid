package ca.surgestorm.notitowin.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import ca.surgestorm.notitowin.BackgroundService;
import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.DefaultNotification;
import ca.surgestorm.notitowin.backend.JSONConverter;
import ca.surgestorm.notitowin.controller.notifyList.ActiveNotiProcessor;

public class NotiListActivity extends Activity implements RecyclerViewClickListener {

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
        recyclerView = findViewById(R.id.recyclerView2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        updater = new NotiListUpdater(MainActivity.getAppContext(), defaultNotifications, this);
        recyclerView.setAdapter(updater);
    }

    private void configureGoToMainButton(ActiveNotiProcessor anp) {
        Button backButton = findViewById(R.id.gotomainbutton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToMain();
            }
        });
    }

    public void goBackToMain() {
        anp.onDestroy();
        BackgroundService.RunCommand(MainActivity.getAppContext(), BackgroundService::cleanDevices);
        startActivity(new Intent(NotiListActivity.this, MainActivity.class));
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {
//        MainActivity.serverConnector.setJson(json);
//        MainActivity.serverConnector.sendJSONToServer();
//        try {
            JSONConverter json = defaultNotifications.get(position).populateJSON();
            String s = json.serialize();
            Log.i("NotiToWin", "JSON Export: " + s);
        BackgroundService.RunCommand(this, service -> {
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
