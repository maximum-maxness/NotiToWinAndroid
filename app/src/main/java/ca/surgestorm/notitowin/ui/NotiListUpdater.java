package ca.surgestorm.notitowin.ui;

import android.content.Context;
import android.graphics.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.DefaultNotification;

import java.util.List;

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
        View view = inflater.inflate(R.layout.notilist_card, null);
        return new NotiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotiViewHolder holder, int position) {
        DefaultNotification notification = notificationList.get(position);
        holder.appName.setText(notification.getAppName());
        holder.content.setText(notification.getText());
        holder.title.setText(notification.getTitle());

        Bitmap smBM = notification.getSmallIconBitmap();
        Bitmap lrgBM = notification.getLargeIconBitmap();
        Paint paint = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(ContextCompat.getColor(MainActivity.getAppContext(), R.color.niceDark), PorterDuff.Mode.SRC_IN);
        paint.setColorFilter(filter);
        Canvas canvas = new Canvas(smBM);
        canvas.drawBitmap(smBM, 0, 0, paint);
        holder.appIcon.setImageBitmap(smBM);
        if (lrgBM != null) {
            holder.largeIcon.setImageBitmap(lrgBM);
        } else {
            holder.largeIcon.setImageBitmap(smBM);
        }
        holder.time.setText(notification.getTime());
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class NotiViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView appName, time, content, title;
        ImageView appIcon, largeIcon;

        NotiViewHolder(@NonNull View itemView) {
            super(itemView);

            appName = itemView.findViewById(R.id.appName);
            time = itemView.findViewById(R.id.time);
            content = itemView.findViewById(R.id.content);
            title = itemView.findViewById(R.id.title);
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
