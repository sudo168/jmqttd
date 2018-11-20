package com.ewant.jmqttd.core;

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.ewant.jmqttd.config.HostPortSslConfiguration;
import com.ewant.jmqttd.config.ServerConfiguration;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;

public abstract class AbstractHandlerChainAdapter<T extends HostPortSslConfiguration> extends ChannelInitializer<Channel> implements Closeable{

    private SSLContext sslContext;
    
    protected ServerConfiguration configuration;
    
    protected T hostPortSsl;
    
    public void start(ServerConfiguration configuration, T hostPortSsl) {
    	this.configuration = configuration;
    	this.hostPortSsl = hostPortSsl;
        if (hostPortSsl.isSsl()) {
            try {
                sslContext = createSSLContext(hostPortSsl);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        addSslHandler(pipeline);
        // 防止TCP粘包（裸协议才有必要）
        //pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
		//pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        addSocketHandlers(pipeline);
    }

    /**
     * Adds the ssl handler
     *
     * @return
     */
    protected void addSslHandler(ChannelPipeline pipeline) {
        if (sslContext != null) {
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            engine.setEnabledProtocols(hostPortSsl.getSslProtocol().split(","));
            pipeline.addLast("SSL_HANDLER", new SslHandler(engine));
        }
    }

    /**
     * Adds channel handlers
     *
     * @param pipeline
     */
    public abstract void addSocketHandlers(ChannelPipeline pipeline) ;

    private SSLContext createSSLContext(T configuration) throws Exception {
        TrustManager[] managers = null;
        if (configuration.getTrustStore() != null) {
            KeyStore ts = KeyStore.getInstance(configuration.getTrustStoreFormat());
            ts.load(configuration.getTrustStore(), configuration.getTrustStorePassword().toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            managers = tmf.getTrustManagers();
        }

        KeyStore ks = KeyStore.getInstance(configuration.getKeyStoreFormat());
        ks.load(configuration.getKeyStore(), configuration.getKeyStorePassword().toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, configuration.getKeyStorePassword().toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(kmf.getKeyManagers(), managers, null);
        return sslContext;
    }

    @Override
    public void close() {

    }
    
}
