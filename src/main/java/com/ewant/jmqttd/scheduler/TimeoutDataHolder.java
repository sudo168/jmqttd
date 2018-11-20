package com.ewant.jmqttd.scheduler;

import io.netty.util.Timeout;

public class TimeoutDataHolder<T> {

    private Timeout timeout;
    private T data;

    TimeoutDataHolder(Timeout timeout){
        this.timeout = timeout;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
