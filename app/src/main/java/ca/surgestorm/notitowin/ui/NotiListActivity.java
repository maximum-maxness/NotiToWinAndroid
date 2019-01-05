package ca.surgestorm.notitowin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.DefaultNotification;
import ca.surgestorm.notitowin.controller.notifyList.ActiveNotiProcessor;

public class NotiListActivity extends AppCompatActivity {

    public static boolean refreshButtonPressed = false;
    private RecyclerView recyclerView;
    private ArrayList<DefaultNotification> defaultNotifications;
    private NotiListUpdater updater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notilist_activity);

        ActiveNotiProcessor anp = new ActiveNotiProcessor();
        Log.i("Noti Activity", "Initialized anp!");

        boolean success = anp.onCreate();
        if (!success) {
            anp.getErrorDialog(this).show();
            anp.onCreate();
        }

        configureGoToMainButton(anp);

        defaultNotifications = anp.activeNotis;

        recyclerView = findViewById(R.id.recyclerView2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        updater = new NotiListUpdater(MainActivity.getAppContext(), defaultNotifications);
        recyclerView.setAdapter(updater);
        configureRefreshButton();

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

    private void configureRefreshButton() {
        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updater.notifyDataSetChanged();
                refreshButtonPressed = true;
            }
        });
    }
}
