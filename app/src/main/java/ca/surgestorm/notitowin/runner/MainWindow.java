package ca.surgestorm.notitowin.runner;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.IPGetter;

public class MainWindow extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_window);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        EditText consoleOut = this.findViewById(R.id.consoleOut);
        consoleOut.setText("hello");
        try {
            consoleOut.setText("\nrunning!\n");
            consoleOut.append("internal IP: " + IPGetter.getInternalIP(true) + "\n");
            consoleOut.append("external IP: " + IPGetter.getExternalIP() + "\n");

        } catch (Exception e) {
            Log.e("ca.surgestorm.notitowin", "exception", e);
        }
    }
}
