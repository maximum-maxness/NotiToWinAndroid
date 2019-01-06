package ca.surgestorm.notitowin.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.DefaultNotification;

public class NotiListUpdater extends RecyclerView.Adapter<NotiListUpdater.NotiViewHolder> {

    private Context listContext;
    private List<DefaultNotification> notificationList;
    private static RecyclerViewClickListener mListener;

    public NotiListUpdater(Context listContext, List<DefaultNotification> notificationList, RecyclerViewClickListener itemClickListener) {
        this.listContext = listContext;
        this.notificationList = notificationList;
        mListener = itemClickListener;
    }

    @NonNull
    @Override
    public NotiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(listContext);
        View view = inflater.inflate(R.layout.layout_notilist, null);
        return new NotiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotiViewHolder holder, int position) {
        DefaultNotification notification = notificationList.get(position);
        holder.appName.setText(notification.getAppName());
        holder.content.setText(notification.getText());
        holder.appIcon.setImageBitmap(notification.getSmallIconBitmap());
        holder.largeIcon.setImageBitmap(notification.getLargeIconBitmap()); //TODO Implement Title into cards, as well as Ticker Text?
        holder.time.setText(notification.getTime());
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class NotiViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView appName, time, content;
        ImageView appIcon, largeIcon;

        NotiViewHolder(@NonNull View itemView) {
            super(itemView);

            appName = itemView.findViewById(R.id.appName);
            time = itemView.findViewById(R.id.time);
            content = itemView.findViewById(R.id.content);
            appIcon = itemView.findViewById(R.id.appIcon);
            largeIcon = itemView.findViewById(R.id.largeIcon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mListener.recyclerViewListClicked(v, getLayoutPosition());
        }

    }
}
