package net.ewant.jmqttd.listener;

import net.ewant.jmqttd.config.ConfigParseException;
import net.ewant.jmqttd.config.ServerConfiguration;
import net.ewant.jmqttd.config.impl.ClientConfig;
import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.server.mqtt.MqttServer;
import net.ewant.jmqttd.server.mqtt.MqttServerContext;
import net.ewant.jmqttd.server.mqtt.MqttSession;
import net.ewant.jmqttd.utils.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Created by admin on 2019/4/18.
 */
public class ClientSessionListenerWrapper implements ClientSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(MqttServer.class);

    private static final String NOTIFY_TOPIC = "$sys/clients";
    private MqttServer server;
    private ClientSessionListener customListener;

    public ClientSessionListenerWrapper(ServerProtocol protocol){
        this.server = MqttServerContext.getServer(protocol);
        ClientConfig clientConfig = this.server.getConfiguration().getClientConfig();
        if(clientConfig != null){
            String pluginName = clientConfig.getPluginNotify();
            try {
                Class<?> pClass = Class.forName(pluginName);
                if(ClientSessionListener.class.isAssignableFrom(pClass)){
                    this.customListener = (ClientSessionListener) pClass.newInstance();
                }
            } catch (Exception e) {}
            logger.info("MQTT client state notify use: topic->{}, plugin->{}",
                    clientConfig.isUseTopicNotify() ? NOTIFY_TOPIC : "false",
                    this.customListener != null ? this.customListener.getClass() : "not set");
        }

    }

    @Override
    public void onSessionOpen(MqttSession session) {
        //TODO 在集群中广播客户端上线。目的是离线消息重传
        ClientConfig clientConfig = this.server.getConfiguration().getClientConfig();
        if(clientConfig != null && clientConfig.isUseTopicNotify()){
            // TODO
        }
        if(this.customListener != null){
            this.customListener.onSessionOpen(session);
        }
    }

    @Override
    public void onSessionClose(MqttSession session) {
        //TODO 在集群中广播客户端离线
        ClientConfig clientConfig = this.server.getConfiguration().getClientConfig();
        if(clientConfig != null && clientConfig.isUseTopicNotify()){
            // TODO
        }
        if(this.customListener != null){
            this.customListener.onSessionClose(session);
        }
    }
}
