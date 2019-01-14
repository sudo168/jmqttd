package net.ewant.jmqttd.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.ewant.jmqttd.codec.message.MqttPublish;
import net.ewant.jmqttd.persistent.FileSystemMqttPublishPersistence;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public class PersistenceUtil {
	
	private static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");
	private static SimpleDateFormat YEAR_MONTH_FORMAT = new SimpleDateFormat("yyyyMM");
	private static SimpleDateFormat MESSAGEID_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

	/**
	 * 持久化到硬盘的key
	 * @param client
	 * @param message
	 * @return
	 */
	public static String getStoreKey(MqttSession client, MqttPublish message){
		StringBuilder sb = new StringBuilder();
		sb.append(message.getTopic().getName());
		sb.append(",");
		sb.append(client.getId());
		sb.append(",");
		sb.append(YEAR_MONTH_FORMAT.format(new Date(message.getMessageTime())));
		sb.append(",");
		sb.append(message.getMessageId());
		return sb.toString();
	}
	
	/**
	 * 暂存内存的key
	 * 存储的时候，需要解析出clientId，因此 分隔符号不要轻易改
	 * @param clientId
	 * @param messageId
	 * @see FileSystemMqttPublishPersistence.run()
	 * @return
	 */
	public static String getTempStoreKey(String clientId, String topicName, int messageId){
		StringBuilder sb = new StringBuilder();
		sb.append(clientId);
		sb.append("@");
		sb.append(topicName);
		sb.append("-");
		sb.append(messageId);
		return sb.toString();
	}
	
	/**
	 * @return 201804061240324026599244091471
	 */
	public static String generateMessageId(){
		StringBuilder sb = new StringBuilder();
		sb.append(MESSAGEID_FORMAT.format(new Date()));
		sb.append(System.nanoTime());
		return sb.toString();
	}
	
	public static byte[] double2Bytes(double d) {  
        long value = Double.doubleToRawLongBits(d);  
        byte[] bytes = new byte[8];  
        for (int i = 0; i < 8; i++) {  
        	bytes[i] = (byte) ((value >> 8 * i) & 0xff);  
        }  
        return bytes;  
    }
	
	public static double bytes2Double(byte[] bytes) {  
        long value = 0;  
        for (int i = 0; i < 8; i++) {  
            value |= ((long) (bytes[i] & 0xff)) << (8 * i);  
        }  
        return Double.longBitsToDouble(value);  
    }
	
	public static long parseMillis(String dateMark){
		if(dateMark == null || dateMark.trim().length() < 2){
			throw new IllegalArgumentException("无效的时间表达式：[" + dateMark + "], 示例：300000S（30万毫秒）, 30s（30秒）, 10m|10M（10分钟）, 8h|8H（8小时）, 1d|1D（天）");
		}
		String mark = dateMark.trim();
		long num = Long.parseLong(mark.substring(0, mark.length() - 1));
		String unit = mark.substring(mark.length() - 1);
		long result = 0;
		switch (unit) {
			case "S":
				result = num;
				break;
			case "s":
				result = num * 1000;
				break;
			case "m":
			case "M":
				result = num * 60 * 1000;
				break;
			case "h":
			case "H":
				result = num * 60 * 60 * 1000;
				break;
			case "d":
			case "D":
				result = num * 24 * 60 * 60 * 1000;
				break;
			default :
				throw new IllegalArgumentException("无效的时间表达式：[" + dateMark + "], 示例：300000S（30万毫秒）, 30s（30秒）, 10m|10M（10分钟）, 8h|8H（8小时）, 1d|1D（天）");
		}
		return result;
	}
	
	public static long parseBytes(String sizeMark){
		if(sizeMark == null || sizeMark.trim().length() < 2){
			throw new IllegalArgumentException("无效的容量表达式：[" + sizeMark + "], 示例：1024b（1024 字节）, 10k（1kb）, 1M（1兆）, 1G（1G）");
		}
		String mark = sizeMark.trim();
		long num = Long.parseLong(mark.substring(0, mark.length() - 1));
		String unit = mark.substring(mark.length() - 1);
		long result = 0;
		switch (unit) {
			case "b":
				result = num;
				break;
			case "k":
				result = num * 1024;
				break;
			case "M":
				result = num * 1024 * 1024;
				break;
			case "G":
				result = num * 1024 * 1024 * 1024;
				break;
			default :
				throw new IllegalArgumentException("无效的容量表达式：[" + sizeMark + "], 示例：1024b（1024 字节）, 10k（1kb）, 1M（1兆）, 1G（1G）");
		}
		return result;
	}
	
}