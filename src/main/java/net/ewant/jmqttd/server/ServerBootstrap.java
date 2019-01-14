package net.ewant.jmqttd.server;

import net.ewant.jmqttd.server.mqtt.MqttServer;

public class ServerBootstrap {

    public static void main(String[] args) throws Exception {

    	//TODO command line config must apply & plugin reload (use [-p reload] to reload plugins)

    	MqttServer.startup(args);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
				MqttServer.shutdown();
			}
		}));

    }

}
