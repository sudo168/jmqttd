package net.ewant.jmqttd.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.ewant.jmqttd.codec.message.*;
import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.listener.MqttSessionListener;
import net.ewant.jmqttd.server.mqtt.MqttServerContext;
import net.ewant.jmqttd.server.mqtt.MqttSession;
import net.ewant.jmqttd.server.mqtt.MqttSessionManager;

import java.io.IOException;
import java.util.List;

public class MqttDecoder extends ByteToMessageDecoder {

	//Logger logger = LoggerFactory.getLogger(getClass());

	private static final int DEFAULT_MAX_BYTES_IN_PAYLOAD = 1024 * 1024;// 1M

	enum DecoderState {
		READ_FIXED_HEADER,
		READ_REMAINING_LENGTH,
		READ_PAYLOAD,
		READ_PAYLOAD_WAIT,
		BAD_MESSAGE,
	}

	private MqttSessionListener sessionListener;

	private int maxBytesInPayload;

	private MqttPacket currentDecodePacket;

	private DecoderState state;

	private DecoderState state(){
		return state;
	}

	private void state(DecoderState state){
		this.state = state;
	}
	
	public MqttDecoder(ServerProtocol protocol) {
		this(protocol, DEFAULT_MAX_BYTES_IN_PAYLOAD);
	}

	public MqttDecoder(ServerProtocol protocol, int maxBytesInPayload) {
		this.sessionListener = MqttServerContext.getServer(protocol).getSessionListener();
		this.maxBytesInPayload = maxBytesInPayload;
		this.state(DecoderState.READ_FIXED_HEADER);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		sessionListener.onSessionOpen(ctx.channel());
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		MqttSession client = MqttSessionManager.getSession(ctx.channel());
		sessionListener.onSessionClose(client, MqttSession.State.UNEXPECTED_HALT);
	}

