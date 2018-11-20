package com.ewant.jmqttd.cluster;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MulticastKeepaliveHeartbeatSender {


    private static final Logger logger = LoggerFactory.getLogger(MulticastKeepaliveHeartbeatSender.class);

    private static final int DEFAULT_HEARTBEAT_INTERVAL = 6000;
    private static final int MINIMUM_HEARTBEAT_INTERVAL = 1500;
    private static final int ONE_HUNDRED_MS = 200;

    private static long heartBeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
    private static long heartBeatStaleTime = -1;

    private MulticastPeerDiscover peerDiscover;
    
    private final InetAddress groupMulticastAddress;
    private final Integer groupMulticastPort;
    private final Integer timeToLive;
    private MulticastServerThread serverThread;
    private volatile boolean stopped;
    private InetAddress hostAddress;

    public MulticastKeepaliveHeartbeatSender(
    		MulticastPeerDiscover peerDiscover,
    		InetAddress multicastAddress,
    		Integer multicastPort,
            Integer timeToLive,
            InetAddress hostAddress) {
    	this.peerDiscover = peerDiscover;
		this.groupMulticastAddress = multicastAddress;
		this.groupMulticastPort = multicastPort;
		this.timeToLive = timeToLive;
		this.hostAddress = hostAddress;
    }

    /**
     * Start the heartbeat thread
     */
    public final void init() {
        serverThread = new MulticastServerThread();
        serverThread.start();
    }

    /**
     * Shutdown this heartbeat sender
     */
    public final synchronized void dispose() {
        stopped = true;
        notifyAll();
        serverThread.interrupt();
    }
    
    private byte[] createPeerDiscoverPacket(){
		Peer self = peerDiscover.getSelf();
		String groupId = self.getGroupId();
		String payload = self.getId();
		byte type = 0;
		byte[] groupBytes = groupId.getBytes(Charset.forName("UTF-8"));
		byte[] payloadBytes = payload.getBytes(Charset.forName("UTF-8"));
		int gLength = groupBytes.length;
		int pLength = payloadBytes.length;
		if(gLength > PeerDiscover.MAX_GROUP_ID_LENGTH){
			throw new DiscoveryException("group id too large: " + gLength + ", limit: " + PeerDiscover.MAX_GROUP_ID_LENGTH);
		}
		int packetLength = 1 + 1 + gLength + pLength;
		if(packetLength > PeerDiscover.MAX_FRAME_LENGTH){
			throw new DiscoveryException("discover packet too large: " + packetLength + ", limit: " + PeerDiscover.MAX_FRAME_LENGTH);
		}
		
		byte[] packet = new byte[packetLength + 2];
		int writeIndex = 0;
		packet[writeIndex++] = (byte) (packetLength & 0xFF);
		packet[writeIndex++] = (byte) (packetLength >> 8 & 0xFF);
		
		packet[writeIndex++] = type;
		packet[writeIndex++] = (byte) (gLength & 0xFF);
		for (int i = 0; i < gLength; i++, writeIndex++) {
			packet[writeIndex] = groupBytes[i];
		}
		for (int i = 0; i < pLength; i++, writeIndex++) {
			packet[writeIndex] = payloadBytes[i];
		}
		
		return packet;
    }
    
    /**
     * A thread which sends a multicast heartbeat every INTERVAL
     */
    private final class MulticastServerThread extends Thread {

        private MulticastSocket socket;

        /**
         * Constructor
         */
        public MulticastServerThread() {
            super("Multicast Heartbeat Sender");
            setDaemon(true);
        }

        @Override
        public final void run() {
            while (!stopped) {
                try {
                    socket = new MulticastSocket(groupMulticastPort.intValue());
                    if (hostAddress != null) {
                        socket.setInterface(hostAddress);
                    }
                    socket.setTimeToLive(timeToLive.intValue());
                    socket.joinGroup(groupMulticastAddress);

                    while (!stopped) {
                    	byte[] buffer = createPeerDiscoverPacket();
                    	DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupMulticastAddress,
                                groupMulticastPort.intValue());
                        socket.send(packet);
                        try {
                            synchronized (this) {
                                wait(heartBeatInterval);
                            }
                        } catch (InterruptedException e) {
                            if (!stopped) {
                                logger.error("Error sending heartbeat. Initial cause was " + e.getMessage(), e);
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.debug("Error on multicast socket", e);
                } catch (Throwable e) {
                    logger.info("Unexpected throwable in run thread. Continuing..." + e.getMessage(), e);
                } finally {
                    closeSocket();
                }
                if (!stopped) {
                    try {
                        sleep(heartBeatInterval);
                    } catch (InterruptedException e) {
                        logger.error("Sleep after error interrupted. Initial cause was " + e.getMessage(), e);
                    }
                }
            }
        }

        @Override
        public final void interrupt() {
            closeSocket();
            super.interrupt();
        }

        private void closeSocket() {
            try {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.leaveGroup(groupMulticastAddress);
                    } catch (IOException e) {
                        logger.error("Error leaving multicast group. Message was " + e.getMessage());
                    }
                    socket.close();
                }
            } catch (NoSuchMethodError e) {
                logger.debug("socket.isClosed is not supported by JDK1.3");
                try {
                    socket.leaveGroup(groupMulticastAddress);
                } catch (IOException ex) {
                    logger.error("Error leaving multicast group. Message was " + ex.getMessage());
                }
                socket.close();
            }
        }

    }

    /**
     * Sets the heartbeat interval to something other than the default of 5000ms. This is useful for testing,
     * but not recommended for production. This method is static and so affects the heartbeat interval of all
     * senders. The change takes effect after the next scheduled heartbeat.
     *
     * @param heartBeatInterval a time in ms, greater than 1000
     */
    public static void setHeartBeatInterval(long heartBeatInterval) {
        if (heartBeatInterval < MINIMUM_HEARTBEAT_INTERVAL) {
            logger.warn("Trying to set heartbeat interval too low. Using MINIMUM_HEARTBEAT_INTERVAL instead.");
            MulticastKeepaliveHeartbeatSender.heartBeatInterval = MINIMUM_HEARTBEAT_INTERVAL;
        } else {
            MulticastKeepaliveHeartbeatSender.heartBeatInterval = heartBeatInterval;
        }
    }

    /**
     * Sets the heartbeat stale time to something other than the default of {@code ((2 * HeartBeatInterval) + 100)ms}.
     * This is useful for testing, but not recommended for production. This method is static and so affects the stale
     * time all users.
     *
     * @param heartBeatStaleTime a time in ms
     */
    public static void setHeartBeatStaleTime(long heartBeatStaleTime) {
        MulticastKeepaliveHeartbeatSender.heartBeatStaleTime = heartBeatStaleTime;
    }

    /**
     * Returns the heartbeat interval.
     */
    public static long getHeartBeatInterval() {
        return heartBeatInterval;
    }

    /**
     * Returns the time after which a heartbeat is considered stale.
     */
    public static long getHeartBeatStaleTime() {
        if (heartBeatStaleTime < 0) {
            return (heartBeatInterval * 2) + ONE_HUNDRED_MS;
        } else {
            return heartBeatStaleTime;
        }
    }

    /**
     * @return the TTL
     */
    public Integer getTimeToLive() {
        return timeToLive;
    }
}
