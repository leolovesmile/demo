package cn.farmlan.iot.tbdevicesimulator.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import cn.farmlan.iot.tbdevicesimulator.R;
import cn.farmlan.iot.tbdevicesimulator.model.TcpMessage;
import cn.farmlan.iot.tbdevicesimulator.ui.adapter.TcpMessageRecyclerViewAdapter;
import io.netty.util.internal.StringUtil;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    private RecyclerView mRecyclerView;
    private String hostname;
    private int port;
    private String token;
    private boolean serverInfoValid = false;
    private String authTemplate;
    private String dataTemplate;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        this.retrieveServerInfo();


        final TextView textView = root.findViewById(R.id.text_title);
        homeViewModel.getServerInfoText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        root.findViewById(R.id.fab_new_msg).setOnClickListener(l -> this.sendMsg());


        mRecyclerView = root.findViewById(R.id.msgList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext(), RecyclerView.VERTICAL, false));
        TcpMessageRecyclerViewAdapter myRecyclerViewAdapter = new TcpMessageRecyclerViewAdapter(new TcpMessageRecyclerViewAdapter.MyRecyclerViewItemClickListener() {
            //Handling clicks
            @Override
            public void onItemClicked(TcpMessage msg) {
                Toast.makeText(HomeFragment.this.getContext(), msg.getContent(), Toast.LENGTH_SHORT).show();
            }
        });
        //Set adapter to RecyclerView
        mRecyclerView.setAdapter(myRecyclerViewAdapter);

        homeViewModel.bind(getViewLifecycleOwner(), this.hostname, this.port, this.token, this.authTemplate, this.dataTemplate);
        homeViewModel.getMsgList().observe(this.getViewLifecycleOwner(), lst -> {
            myRecyclerViewAdapter.setMessages(lst);
            myRecyclerViewAdapter.notifyDataSetChanged();
        });
        return root;
    }

    private void retrieveServerInfo() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this.getContext());
        hostname = sharedPreferences.getString(getString(R.string.sp_key_server_host_name), "");
        port = Integer.valueOf(sharedPreferences.getString(getString(R.string.sp_key_server_port), "6060"));
        token = sharedPreferences.getString(getString(R.string.sp_key_device_token), "");
        authTemplate = sharedPreferences.getString(getString(R.string.sp_key_auth_data), "");
        dataTemplate = sharedPreferences.getString(getString(R.string.sp_key_tele_data), "");

        serverInfoValid = StringUtil.isNullOrEmpty(hostname) || StringUtil.isNullOrEmpty(token) || port > 65535 || port < 1;
    }

    private void sendMsg() {
        if (!serverInfoValid) {
            Toast.makeText(this.getContext(), R.string.msg_server_info_invalid, Toast.LENGTH_LONG).show();
            return;
        }

        final AlertDialog.Builder newMsgDlg = new AlertDialog.Builder(this.getContext());
        newMsgDlg.setIcon(android.R.drawable.ic_dialog_info);
        newMsgDlg.setTitle("编辑设备的消息");
        newMsgDlg.setMessage("请编辑设备将要发送的消息：");
        final EditText editText = new EditText(this.getContext());
        editText.setText(this.dataTemplate);
        newMsgDlg.setView(editText);
        newMsgDlg.setPositiveButton("确定",
                (dialog, which) -> {
                    String msg = editText.getEditableText().toString().trim();
                    if (!StringUtil.isNullOrEmpty(msg)) {
                        homeViewModel.sendMsg(msg);
                    }
                });
        newMsgDlg.setNegativeButton("关闭", null);
        newMsgDlg.show();
    }
}
