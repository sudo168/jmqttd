package com.ewant.jmqttd.codec;

import com.ewant.jmqttd.codec.message.MqttFixedHeader;
import com.ewant.jmqttd.codec.message.MqttQoS;
import com.ewant.jmqttd.codec.message.MqttVersion;

public class MqttCodecUtils {

    private static final char[] TOPIC_WILDCARDS = {'#', '+'};
    private static final int MIN_CLIENT_ID_LENGTH = 1;
    private static final int MAX_CLIENT_ID_LENGTH = 23;

	public static MqttFixedHeader isValidFixedHeader(MqttFixedHeader fixedHeader) throws MqttException{
        switch (fixedHeader.getMessageType()) {
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                if (fixedHeader.getQos() != MqttQoS.AT_LEAST_ONCE) {
                	throw new MqttException("MQTT [" + fixedHeader.getMessageType().name() + "] message must have QoS 1");
                }
            default:
                return fixedHeader;
        }
    }

    public static boolean isValidPublishTopicName(String topicName) throws MqttException{
        for (char c : TOPIC_WILDCARDS) {
            if (topicName.indexOf(c) >= 0) {
                throw new MqttException("not support char [" + c + "] in publish topic name [" + topicName + "]");
            }
        }
        return true;
    }

    public static boolean isValidClientId(MqttVersion mqttVersion, String clientId) throws MqttException{
        if (mqttVersion == MqttVersion.MQTT_31) {
            boolean valid = clientId != null && clientId.length() >= MIN_CLIENT_ID_LENGTH &&
                    clientId.length() <= MAX_CLIENT_ID_LENGTH;
            if(!valid){
                throw new MqttException("Invalid client id [" + clientId + "] in mqtt version " + mqttVersion + " . Support length between 1 and 23");
            }
            return true;
        }
        if (mqttVersion == MqttVersion.MQTT_311) {
            // In 3.1.3.1 Client Identifier of MQTT 3.1.1 specification, The Server MAY allow ClientId’s
            // that contain more than 23 encoded bytes. And, The Server MAY allow zero-length ClientId.
            if(clientId == null || clientId.length() == 0){
                throw new MqttException("Client id [" + clientId + "] must be not empty in mqtt version " + mqttVersion);
            }
            return true;
        }
        throw new MqttException(mqttVersion + " is unknown mqtt version");
    }
}
