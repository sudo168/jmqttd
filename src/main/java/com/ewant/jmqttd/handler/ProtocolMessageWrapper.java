package com.ewant.jmqttd.handler;

import io.netty.buffer.ByteBuf;

public interface ProtocolMessageWrapper {
    Object wraperMessage(ByteBuf mqttBuffer);
}
