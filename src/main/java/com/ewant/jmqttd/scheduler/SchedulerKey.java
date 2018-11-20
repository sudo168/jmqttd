package com.ewant.jmqttd.scheduler;

import com.ewant.jmqttd.utils.ProtocolUtil;

import io.netty.channel.Channel;

public class SchedulerKey {

    public enum Type {
        /**
         * tcp连接上到发送connect数据包的间隔时间
         */
        CONNECT_TIMEOUT,
        /**
    	 * 连接上后到发送第一条数据的间隔时间（未用）
    	 */
        FIRST_DATA_TIMEOUT,
        /**
    	 * 客户端空闲超时时间（未用）
    	 */
        IDLE_TIMEOUT,
        /**
         * 客户端的ping心跳包定时发送调度
         */
        CLIENT_PING_SENDER_SCHEDULER,
        /**
         * 客户端发送QoS1、QoS2的publish包，没有收到对端回执需要重发调度
         */
        CLIENT_PUBLISH_SCHEDULER,
        /**
         * 客户端发送QoS2的publish包，收到对端PUBREC，继而发送PUBREL，但没有收到对端PUBCOMP回执需要重发调度
         */
        CLIENT_PUBREL_SCHEDULER,
        /**
         * 服务端发送QoS1、QoS2的publish包，没有收到对端回执需要重发调度
         */
        SERVER_PUBLISH_SCHEDULER,
        /**
         * 服务端发送QoS2的publish包，收到对端PUBREC，继而发送PUBREL，但没有收到对端PUBCOMP回执需要重发调度
         */
        SERVER_PUBREL_SCHEDULER,
    }

    private final Type type;
    private final String sessionId;

    private SchedulerKey(){
        this(null, "");
    }

    private SchedulerKey(Type type, Channel channel) {
        this(type, ProtocolUtil.toSessionId(channel));
    }

    private SchedulerKey(Type type, String sessionId) {
        this.type = type;
        this.sessionId = sessionId;
    }

    public static SchedulerKey forKey(Type type, Channel channel){
        return new SchedulerKey(type, channel);
    }

    public static SchedulerKey forKey(Type type, String sessionId){
        return new SchedulerKey(type, sessionId);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((sessionId == null) ? 0 : sessionId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SchedulerKey other = (SchedulerKey) obj;
        if (sessionId == null) {
            if (other.sessionId != null)
                return false;
        } else if (!sessionId.equals(other.sessionId))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

}
