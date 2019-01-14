package net.ewant.jmqttd.config;

import net.ewant.jmqttd.config.impl.ClientConfig;
import net.ewant.jmqttd.config.impl.ClusterConfig;
import net.ewant.jmqttd.config.impl.ListenersConfig;
import net.ewant.jmqttd.config.impl.MonitorConfig;
import net.ewant.jmqttd.config.impl.ServerConfig;
import net.ewant.jmqttd.config.impl.SocketConfig;

public class ServerConfiguration {

    static final ServerConfiguration INSTANCE = new ServerConfiguration();

    private ClientConfig clientConfig;
    private ClusterConfig clusterConfig;
    private ListenersConfig listenersConfig;
    private MonitorConfig monitorConfig;
    private ServerConfig serverConfig;
    private SocketConfig socketConfig;
    private AccessControlConfig aclConfig;

    private ServerConfiguration() {
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    public void setClusterConfig(ClusterConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    public ListenersConfig getListenersConfig() {
        return listenersConfig;
    }

    public void setListenersConfig(ListenersConfig listenersConfig) {
        this.listenersConfig = listenersConfig;
    }

    public MonitorConfig getMonitorConfig() {
        return monitorConfig;
    }

    public void setMonitorConfig(MonitorConfig monitorConfig) {
        this.monitorConfig = monitorConfig;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public void setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    public AccessControlConfig getAclConfig() {
        return aclConfig;
    }

    public void setAclConfig(AccessControlConfig aclConfig) {
        this.aclConfig = aclConfig;
    }
}
