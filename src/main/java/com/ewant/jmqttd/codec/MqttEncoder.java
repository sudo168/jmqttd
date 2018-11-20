package com.ewant.jmqttd.codec;

import java.util.List;

import com.ewant.jmqttd.codec.message.MqttWireMessage;
import com.ewant.jmqttd.core.ServerProtocol;
import com.ewant.jmqttd.handler.ProtocolMessageWrapper;
import com.ewant.jmqttd.listener.MqttSessionListener;
import com.ewant.jmqttd.server.mqtt.MqttServerContext;
import com.ewant.jmqttd.server.mqtt.MqttSessionManager;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

public class MqttEncoder extends MessageToMessageEncoder<MqttWireMessage> {

	private ProtocolMessageWrapper wraper;

	private MqttSessionListener sessionListener;

	public MqttEncoder(ServerProtocol protocol, ProtocolMessageWrapper wraper){
		this.wraper = wraper;
		sessionListener = MqttServerContext.getServer(protocol).getSessionListener();
	}
	
	@Override
	public boolean acceptOutboundMessage(Object msg) throws Exception {
		return msg instanceof MqttWireMessage;
	}
	
	@Override
	protected void encode(ChannelHandlerContext ctx, MqttWireMessage msg, List<Object> out) throws Exception {
		ByteBuf buffer = null;
		try {
			byte[] encode = msg.encode();
			buffer = ctx.alloc().buffer(encode.length);
			buffer.writeBytes(encode);
			out.add(wraper.wraperMessage(buffer));
			sessionListener.onSend(MqttSessionManager.getSession(ctx.channel()), msg);
		}catch (MqttEncodeException e){
			sessionListener.onSessionException(ctx.channel(), MqttSessionManager.getSession(ctx.channel()), e);
		}catch (Exception e){
			sessionListener.onSessionException(ctx.channel(), MqttSessionManager.getSession(ctx.channel()), e);
		}finally {
			if(buffer != null){
				//ReferenceCountUtil.release(buffer); 这里不能释放释放了就没东西写出去了。这里申请的内存，会在websocket编码时释放
			}
		}
	}

}
