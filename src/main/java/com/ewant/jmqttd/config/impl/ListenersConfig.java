package com.ewant.jmqttd.config.impl;

import com.ewant.jmqttd.config.HostPortSslConfiguration;
import com.ewant.jmqttd.core.ServerProtocol;

import java.util.ArrayList;
import java.util.List;

public class ListenersConfig {
	
	private TcpConfig tcp;
	private TlsConfig tls;
	private WsConfig ws;
	private WssConfig wss;
	private HttpConfig http;
	private HttpsConfig https;

	public List<HostPortSslConfiguration> getActives(){
		List<HostPortSslConfiguration> all = new ArrayList<>();
		if(tcp != null && tcp.isEnable()) all.add(tcp);
		if(tls != null && tls.isEnable()) all.add(tls);
		if(ws != null && ws.isEnable()) all.add(ws);
		if(wss != null && wss.isEnable()) all.add(wss);
		if(http != null && http.isEnable()) all.add(http);
		if(https != null && https.isEnable()) all.add(https);
		return all;
	}
	
	public TcpConfig getTcp() {
		return tcp;
	}

	public void setTcp(TcpConfig tcp) {
		this.tcp = tcp;
	}

	public TlsConfig getTls() {
		return tls;
	}

	public void setTls(TlsConfig tls) {
		this.tls = tls;
	}

	public WsConfig getWs() {
		return ws;
	}

	public void setWs(WsConfig ws) {
		this.ws = ws;
	}

	public WssConfig getWss() {
		return wss;
	}

	public void setWss(WssConfig wss) {
		this.wss = wss;
	}

	public HttpConfig getHttp() {
		return http;
	}

	public void setHttp(HttpConfig http) {
		this.http = http;
	}

	public HttpsConfig getHttps() {
		return https;
	}

	public void setHttps(HttpsConfig https) {
		this.https = https;
	}

	public static class TcpConfig extends HostPortSslConfiguration{
		public TcpConfig(){
			this.setProtocol(ServerProtocol.TCP);
		}
	}
	
	public static class TlsConfig extends HostPortSslConfiguration{
		public TlsConfig(){
			this.setProtocol(ServerProtocol.TLS);
		}
		@Override
		public boolean isSslRequired() {
			return true;
		}
	}
	
	public static class WsConfig extends HostPortSslConfiguration{
		public WsConfig(){
			this.setProtocol(ServerProtocol.WS);
		}
	}
	
	public static class WssConfig extends HostPortSslConfiguration{
		public WssConfig(){
			this.setProtocol(ServerProtocol.WSS);
		}
		@Override
		public boolean isSslRequired() {
			return true;
		}
	}
	
	public static class HttpConfig extends HostPortSslConfiguration{
		public HttpConfig(){
			this.setProtocol(ServerProtocol.HTTP);
		}
	}
	
	public static class HttpsConfig extends HostPortSslConfiguration{
		public HttpsConfig(){
			this.setProtocol(ServerProtocol.HTTPS);
		}
		@Override
		public boolean isSslRequired() {
			return true;
		}
	}
}