	@Override
	protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
		//logger.warn("arrived: " + in.readableBytes() + " =======> " + this);
		try {
			DecoderState newState = null;
			while (in.isReadable() || newState != DecoderState.READ_FIXED_HEADER) {
				decode(ctx, in, out);
				newState = state();
				if(newState == DecoderState.READ_PAYLOAD_WAIT){
					break;
				}
			}
		} catch (DecoderException e) {
			throw e;
		} catch (Exception cause) {
			throw new DecoderException(cause);
		}
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
		switch (state()) {
			case READ_FIXED_HEADER: try {
				currentDecodePacket = new MqttPacket();
				MqttFixedHeader fixedHeader = MqttCodecUtils.isValidFixedHeader(new MqttFixedHeader(buffer.readByte()));
				MqttSession client = MqttSessionManager.getSession(ctx.channel());
				currentDecodePacket.setFixedHeader(fixedHeader);
				currentDecodePacket.setClient(client);
				state(DecoderState.READ_REMAINING_LENGTH);
				break;
			}catch (Exception e){
				state(DecoderState.BAD_MESSAGE);
				currentDecodePacket.setCause(e);
			}

			case READ_REMAINING_LENGTH: try {
				int remainingLength = parseRemainingLength(buffer);
				if(remainingLength > maxBytesInPayload){
					throw new MqttDecodeException("too large message: " + remainingLength + " bytes. limit: " + maxBytesInPayload + " bytes");
				}
				ByteBuf payload = ctx.alloc().buffer(remainingLength, remainingLength);
				currentDecodePacket.setRemainingData(payload);
				currentDecodePacket.setRemainingLength(remainingLength);
				state(DecoderState.READ_PAYLOAD);
				break;
			}catch (Exception e){
				state(DecoderState.BAD_MESSAGE);
				currentDecodePacket.setCause(e);
			}

			case READ_PAYLOAD:
			case READ_PAYLOAD_WAIT: try {
				ByteBuf data = currentDecodePacket.getRemainingData();
				if(data.writableBytes() > buffer.readableBytes()){// 拆包了
					readBytes(buffer, data, buffer.readableBytes());// read min
					state(DecoderState.READ_PAYLOAD_WAIT);
				}else if(data.writableBytes() < buffer.readableBytes()){// 粘包了
					readBytes(buffer, data, data.writableBytes());// read min
					decodeFinish(ctx);
				}else{
					readBytes(buffer, data, buffer.readableBytes());
					decodeFinish(ctx);
				}
				break;
			}catch (Exception e){
				state(DecoderState.BAD_MESSAGE);
				currentDecodePacket.setCause(e);
			}
			case BAD_MESSAGE:
				processBadMessage(ctx, buffer);
				break;

			default:
				// Shouldn't reach here.
				state(DecoderState.BAD_MESSAGE);
				currentDecodePacket.setCause(new MqttDecodeException("Unknown Error!"));
		}
	}

	private void readBytes(ByteBuf src, ByteBuf dst, int length){
		if(length < 1){
			return;
		}
		dst.writeBytes(src, length);
		/*for(int i = 0; i < length; i++){
			dst.writeByte(src.readByte());
		}*/
	}

	private void processBadMessage(ChannelHandlerContext ctx, ByteBuf buffer){
		try{
			state(DecoderState.READ_FIXED_HEADER);
			// Keep discarding until disconnection.
			buffer.skipBytes(actualReadableBytes());
			sessionListener.onSessionException(ctx.channel(), currentDecodePacket.getClient(), currentDecodePacket.getCause());
		} finally {
			currentDecodePacket.clear();
			currentDecodePacket = null;
		}
	}

	private void decodeFinish(ChannelHandlerContext ctx){
		MqttWireMessage message;
		MqttSession client = null;
		try {
			ByteBuf remainingData = this.currentDecodePacket.getRemainingData();
			byte[] data = new byte[remainingData.readableBytes()];
			remainingData.readBytes(data, 0, remainingData.readableBytes());
			message  = buildWireMessage(this.currentDecodePacket.getFixedHeader(), this.currentDecodePacket.getRemainingLength(), data);
			client = this.currentDecodePacket.getClient();
			state(DecoderState.READ_FIXED_HEADER);
			//logger.warn("message: " + message.getClass().getName() + " client: " + (client==null? ProtocolUtil.toSessionId(ctx.channel()):client.getId()) + " =======> " + this);
		} catch (MqttDecodeException e) {
			throw e;
		}catch (Exception e){
			throw new MqttDecodeException(e);
		}

		try {
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

	private int parseRemainingLength(ByteBuf in) throws IOException {
		int multiplier = 1;
		int length = 0;
		byte digit;
		do {
			digit = in.readByte();// 读取一个字节
			length += (digit & 0x7f) * multiplier;
			multiplier *= 128;
		} while ((digit & 0x80) != 0);
		if(length > MqttPacket.MAX_FRAME_LENGTH)throw new MqttDecodeException("too large message! Remaining Length ["+length+"], has been exceeded. MAX_FRAME_LENGTH ["+MqttPacket.MAX_FRAME_LENGTH+"]");
		return length;
	}

	private MqttWireMessage buildWireMessage(MqttFixedHeader fixedHeader, int remainingLength, byte[] remainingData){
		MqttMessageType type = fixedHeader.getMessageType();
		MqttWireMessage result;
		if (type == MqttMessageType.CONNECT) {
			result = new MqttConnect(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.CONNACK) {
			result = new MqttConnAck(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.PUBLISH) {
			result = new MqttPublish(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.PUBACK) {
			result = new MqttPubAck(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.PUBREC) {
			result = new MqttPubRec(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.PUBREL) {
			result = new MqttPubRel(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.PUBCOMP) {
			result = new MqttPubComp(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.SUBSCRIBE) {
			result = new MqttSubscribe(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.SUBACK) {
			result = new MqttSubAck(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.UNSUBSCRIBE) {
			result = new MqttUnsubscribe(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.UNSUBACK) {
			result = new MqttUnsubAck(fixedHeader, remainingLength, remainingData);
		}else if (type == MqttMessageType.PING) {
			result = new MqttPing();
		}else if (type == MqttMessageType.PONG) {
			result = new MqttPong();
		}else if (type == MqttMessageType.DISCONNECT) {
			result = new MqttDisconnect();
		}else {
			throw new MqttDecodeException("MQTT packet decode error ! not support type [" + type + "]");
		}
		return result;
	}
}
