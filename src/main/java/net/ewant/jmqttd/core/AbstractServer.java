package net.ewant.jmqttd.core;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import net.ewant.jmqttd.handler.MqttHttpHandlerChainAdapter;
import net.ewant.jmqttd.cluster.PeerDiscover;
import net.ewant.jmqttd.config.HostPortSslConfiguration;
import net.ewant.jmqttd.config.ServerConfiguration;
import net.ewant.jmqttd.config.impl.ClusterConfig;
import net.ewant.jmqttd.config.impl.SocketConfig;
import net.ewant.jmqttd.handler.MqttHandlerChainAdapter;
import net.ewant.jmqttd.handler.MqttWebSocketHandlerChainAdapter;
import net.ewant.jmqttd.server.mqtt.MqttServer;
import net.ewant.jmqttd.server.mqtt.MqttServerContext;
import net.ewant.jmqttd.utils.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ewant.jmqttd.cluster.MulticastPeerDiscover;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public abstract class AbstractServer<T extends HostPortSslConfiguration> {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	protected static PeerDiscover peerDiscover;
	
	protected static CountDownLatch CLUSTER_START_WATCH;

    protected final ServerConfiguration configuration;
    
    protected final T hostPortSsl;

    private AbstractHandlerChainAdapter<T> pipelineFactory;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @SuppressWarnings("unchecked")
	public AbstractServer(ServerConfiguration configuration,T hostPortSsl) {
        this.configuration = configuration;
        this.hostPortSsl = hostPortSsl;
        this.pipelineFactory = createPipelineFactory(hostPortSsl.getProtocol());
    }

    @SuppressWarnings("rawtypes")
	private AbstractHandlerChainAdapter createPipelineFactory(ServerProtocol protocol) {
        switch (protocol){
        case CLUSTER:
            case TCP:
            case TLS:
                return new MqttHandlerChainAdapter();
            case WS:
            case WSS:
                return  new MqttWebSocketHandlerChainAdapter();
            case HTTP:
            case HTTPS:
                return new MqttHttpHandlerChainAdapter();
            default:
                return null;
        }
    }

    public ServerProtocol getProtocol(){
        return hostPortSsl.getProtocol();
    }
    
    public static PeerDiscover getPeerDiscover() {
		return peerDiscover;
	}

    /**
     * Start server
     */
    public void start() {
        Future<Void> future = startAsync();
        if (future != null){
            future.syncUninterruptibly();
        }
    }

    /**
     * Start server asynchronously
     */
    private Future<Void> startAsync() {

        try {

            if(configuration == null){
                throw new NullPointerException("server configuration can not be NULL!");
            }
            if(hostPortSsl == null){
                throw new NullPointerException("hostPortSsl config can not be NULL!");
            }
            if(pipelineFactory == null){
                throw new NullPointerException("pipelineFactory can not be NULL!");
            }

            final long start = System.currentTimeMillis();

            initGroups();

            initListeners();

            initInterceptors();
            
            initStorage();

            pipelineFactory.start(configuration, hostPortSsl);

            Class<? extends ServerChannel> channelClass = NioServerSocketChannel.class;
            if (Epoll.isAvailable()) {
                channelClass = EpollServerSocketChannel.class;
            }
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(channelClass)
            .childHandler(pipelineFactory);
            applyConnectionOptions(b);

            InetSocketAddress addr = new InetSocketAddress(hostPortSsl.getPort());
            if (hostPortSsl.getHost() != null) {
                addr = new InetSocketAddress(hostPortSsl.getHost(), hostPortSsl.getPort());
            }
            //final String simpleName = this.getClass().getSimpleName();
            return b.bind(addr).addListener(new FutureListener<Void>() {
                public void operationComplete(Future<Void> future) throws Exception {
                	CLUSTER_START_WATCH.countDown();
                    if (future.isSuccess()) {
                        logger.info("[{}] server started in {}ms. listen at port: {}", getProtocol(), (System.currentTimeMillis() - start), hostPortSsl.getPort());
                    } else {
                        logger.error("[{}] server start failed at port: {}!", getProtocol(), hostPortSsl.getPort());
                    }
                }
            });
        } catch (Exception e) {
        	CLUSTER_START_WATCH.countDown();
            logger.error("[{}] server start error at port: {}. cause: {} [{}] , at {}", getProtocol(), hostPortSsl.getPort(), e.getClass().getName(), e.getMessage(), ReflectUtil.getAvailableStack(e));
        }
        return null;
    }

    private void applyConnectionOptions(ServerBootstrap bootstrap) {
        SocketConfig config = configuration.getSocketConfig();
        bootstrap.childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay());
        if (config.getTcpSendBufferSize() > 0) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, config.getTcpSendBufferSize());
        }
        if (config.getTcpReceiveBufferSize() > 0) {
            bootstrap.childOption(ChannelOption.SO_RCVBUF, config.getTcpReceiveBufferSize());
            bootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(config.getTcpReceiveBufferSize()));
        }
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, config.isTcpKeepAlive());

        //bootstrap.option(ChannelOption.SO_LINGER, config.getSoLinger()); // invalid in NIO channel
        bootstrap.option(ChannelOption.SO_REUSEADDR, config.isReuseAddress());
        bootstrap.option(ChannelOption.SO_BACKLOG, config.getBackLog());
    }

    private void initGroups() {
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(configuration.getServerConfig().getAcceptors(), new NameGroupThreadFactory(getProtocol().name() + "@epoll-accptor"));
            workerGroup = new EpollEventLoopGroup(configuration.getServerConfig().getWorkers(), new NameGroupThreadFactory(getProtocol().name() + "@epoll-worker"));
        } else {
            bossGroup = new NioEventLoopGroup(configuration.getServerConfig().getAcceptors(), new NameGroupThreadFactory(getProtocol().name() + "@nio-accptor"));
            workerGroup = new NioEventLoopGroup(configuration.getServerConfig().getWorkers(), new NameGroupThreadFactory(getProtocol().name() + "@nio-worker"));
        }
    }
    
    protected static void initCluster(ServerConfiguration configuration) {
    	// TODO 监听tcp集群数据传输端口
		ClusterConfig clusterConfig = configuration.getClusterConfig();
		if(clusterConfig != null && clusterConfig.valid()){
			MqttServer cluster = new MqttServer(configuration, clusterConfig.toHostPortConfig());
			MqttServerContext.addServer(cluster);
			peerDiscover = new MulticastPeerDiscover(clusterConfig, cluster);
			peerDiscover.init();
			cluster.start();
		}
	}

    /**
     * Stop server
     */
    public void stop() {
        bossGroup.shutdownGracefully().syncUninterruptibly();
        workerGroup.shutdownGracefully().syncUninterruptibly();
        pipelineFactory.close();
    }

    public ServerConfiguration getConfiguration() {
        return configuration;
    }

    public abstract void initListeners();

    public abstract void initInterceptors();
    
    public abstract void initStorage();
    
}
