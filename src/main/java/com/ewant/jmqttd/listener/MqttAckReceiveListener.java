package com.ewant.jmqttd.listener;

import com.ewant.jmqttd.codec.message.MqttConnAck;
import com.ewant.jmqttd.codec.message.MqttPong;
import com.ewant.jmqttd.codec.message.MqttPubAck;
import com.ewant.jmqttd.codec.message.MqttPubComp;
import com.ewant.jmqttd.codec.message.MqttPubRec;
import com.ewant.jmqttd.codec.message.MqttSubAck;
import com.ewant.jmqttd.codec.message.MqttUnsubAck;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public interface MqttAckReceiveListener {
	
	void onPubAck(MqttSession client, MqttPubAck message);
	void onPubRec(MqttSession client, MqttPubRec message);
	void onPubComp(MqttSession client, MqttPubComp message);
	
	void onSubAck(MqttSession client, MqttSubAck message);
	void onUnsubAck(MqttSession client, MqttUnsubAck message);
	void onPong(MqttSession client, MqttPong message);
	
	void onConnAck(MqttSession client, MqttConnAck ack);
	
}
