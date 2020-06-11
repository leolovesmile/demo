package cn.farmlan.iot.tbdevicesimulator.model;

import java.io.Serializable;
import java.util.Date;

public class TcpMessage implements Serializable {
    private String content;

    private Date timestamp;

    private boolean fromServer;


    public TcpMessage(String content, Date timestamp, boolean fromServer) {
        this.content = content;
        this.timestamp = timestamp;
        this.fromServer = fromServer;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFromServer() {
        return fromServer;
    }

    public void setFromServer(boolean fromServer) {
        this.fromServer = fromServer;
    }
}
