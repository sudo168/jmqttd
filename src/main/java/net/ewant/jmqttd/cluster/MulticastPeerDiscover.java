package net.ewant.jmqttd.cluster;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.ewant.jmqttd.config.impl.ClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A multicast group is specified by a class D IP address
 * and by a standard UDP port number. Class D IP addresses
 * are in the range 224.0.0.0 to 239.255.255.255,
 * inclusive. The address 224.0.0.0 is reserved and should not be used.
 * 
 * timeToLive TTL:
 * 0 is restricted to the same host
 * 1 is restricted to the same subnet
 * 32 is restricted to the same site
 * 64 is restricted to the same region
 * 128 is restricted to the same continent
 * 255 is unrestricted
 * @author hoey
 */
public class MulticastPeerDiscover implements PeerDiscover {
	
	private static final Logger logger = LoggerFactory.getLogger(MulticastPeerDiscover.class);
	
	private static String MULTICAST_GROUP_ADDRESS = "226.7.8.9";
	
	private Map<String, Peer> peers = new HashMap<>();
	
	private Peer self;
	
	private ClusterConfig config;
	
	private PeerListener peerListener;
	
	private MulticastKeepaliveHeartbeatSender heartbeatSender;
	
	private MulticastKeepaliveHeartbeatReceiver heartbeatReceiver;
	
	public MulticastPeerDiscover(ClusterConfig config){
		this(config, null);
	}
	
	public MulticastPeerDiscover(ClusterConfig config, PeerListener peerListener){
		this.config = config;
		this.self = new Peer(config.getGroupId(), config.getHost(), config.getPort());
		this.setPeerListener(peerListener);
	}

	public void setPeerListener(PeerListener peerListener) {
		this.peerListener = peerListener;
	}
	
	@Override
	public void registerPeer(Peer peer) {
		Peer exists = peers.get(peer.getId());
		if(exists != null){
			exists.setLatestUpdateTime(System.currentTimeMillis());
		}else{
			peers.put(peer.getId(), peer);
			if(peerListener != null){
				peerListener.peerJoin(peer);
			}
		}
	}

	@Override
	public void unregisterPeer(Peer peer) {
		Peer remove = peers.remove(peer.getId());
		if(peerListener != null){
			peerListener.peerLeave(remove);
		}
	}

	@Override
	public Collection<Peer> listRemotePeers() {
		Iterator<Entry<String, Peer>> iterator = peers.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Peer> entry = (Map.Entry<String, Peer>) iterator.next();
			if(stale(entry.getValue())){
				iterator.remove();
				if(peerListener != null){
					peerListener.peerLeave(entry.getValue());
				}
			}
		}
		return peers.values();
	}
	
	public Peer getSelf() {
		return self;
	}

	@Override
	public void init(){
		try {
			InetAddress multicastAddress = InetAddress.getByName(MULTICAST_GROUP_ADDRESS);
			Integer multicastPort = config.getPort();
			Integer timeToLive = config.getTtl().getValue();
			InetAddress hostAddress = InetAddress.getByName(config.getHost());
			
			this.heartbeatSender = new MulticastKeepaliveHeartbeatSender(this, multicastAddress, multicastPort, timeToLive, hostAddress);
			this.heartbeatReceiver = new MulticastKeepaliveHeartbeatReceiver(this, multicastAddress, multicastPort, hostAddress);
			
			this.heartbeatSender.init();
			this.heartbeatReceiver.init();
		} catch (Exception e) {
			logger.error("init Multicast Discovery error. " + e.getMessage(), e);
		}
	}
	
	@Override
	public void dispose() {
		this.heartbeatSender.dispose();
		this.heartbeatReceiver.dispose();
	}
	
	/**
	 * 根据最后心跳时间判断节点是否已失联
	 * @return true if No heartbeat for a long time
	 */
	protected final boolean stale(Peer peer) {
        long now = System.currentTimeMillis();
        return peer.getLatestUpdateTime() < (now - getStaleTime());
    }

	/**
	 * 获取失联毫秒数，心跳间隔大于此值就表示节点失联
	 * @return
	 */
	private long getStaleTime() {
		return MulticastKeepaliveHeartbeatSender.getHeartBeatStaleTime();
	}

	@Override
	public Peer getPeer(String id) {
		return peers.get(id);
	}
	
}
