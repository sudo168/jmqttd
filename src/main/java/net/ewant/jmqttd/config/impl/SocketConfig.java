package net.ewant.jmqttd.config.impl;

public class SocketConfig {

    private boolean tcpNoDelay = true;

    private int tcpSendBufferSize = -1;

    private int tcpReceiveBufferSize = -1;

    private boolean tcpKeepAlive = true;

    private int soLinger = -1;

    private boolean reuseAddress = true;

    private int backLog = 1024;

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getTcpSendBufferSize() {
        return tcpSendBufferSize;
    }
    public void setTcpSendBufferSize(int tcpSendBufferSize) {
        this.tcpSendBufferSize = tcpSendBufferSize;
    }

    public int getTcpReceiveBufferSize() {
        return tcpReceiveBufferSize;
    }
    public void setTcpReceiveBufferSize(int tcpReceiveBufferSize) {
        this.tcpReceiveBufferSize = tcpReceiveBufferSize;
    }

    public boolean isTcpKeepAlive() {
        return tcpKeepAlive;
    }
    public void setTcpKeepAlive(boolean tcpKeepAlive) {
        this.tcpKeepAlive = tcpKeepAlive;
    }

    public int getSoLinger() {
        return soLinger;
    }
    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public int getBackLog() {
        return backLog;
    }

    public void setBackLog(int backLog) {
        this.backLog = backLog;
    }
}
