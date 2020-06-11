package cn.farmlan.iot.tbdevicesimulator.ui.home;


import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Date;

import cn.farmlan.iot.tbdevicesimulator.connector.NettyTcpClientConnector;
import cn.farmlan.iot.tbdevicesimulator.model.TcpMessage;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeViewModel extends ViewModel {

    private NettyTcpClientConnector client;
    private MutableLiveData<String> mViewText;
    private MutableLiveData<String> mMsgText;
    private MutableLiveData<ArrayList<TcpMessage>> msgList;

    public HomeViewModel() {
        mMsgText = new MutableLiveData<>();
        mViewText = new MutableLiveData<>();
        msgList = new MutableLiveData<>();
    }

    public void bind(LifecycleOwner lifecycleOwner, String hostname, int port, String token, String authTemplate, String dataTemplate) {
        mViewText.setValue(String.format("服务器信息： %1s:%2s\n本机token： %3s", hostname, port, token));
        final String authMsg = String.format(authTemplate, token);
        client = new NettyTcpClientConnector(hostname, port);

        Publisher<String> pub = LiveDataReactiveStreams.toPublisher(lifecycleOwner, mMsgText);
        @NonNull Observable<String> msgsWithAuthdataAdded = Observable.fromPublisher(pub).flatMap(msg -> Observable.fromArray(authMsg, msg));

//        @NonNull Flowable<TcpMessage> clientFlow = Flowable.fromPublisher(pub).map(msg -> new TcpMessage(msg, new Date(), "客户端"));
//        @NonNull Flowable<TcpMessage> serverFlow = Flowable.fromPublisher(pub).map(msg -> new TcpMessage(msg, new Date(), "服务端"));
        @NonNull Observable<TcpMessage> clientFlow = client.connectAndSend(msgsWithAuthdataAdded).map(msg -> new TcpMessage(msg, new Date(), false));
        @NonNull Observable<TcpMessage> serverFlow = client.getServerMsg().map(msg -> new TcpMessage(msg, new Date(), true));

        clientFlow.mergeWith(serverFlow)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
            ArrayList<TcpMessage> list = msgList.getValue();
            list = list == null ?  new ArrayList<>() : list;
            list.add(s);
            msgList.setValue(list);
        });
    }

    public LiveData<String> getServerInfoText() {
        return mViewText;
    }

    public void sendMsg(String msg) {
        this.mMsgText.setValue(msg);
    }

    public MutableLiveData<ArrayList<TcpMessage>> getMsgList() {
        return msgList;
    }
}