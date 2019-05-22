package ca.surgestorm.notitowin.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import ca.surgestorm.notitowin.R;

public class ServerListFragment extends Fragment {
    private MainActivity mainActivity;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.serverlist_activity, container, false);
        View listView = rootView.findViewById(R.id.serverList);
        swipeRefreshLayout = rootView.findViewById(R.id.refresh_list_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshServerList);
        return rootView;
    }

    private void refreshServerList() {
        swipeRefreshLayout.setRefreshing(true);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            swipeRefreshLayout.setRefreshing(false);
        }).start();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = ((MainActivity) getActivity());
    }
}
