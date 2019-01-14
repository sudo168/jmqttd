package net.ewant.jmqttd.config;

import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.utils.ProtocolUtil;

import java.io.InputStream;

public class HostPortSslConfiguration implements DetachableConfig{

    private String host;
    private int port;
    
    private ServerProtocol protocol;

    private String sslProtocol = "TLSv1,TLSv1.1,TLSv1.2";

    private String keyStoreFormat = "JKS";
    private InputStream keyStore;
    private String keyStorePassword;

    private String trustStoreFormat = "JKS";
    private InputStream trustStore;
    private String trustStorePassword;
    
    private boolean enable;

    private int handshakeTimeout = 15;
    
	public String getHost() {
		if(host == null){
			return ProtocolUtil.getLocalHost();
		}
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getKeyStoreFormat() {
        return keyStoreFormat;
    }

    public void setKeyStoreFormat(String keyStoreFormat) {
        this.keyStoreFormat = keyStoreFormat;
    }

    public InputStream getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(InputStream keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getTrustStoreFormat() {
        return trustStoreFormat;
    }

    public void setTrustStoreFormat(String trustStoreFormat) {
        this.trustStoreFormat = trustStoreFormat;
    }

    public InputStream getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(InputStream trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public int getHandshakeTimeout() {
        return handshakeTimeout;
    }

    public void setHandshakeTimeout(int handshakeTimeout) {
        this.handshakeTimeout = handshakeTimeout;
    }

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public boolean isSsl(){
		return this.keyStore != null;
	}

    public boolean isSslRequired(){
        return false;
    }

    public ServerProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(ServerProtocol protocol) {
        this.protocol = protocol;
    }
}
