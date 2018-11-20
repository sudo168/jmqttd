package com.ewant.jmqttd.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;

public class ProtocolUtil {
	
	private static Logger logger = LoggerFactory.getLogger(ProtocolUtil.class);
	
	public static final int MTU = 1500;

    public static boolean isFlashSocket(ByteBuf buf) {
        if (buf.writableBytes() < 22) {
            return false;
        }
        buf.markReaderIndex();
        CharSequence seq = buf.readCharSequence(22, CharsetUtil.UTF_8);
        if (seq.toString().equals("<policy-file-request/>")) {
            return true;
        }
        buf.resetReaderIndex();
        return false;
    }

    public static String flashSocketCrossAllow(){
        // 针对flash Socket 跨域  支持 。 可添加ip白名单
        String crossStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                +"<!DOCTYPE cross-domain-policy SYSTEM \"http://www.macromedia.com/xml/dtds/cross-domain-policy.dtd\">"
                +"<cross-domain-policy>"
                +"<site-control permitted-cross-domain-policies=\"all\"/>"
                +"<allow-access-from domain=\"*\" to-ports=\"*\"/>"
                +"<allow-http-request-headers-from domain=\"*\" headers=\"*\"/>"
                +"</cross-domain-policy>";
        return crossStr;
    }
    
    public static String toSessionId(Channel channel){
    	InetSocketAddress localAddress = (InetSocketAddress)channel.localAddress();
    	InetSocketAddress remoteAddress = (InetSocketAddress)channel.remoteAddress();
    	StringBuilder sb = new StringBuilder();
    	sb.append(localAddress.getHostString());
    	sb.append(":");
    	sb.append(localAddress.getPort());
    	sb.append("/");
    	sb.append(remoteAddress.getHostString());
    	sb.append(":");
    	sb.append(remoteAddress.getPort());
    	return sb.toString();
    }
    
    public static byte[] gzip(byte[] ungzipped) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bytes);
            gzipOutputStream.write(ungzipped);
            gzipOutputStream.close();
        } catch (IOException e) {
        	logger.error("Could not gzip " + Arrays.toString(ungzipped));
        }
        return bytes.toByteArray();
    }
    
    public static byte[] ungzip(final byte[] gzipped) {
        byte[] ungzipped = new byte[0];
        try {
            final GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(gzipped));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(gzipped.length);
            final byte[] buffer = new byte[MTU];
            int bytesRead = 0;
            while (bytesRead != -1) {
                bytesRead = inputStream.read(buffer, 0, MTU);
                if (bytesRead != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
            }
            ungzipped = byteArrayOutputStream.toByteArray();
            inputStream.close();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            logger.error("Could not ungzip. Heartbeat will not be working. " + e.getMessage());
        }
        return ungzipped;
    }
    
    public static boolean isWindowsOS() {
        boolean isWindowsOS = false;
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().indexOf("windows") > -1) {
            isWindowsOS = true;
        }
        return isWindowsOS;
    }
    
    public static String getLocalHost(){
		String host = null;
		try {
			InetAddress localHost = InetAddress.getLocalHost();
			host = localHost.getHostAddress();
		} catch (UnknownHostException e) {
			try {
				InetAddress inetAddress = null;
				Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
				while (networkInterfaces.hasMoreElements() && host == null) {
					NetworkInterface networkInterface = (NetworkInterface) networkInterfaces.nextElement();
					Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
					while (inetAddresses.hasMoreElements() && host == null) {
						inetAddress = (InetAddress) inetAddresses.nextElement();
						if (inetAddress instanceof Inet4Address) {
							host = inetAddress.getHostAddress();
						}
					}
				}
			} catch (SocketException e1) {
				host = "127.0.0.1";
			}
		}
		return host;
	}
    
    public static Map<String, String> parseQueryString(String query){
		Map<String, String> paramsMap = new HashMap<>();
		if(query != null){
			String[] properties = query.split("&");
			for (String kv : properties) {
				String[] mapper = kv.split("=");
				paramsMap.put(mapper[0], mapper.length > 1 ? mapper[1] : "");
			}
		}
		return paramsMap;
	}
}
