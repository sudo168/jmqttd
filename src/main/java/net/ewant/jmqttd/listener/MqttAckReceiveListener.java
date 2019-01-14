package net.ewant.jmqttd.listener;

import net.ewant.jmqttd.codec.message.MqttConnAck;
import net.ewant.jmqttd.codec.message.MqttPong;
import net.ewant.jmqttd.codec.message.MqttPubAck;
import net.ewant.jmqttd.codec.message.MqttPubComp;
import net.ewant.jmqttd.codec.message.MqttPubRec;
import net.ewant.jmqttd.codec.message.MqttSubAck;
import net.ewant.jmqttd.codec.message.MqttUnsubAck;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public interface MqttAckReceiveListener {
	
	void onPubAck(MqttSession client, MqttPubAck message);
	void onPubRec(MqttSession client, MqttPubRec message);
	void onPubComp(MqttSession client, MqttPubComp message);
	
	void onSubAck(MqttSession client, MqttSubAck message);
	void onUnsubAck(MqttSession client, MqttUnsubAck message);
	void onPong(MqttSession client, MqttPong message);
	
	void onConnAck(MqttSession client, MqttConnAck ack);
	
}
