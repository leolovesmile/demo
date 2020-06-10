package cn.farmlan.iot.tbdevicesimulator.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import cn.farmlan.iot.tbdevicesimulator.R;
import cn.farmlan.iot.tbdevicesimulator.model.TcpMessage;
import cn.farmlan.iot.tbdevicesimulator.ui.adapter.TcpMessageRecyclerViewAdapter;
import io.netty.util.internal.StringUtil;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    private RecyclerView mRecyclerView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);

        View root = inflater.inflate(R.layout.fragment_home, container, false);


        final EditText msgTxt = root.findViewById(R.id.messageEdit);
        root.findViewById(R.id.btnMsgSend).setOnClickListener(btn -> {
            String msg = msgTxt.getEditableText().toString().trim();
            if (!StringUtil.isNullOrEmpty(msg)) {
                homeViewModel.sendMsg(msg);
            }
            msgTxt.getEditableText().clear();
        });

        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });


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

        homeViewModel.bind(getViewLifecycleOwner());


        homeViewModel.getMsgList().observe(this.getViewLifecycleOwner(), lst -> {
            myRecyclerViewAdapter.setMessages(lst);
            myRecyclerViewAdapter.notifyDataSetChanged();
        });
        return root;
    }
}
