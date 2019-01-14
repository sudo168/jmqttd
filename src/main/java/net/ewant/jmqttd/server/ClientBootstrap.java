package net.ewant.jmqttd.server;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

public class ClientBootstrap {
	
	public void start(String host,int port, boolean bindLocal) throws Exception{
		EventLoopGroup workerGroup = new NioEventLoopGroup();  
		  
        try {  
            Bootstrap b = new Bootstrap();  
            b.group(workerGroup);  
            b.channel(NioSocketChannel.class);  
            b.option(ChannelOption.SO_KEEPALIVE, true);  
            b.option(ChannelOption.SO_REUSEADDR, true);  
            b.handler(new ChannelInitializer<SocketChannel>() {  
                @Override  
                public void initChannel(SocketChannel ch) throws Exception { 
                	ChannelPipeline pipeline = ch.pipeline();
                	pipeline.addLast("objectDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                    pipeline.addLast("lengthFieldPrepender",new LengthFieldPrepender(4));
                    pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                    pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));
                    pipeline.addLast(new ChannelInboundHandlerAdapter(){
                		@Override
                		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                			System.out.println("1收到" + ctx.channel().remoteAddress()+"=="+ctx.channel().localAddress() + "：" + msg);
                			String string = msg.toString();
                			if (string.startsWith("127.0.0.1")) {
                				String[] split = string.split(":");
                				if (!split[1].equals(ctx.channel().localAddress().toString().split(":")[1])) {
                					/*ByteBuf buf1 = Unpooled.buffer();
                					byte[] bytes = "打洞...".getBytes();
                					buf1.writeInt(bytes.length);
                					buf1.writeBytes(bytes);
                					ctx.writeAndFlush(new DatagramPacket(buf1, new InetSocketAddress(split[0], Integer.parseInt(split[1]))));
                					Thread.sleep(1000);
                					ByteBuf buf2 = Unpooled.buffer();
                					byte[] bytes2 = "P2P...".getBytes();
                					buf2.writeInt(bytes2.length);
                					buf2.writeBytes(bytes2);
                					ctx.writeAndFlush(new DatagramPacket(buf2, new InetSocketAddress(split[0], Integer.parseInt(split[1]))));*/
                					ctx.close();
                					Thread.sleep(2000);
                					ClientBootstrap client = new ClientBootstrap();
                			    	client.start(split[0], Integer.parseInt(split[1]),true);
                				}
							}
                		}
                		@Override
                		public void channelActive(ChannelHandlerContext ctx) throws Exception {
                			System.out.println("1发送："+ctx.channel().remoteAddress()+"=="+ctx.channel().localAddress());
                			ByteBuf buf = Unpooled.buffer();
        					byte[] bytes = "{msg:你好}".getBytes();
        					//buf.writeInt(bytes.length);
        					buf.writeBytes(bytes);
        					//new DatagramPacket(buf, new InetSocketAddress("127.0.0.1", 8083))
        					ctx.writeAndFlush(buf);
                		}
                	});
                }  
            });  
            InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 62273);
            // Start the client.  
            ChannelFuture f = b.connect(new InetSocketAddress(host, port),bindLocal ? localAddress : null).sync();
            //if(bindLocal) this.listen(localAddress.getHostName(),localAddress.getPort()+1);
            // Wait until the connection is closed.  
            f.channel().closeFuture().sync();  
        } finally {  
            workerGroup.shutdownGracefully();  
        }
	}
	
	public void listen(String host,int port) throws Exception{
		EventLoopGroup workerGroup = new NioEventLoopGroup();  
		  
        try {  
            io.netty.bootstrap.ServerBootstrap b = new io.netty.bootstrap.ServerBootstrap();  
            b.group(workerGroup,workerGroup);  
            b.channel(NioServerSocketChannel.class);  
            b.option(ChannelOption.SO_KEEPALIVE, true);  
            b.option(ChannelOption.SO_REUSEADDR, true);  
            b.childHandler(new ChannelInitializer<ServerSocketChannel>() {  
                @Override  
                public void initChannel(ServerSocketChannel ch) throws Exception { 
                	ChannelPipeline pipeline = ch.pipeline();
                	pipeline.addLast("objectDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                    pipeline.addLast("lengthFieldPrepender",new LengthFieldPrepender(4));
                    pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                    pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));
                    pipeline.addLast(new ChannelInboundHandlerAdapter(){
                		@Override
                		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                			System.out.println("listen收到" + ctx.channel().remoteAddress()+"=="+ctx.channel().localAddress() + "：" + msg);
                		}
                		@Override
                		public void channelActive(ChannelHandlerContext ctx) throws Exception {
                			System.out.println("listen发送："+ctx.channel().remoteAddress()+"=="+ctx.channel().localAddress());
                			ByteBuf buf = Unpooled.buffer();
        					byte[] bytes = "{msg:listen你好}".getBytes();
        					//buf.writeInt(bytes.length);
        					buf.writeBytes(bytes);
        					//new DatagramPacket(buf, new InetSocketAddress("127.0.0.1", 8083))
        					ctx.writeAndFlush(buf);
                		}
                	});
                }  
            });  
            // Start the client.  
            ChannelFuture f = b.bind(host, port).syncUninterruptibly();  
            // Wait until the connection is closed.  
            //f.channel().closeFuture().sync();  
        } finally {  
            workerGroup.shutdownGracefully();  
        }
	}

    public static void main(String[] args) throws Exception {
    	ClientBootstrap client = new ClientBootstrap();
    	client.start("127.0.0.1", 1885 , true);
    }

}
