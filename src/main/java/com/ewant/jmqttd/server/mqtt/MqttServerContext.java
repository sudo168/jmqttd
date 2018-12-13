package com.ewant.jmqttd.server.mqtt;

import com.ewant.jmqttd.core.ServerProtocol;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MqttServerContext {

    private static Map<ServerProtocol, MqttServer> protocolServers = new ConcurrentHashMap<>();

    public static void addServer(MqttServer server){
        protocolServers.put(server.getProtocol(), server);
    }

    public static MqttServer getServer(ServerProtocol protocol){
        return protocolServers.get(protocol);
    }

    static MqttServer removeServer(ServerProtocol protocol){
        return protocolServers.remove(protocol);
    }

    public static Collection<MqttServer> getServers(){
        return Collections.unmodifiableCollection(protocolServers.values());
    }

}
