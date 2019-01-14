package net.ewant.jmqttd.listener;

import net.ewant.jmqttd.codec.message.MqttConnect;
import net.ewant.jmqttd.codec.message.MqttPing;
import net.ewant.jmqttd.codec.message.MqttPubRel;
import net.ewant.jmqttd.codec.message.MqttPublish;
import net.ewant.jmqttd.codec.message.MqttSubscribe;
import net.ewant.jmqttd.codec.message.MqttUnsubscribe;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public interface MqttMessageArriveListener {
	
	void onPublish(MqttSession client, MqttPublish message);
	void onPubRel(MqttSession client, MqttPubRel message);
	
	void onSubscribe(MqttSession client, MqttSubscribe message);
	void onUnsubscribe(MqttSession client, MqttUnsubscribe message);
	void onPing(MqttSession client, MqttPing message);
	
	void onConnect(MqttSession client, MqttConnect connect);
	
	void onDisconnect(MqttSession client);
	
}
