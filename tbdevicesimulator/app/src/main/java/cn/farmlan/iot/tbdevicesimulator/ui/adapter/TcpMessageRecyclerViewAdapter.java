package cn.farmlan.iot.tbdevicesimulator.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cn.farmlan.iot.tbdevicesimulator.R;
import cn.farmlan.iot.tbdevicesimulator.model.TcpMessage;

public class TcpMessageRecyclerViewAdapter extends RecyclerView.Adapter<TcpMessageRecyclerViewAdapter.MyViewHolder> {
    private ArrayList<TcpMessage> mMessages;
    private MyRecyclerViewItemClickListener mItemClickListener;

    public TcpMessageRecyclerViewAdapter(ArrayList<TcpMessage> msgs, MyRecyclerViewItemClickListener itemClickListener) {
        this.mMessages = msgs;
        this.mItemClickListener = itemClickListener;
    }

    public TcpMessageRecyclerViewAdapter(MyRecyclerViewItemClickListener itemClickListener) {
        this.mMessages = new ArrayList<>();
        this.mItemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Inflate RecyclerView row
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_tcp_message_item, parent, false);

        //Create View Holder
        final MyViewHolder myViewHolder = new MyViewHolder(view);

        //Item Clicks
        myViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickListener.onItemClicked(mMessages.get(myViewHolder.getLayoutPosition()));
            }
        });

        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.textViewMsgContent.setText(mMessages.get(position).getContent());
        String timestamp = "时间: " + mMessages.get(position).getTimestamp();
        holder.textViewMsgTimestamp.setText(timestamp);
        holder.imgDevice.setVisibility(mMessages.get(position).isFromServer() ? View.GONE : View.VISIBLE);
        holder.imgServer.setVisibility(mMessages.get(position).isFromServer() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //RecyclerView View Holder
    class MyViewHolder extends RecyclerView.ViewHolder {
        private final View imgDevice;
        private final View imgServer;
        private TextView textViewMsgContent;
        private TextView textViewMsgTimestamp;
        private TextView textViewMsgSender;
        private TextView textViewMsgReceiver;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMsgContent = itemView.findViewById(R.id.msg_content);
            textViewMsgTimestamp = itemView.findViewById(R.id.msg_timestamp);
            textViewMsgSender = itemView.findViewById(R.id.msg_sender);
            textViewMsgReceiver = itemView.findViewById(R.id.msg_receiver);
            imgServer = itemView.findViewById(R.id.sender_server);
            imgDevice = itemView.findViewById(R.id.sender_device);
        }
    }

    //RecyclerView Click Listener
    public interface MyRecyclerViewItemClickListener {
        void onItemClicked(TcpMessage country);
    }

    public void setMessages(ArrayList<TcpMessage> mMessages) {
        this.mMessages = mMessages;
    }
}
