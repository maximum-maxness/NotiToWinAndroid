package ca.surgestorm.notitowin.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;

import ca.surgestorm.notitowin.R;
import ca.surgestorm.notitowin.backend.Server;

public class ServerListUpdater extends RecyclerView.Adapter<ServerListUpdater.ServerViewHolder> {

    private Context listContext;
    private Collection<Server> serverList;
    private static RecyclerViewClickListener mListener;


    public ServerListUpdater(Context listContext, Collection<Server> serverList, RecyclerViewClickListener itemClickListener) {
        this.listContext = listContext;
        this.serverList = serverList;
        mListener = itemClickListener;
    }

    public Collection<Server> getServerList() {
        return this.serverList;
    }

    @NonNull
    @Override
    public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(listContext);
        View view = inflater.inflate(R.layout.serverlist_card, null);
        return new ServerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
        Server server = (Server) serverList.toArray()[position];
        holder.serverName.setText(server.getName());
        holder.osDescription.setText(server.getOsName() + "v." + server.getOsVer());
        holder.serverIP.setText(server.getIP());
        holder.connectionMethod.setText("LAN");
        holder.previewImage.setImageDrawable(listContext.getDrawable(server.getPreviewImage()));
    }

    @Override
    public int getItemCount() {
        return serverList.size();
    }

    class ServerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView serverName, osDescription, serverIP, connectionMethod;
        ImageView previewImage;

        ServerViewHolder(@NonNull View itemView) {
            super(itemView);

            serverName = itemView.findViewById(R.id.serverName);
            osDescription = itemView.findViewById(R.id.osDescription);
            serverIP = itemView.findViewById(R.id.serverIP);
            previewImage = itemView.findViewById(R.id.previewImage);
            connectionMethod = itemView.findViewById(R.id.connectionMethod);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mListener.recyclerViewListClicked(v, getLayoutPosition());
        }
    }
}
