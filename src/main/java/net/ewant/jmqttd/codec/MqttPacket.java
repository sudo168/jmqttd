package net.ewant.jmqttd.codec;

import io.netty.buffer.ByteBuf;
import net.ewant.jmqttd.codec.message.MqttFixedHeader;
import net.ewant.jmqttd.server.mqtt.MqttSession;

/**
 * Created by admin on 2019/5/23.
 */
public class MqttPacket {

    static int MAX_FRAME_LENGTH = 0xFFFFFFF;// 268435455=256M

    MqttSession client;

    MqttFixedHeader fixedHeader;

    int remainingLength;

    ByteBuf remainingData;

    Exception cause;

    public MqttSession getClient() {
        return client;
    }

    public void setClient(MqttSession client) {
        this.client = client;
    }

    public MqttFixedHeader getFixedHeader() {
        return fixedHeader;
    }

    public void setFixedHeader(MqttFixedHeader fixedHeader) {
        this.fixedHeader = fixedHeader;
    }

    public int getRemainingLength() {
        return remainingLength;
    }

    public void setRemainingLength(int remainingLength) {
        this.remainingLength = remainingLength;
    }

    public ByteBuf getRemainingData() {
        return remainingData;
    }

    public void setRemainingData(ByteBuf remainingData) {
        this.remainingData = remainingData;
    }

    public Exception getCause() {
        return cause;
    }

    public void setCause(Exception cause) {
        this.cause = cause;
    }

    public void clear(){
        client = null;
        fixedHeader = null;
        cause = null;
        if(remainingData != null){
            remainingData.release();
            remainingData = null;
        }
    }

}
