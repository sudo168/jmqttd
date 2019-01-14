package net.ewant.jmqttd.cluster;

public interface PeerListener {
	
	void peerJoin(Peer peer);
	
	void peerLeave(Peer peer);
}
