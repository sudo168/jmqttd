package com.ewant.jmqttd.listener;

import com.ewant.jmqttd.codec.message.MqttConnect;
import com.ewant.jmqttd.codec.message.MqttPing;
import com.ewant.jmqttd.codec.message.MqttPubRel;
import com.ewant.jmqttd.codec.message.MqttPublish;
import com.ewant.jmqttd.codec.message.MqttSubscribe;
import com.ewant.jmqttd.codec.message.MqttUnsubscribe;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public interface MqttMessageArriveListener {
	
	void onPublish(MqttSession client, MqttPublish message);
	void onPubRel(MqttSession client, MqttPubRel message);
	
	void onSubscribe(MqttSession client, MqttSubscribe message);
	void onUnsubscribe(MqttSession client, MqttUnsubscribe message);
	void onPing(MqttSession client, MqttPing message);
	
	void onConnect(MqttSession client, MqttConnect connect);
	
	void onDisconnect(MqttSession client);
	
}
