package net.ewant.jmqttd.handler;

import java.util.List;

import net.ewant.jmqttd.codec.MqttCodec;
import net.ewant.jmqttd.config.HostPortSslConfiguration;
import net.ewant.jmqttd.core.AbstractHandlerChainAdapter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class MqttWebSocketHandlerChainAdapter extends AbstractHandlerChainAdapter<HostPortSslConfiguration>{

	@Override
	public void addSocketHandlers(ChannelPipeline pipeline) {
		//HttpServerCodec: 针对http协议进行编解码
		pipeline.addLast("httpServerCodec", new HttpServerCodec());
		//ChunkedWriteHandler分块写处理，文件过大会将内存撑爆
		pipeline.addLast("chunkedWriteHandler", new ChunkedWriteHandler());
		//作用是将一个Http的消息组装成一个完整的HttpRequest或者HttpResponse
		pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(8192));
        
        //WebSocketServerProtocolHandler用于处理websocket将请求解析为WebSocketFrame，并默认处理Ping，Pong, 参数1为访问websocket时的uri，参数2为websocket 子协议
        pipeline.addLast("webSocketServerProtocolHandler", new WebSocketServerProtocolHandler("/mqtt", "mqttv3.1,mqtt"));
        
        pipeline.addLast("WebSocketFrameDecoder", new WebSocketFrameDecoder());

		pipeline.addLast("MqttCodec", new MqttCodec(this.hostPortSsl.getProtocol(), new ProtocolMessageWrapper() {
			@Override
			public Object wrapperMessage(ByteBuf mqttBuffer) {
				return new BinaryWebSocketFrame(mqttBuffer);
			}
		}));
	}
	
	public static class WebSocketFrameDecoder extends MessageToMessageDecoder<WebSocketFrame> {
		@Override
		protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception {
			if(msg instanceof BinaryWebSocketFrame){
				ByteBuf content = msg.content();
				out.add(content.retain());// 引用计数+1，因为WebSocketFrameDecoder的父类做释放操作
			}
		}

	}

}
