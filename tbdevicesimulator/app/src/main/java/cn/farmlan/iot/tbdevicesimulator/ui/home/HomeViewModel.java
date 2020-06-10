package cn.farmlan.iot.tbdevicesimulator.ui.home;


import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Date;

import cn.farmlan.iot.tbdevicesimulator.model.TcpMessage;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mViewText;
    private MutableLiveData<String> mMsgText;
    private MutableLiveData<ArrayList<TcpMessage>> msgList;

    public HomeViewModel() {
        mMsgText = new MutableLiveData<>();
        mViewText = new MutableLiveData<>();
        msgList = new MutableLiveData<>();
        mViewText.setValue("Hostname: Port: Device");
    }

    public void bind(LifecycleOwner lifecycleOwner) {
        Publisher<String> pub = LiveDataReactiveStreams.toPublisher(lifecycleOwner, mMsgText);
        @NonNull Flowable<TcpMessage> clientFlow = Flowable.fromPublisher(pub).map(msg -> new TcpMessage(msg, new Date(), "客户端"));
        @NonNull Flowable<TcpMessage> serverFlow = Flowable.fromPublisher(pub).map(msg -> new TcpMessage(msg, new Date(), "服务端"));

        clientFlow.mergeWith(serverFlow).subscribe(s -> {
            ArrayList<TcpMessage> list = msgList.getValue();
            list = list == null ?  new ArrayList<>() : list;
            list.add(s);
            msgList.setValue(list);
        });
    }

    public LiveData<String> getText() {
        return mViewText;
    }

    public void sendMsg(String msg) {
        this.mMsgText.setValue(msg);
    }

    public MutableLiveData<ArrayList<TcpMessage>> getMsgList() {
        return msgList;
    }
}