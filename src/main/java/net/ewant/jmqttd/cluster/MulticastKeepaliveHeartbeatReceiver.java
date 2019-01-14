package net.ewant.jmqttd.cluster;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ewant.jmqttd.utils.ProtocolUtil;

public class MulticastKeepaliveHeartbeatReceiver {

    private static final Logger logger = LoggerFactory.getLogger(MulticastKeepaliveHeartbeatReceiver.class.getName());

    private MulticastPeerDiscover peerDiscover;
    
    private final InetAddress groupMulticastAddress;
    private final Integer groupMulticastPort;
    private MulticastReceiverThread receiverThread;
    private MulticastSocket socket;
    private volatile boolean stopped;
    private InetAddress hostAddress;

    public MulticastKeepaliveHeartbeatReceiver(
    		MulticastPeerDiscover peerDiscover,
    		InetAddress multicastAddress,
    		Integer multicastPort,
            InetAddress hostAddress) {
        this.peerDiscover = peerDiscover;
        this.groupMulticastAddress = multicastAddress;
        this.groupMulticastPort = multicastPort;
        this.hostAddress = hostAddress;
    }


    /**
     * Start.
     *
     * @throws IOException
     */
    final void init() throws IOException {
        socket = new MulticastSocket(groupMulticastPort.intValue());
        if (hostAddress != null) {
            socket.setInterface(hostAddress);
        }
        socket.joinGroup(groupMulticastAddress);
        receiverThread = new MulticastReceiverThread();
        receiverThread.start();
    }

    /**
     * Shutdown the heartbeat.
     */
    public final void dispose() {
        logger.debug("dispose called");
        stopped = true;
        receiverThread.interrupt();
    }

    /**
     * A multicast receiver which continously receives heartbeats.
     */
    private final class MulticastReceiverThread extends Thread {

        /**
         * Constructor
         */
        public MulticastReceiverThread() {
            super("Multicast Heartbeat Receiver");
            setDaemon(true);
        }

        @Override
        public final void run() {
            byte[] buf = new byte[ProtocolUtil.MTU];
            try {
                while (!stopped) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                        byte[] payload = packet.getData();
                        processPayload(packet.getLength(), payload, packet.getAddress().getHostAddress(), packet.getPort());
                    } catch (IOException e) {
                        if (!stopped) {
                            logger.error("Error receiving heartbeat. " + e.getMessage() +
                                    ". Initial cause was " + e.getMessage(), e);
                        }
                    }
                }
            } catch (Throwable t) {
                logger.error("Multicast receiver thread caught throwable. Cause was " + t.getMessage() + ". Continuing...");
            }
        }

        private void processPayload(int total, byte[] payload, String host, int port) {
        	logger.debug("Receive Multicast discovery packet from peer[{}:{}]", host, port);
        	Peer self = peerDiscover.getSelf();
        	if(payload == null || payload.length < 2){
        		return;
        	}
        	int readIndex = 0;
        	byte low = payload[readIndex++];
        	byte high = payload[readIndex++];
        	int packetLength = ((high << 8) + low) & 0xFFFF;
        	if(packetLength + readIndex != total){
        		logger.error("Unknown Multicast discovery packet.");
        		return;
        	}
        	int type = payload[readIndex++];
        	if(type != 0){// type 0
        		logger.error("Unknown Multicast discovery packet type: {}", type);
        		return;
        	}
        	int gLength = payload[readIndex++];
        	byte[] groupIdBytes = new byte[gLength];
        	for (int i = 0; i < gLength; i++, readIndex++) {
        		groupIdBytes[i] = payload[readIndex];
			}
        	String groupId = new String(groupIdBytes, Charset.forName("UTF-8"));
        	if(!self.getGroupId().equals(groupId)){
        		logger.error("Multicast discovery peer is not a same group: {} VS {}", self.getGroupId(), groupId);
        		return;
        	}
        	byte[] peerIdBytes = new byte[total - readIndex];
        	int pLength = peerIdBytes.length;
        	for (int i = 0; i < pLength; i++, readIndex++) {
        		peerIdBytes[i] = payload[readIndex];
			}
        	String peerId = new String(peerIdBytes, Charset.forName("UTF-8"));
        	if(self.getId().equals(peerId)){// is self
        		return;
        	}
        	String[] hostAndPort = peerId.split(":");
        	if(hostAndPort.length == 2){
        		peerDiscover.registerPeer(new Peer(groupId, hostAndPort[0], Integer.parseInt(hostAndPort[1])));
        		return;
        	}
        	logger.error("Multicast discovery peer invalid ID: {}", peerId);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final void interrupt() {
            try {
                socket.leaveGroup(groupMulticastAddress);
            } catch (IOException e) {
                logger.error("Error leaving group");
            }
            socket.close();
            super.interrupt();
        }
    }

}
