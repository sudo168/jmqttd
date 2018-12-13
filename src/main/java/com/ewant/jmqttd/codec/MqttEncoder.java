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

	private ProtocolMessageWrapper wrapper;

	private MqttSessionListener sessionListener;

	public MqttEncoder(ServerProtocol protocol, ProtocolMessageWrapper wrapper){
		this.wrapper = wrapper;
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
			out.add(wrapper.wrapperMessage(buffer));
			sessionListener.onSend(MqttSessionManager.getSession(ctx.channel()), msg);
		}catch (MqttEncodeException e){
			sessionListener.onSessionException(ctx.channel(), MqttSessionManager.getSession(ctx.channel()), e);
		}catch (Exception e){
			sessionListener.onSessionException(ctx.channel(), MqttSessionManager.getSession(ctx.channel()), e);
		}finally {
			//ReferenceCountUtil.release(buffer); 这里不能释放释放了就没东西写出去了。这里申请的内存，放到out去以后框架会帮我们释放
		}
	}

}
