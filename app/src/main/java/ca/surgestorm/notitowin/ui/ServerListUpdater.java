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
import ca.surgestorm.notitowin.backend.Server;

public class ServerListUpdater extends RecyclerView.Adapter<ServerListUpdater.ServerViewHolder> {

    private Context listContext;
    private List<Server> serverList;
    private static RecyclerViewClickListener mListener;


    public ServerListUpdater(Context listContext, List<Server> serverList, RecyclerViewClickListener itemClickListener) {
        this.listContext = listContext;
        this.serverList = serverList;
        mListener = itemClickListener;
    }

    public List<Server> getServerList(){
        return this.serverList;
    }

    @NonNull
    @Override
    public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(listContext);
        View view = inflater.inflate(R.layout.layout_serverlist, null);
        return new ServerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
        Server server = serverList.get(position);
        holder.serverName.setText(server.getServerName());
        holder.osDescription.setText(server.getOs());
        holder.serverIP.setText(server.getIp());
        holder.connectionMethod.setText(server.getConnectionMethod());
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
