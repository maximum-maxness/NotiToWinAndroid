package ca.surgestorm.notitowin.runner;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.IPGetter;
import ca.surgestorm.notitowin.backend.Server;
import ca.surgestorm.notitowin.views.serverList.ServerListUpdater;

public class MainWindow extends AppCompatActivity {

    List<Server> serverList;
    RecyclerView recyclerView;

    //    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_window);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        serverList = new ArrayList<>();
        String extIP = "";
        String intIP = "";
        try {

            intIP = IPGetter.getInternalIP(true);
            extIP = IPGetter.getExternalIP();

        } catch (Exception e) {
            Log.e("ca.surgestorm.notitowin", "exception", e);
        }
        for (int i = 0; i < 8;
             i++) {
            serverList.add(
                    new Server(
                            intIP,
                            0,
                            R.drawable.windows10,
                            "maxs-laptop",
                            "18.09"
                    )
            );
        }
        ServerListUpdater updater = new ServerListUpdater(this, serverList);
        recyclerView.setAdapter(updater);
    }
}
