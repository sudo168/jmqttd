package net.ewant.jmqttd.codec;

import java.util.List;

import net.ewant.jmqttd.codec.message.MqttWireMessage;
import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.listener.MqttSessionListener;
import net.ewant.jmqttd.server.mqtt.MqttServerContext;
import net.ewant.jmqttd.server.mqtt.MqttSessionManager;
import net.ewant.jmqttd.server.mqtt.MqttSession;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MqttDecoder extends ByteToMessageDecoder {

	private MqttSessionListener sessionListener;
	
	public MqttDecoder(ServerProtocol protocol) {
		sessionListener = MqttServerContext.getServer(protocol).getSessionListener();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		sessionListener.onSessionOpen(ctx.channel());
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		sessionListener.onSessionClose(ctx.channel());
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		MqttSession client = null;
		try{
			MqttWireMessage message = MqttWireMessage.decode(in);

			client = MqttSessionManager.getSession(ctx.channel());

			sessionListener.onMessage(ctx.channel(), client, message);

		} catch (MqttDecodeException e) {
			sessionListener.onSessionException(ctx.channel(), client, e);
		} catch (Exception e) {
			sessionListener.onSessionException(ctx.channel(), client, e);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		sessionListener.onSessionException(ctx.channel(), MqttSessionManager.getSession(ctx.channel()), cause);
	}
}
