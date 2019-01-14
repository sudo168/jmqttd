package net.ewant.jmqttd.cluster;

import java.util.Collection;

/**
 * peer discovery protocol
 * --------------------------------------------------------------------------------------------------------
 * - length     |    packet type        |    group id length    |       group id       |    payload       -
 * -            |                       |                       |                      |                  -
 * - 2byte      |  1byte(use high 4bit) |         1 byte        | group id length byte |  payload bytes   -
 * --------------------------------------------------------------------------------------------------------
 * 
 * 集群节点发现
 * @author hoey
 */
public interface PeerDiscover {
	
	int MAX_FRAME_LENGTH = 0xFFFF;
	
	int MAX_GROUP_ID_LENGTH = 0xFF;

	void registerPeer(Peer peer);
	
	void unregisterPeer(Peer peer);
	
	Peer getPeer(String id);
	
	Collection<Peer> listRemotePeers();
	
	void init();
	
	void dispose();
}
