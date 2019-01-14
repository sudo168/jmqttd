package net.ewant.jmqttd.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.ewant.jmqttd.config.HostPortSslConfiguration;
import net.ewant.jmqttd.core.AbstractHandlerChainAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2018/11/22.
 */
public class MqttHttpHandlerChainAdapter extends AbstractHandlerChainAdapter<HostPortSslConfiguration> {
    @Override
    public void addSocketHandlers(ChannelPipeline pipeline) {
        //HttpServerCodec: 针对http协议进行编解码
        pipeline.addLast("httpServerCodec", new HttpServerCodec());
        //ChunkedWriteHandler分块写处理，文件过大会将内存撑爆
        pipeline.addLast("chunkedWriteHandler", new ChunkedWriteHandler());
        //作用是将一个Http的消息组装成一个完整的HttpRequest或者HttpResponse
        pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(1024 * 1024 * 50));// 50M
        //响应数据压缩
        //pipeline.addLast("compressor", new HttpContentCompressor());

        pipeline.addLast("httpServer", new HttpServerHandler());
    }

    public static class HttpServerHandler extends ChannelInboundHandlerAdapter{

        public static final AttributeKey<String> HTTP_SESSION_KEY = AttributeKey.valueOf("_HttpSessionId_");

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(msg instanceof FullHttpRequest){
                FullHttpRequest request = (FullHttpRequest) msg;
                request.headers().get("sessionId");
                request.headers().get("clientId");
                // ctx.channel().attr(HTTP_SESSION_KEY).set(request.headers().get("clientId"));
                Map<String, Object> parameters = HttpRequestParameterParser.parse(request);
                Object file = parameters.remove("file");
                ByteBuf content = Unpooled.copiedBuffer(JSON.toJSONString(parameters), CharsetUtil.UTF_8);
                FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, content);
                if (content != null) {
                    //response.headers().set("Content-Type", "text/plain;charset=UTF-8");
                    response.headers().set("Content-Type", "application/json;charset=UTF-8");
                    response.headers().set("Content-Length", response.content().readableBytes());

                    allowCros(request, response);
                }
                ctx.writeAndFlush(response);
            }
        }

        private void allowCros(FullHttpRequest request, FullHttpResponse response){
            String origin = request.headers().get("Origin");
            if(origin != null){
                response.headers().set("Access-Control-Allow-Origin", origin);
                response.headers().set("Access-Control-Allow-Credentials", "true");
                response.headers().set("Access-Control-Allow-Methods", "POST,GET,OPTIONS,DELETE");
                response.headers().set("Access-Control-Allow-Headers", "Authorization");
            }
        }
    }

    private static class HttpRequestParameterParser{

        static Logger logger = LoggerFactory.getLogger(HttpRequestParameterParser.class);

        public static Map<String, Object> parse(FullHttpRequest request){
            HttpMethod method = request.method();
            Map<String, Object> parameters = new HashMap<>();
            if(HttpMethod.GET == method){
                QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
                Map<String, List<String>> parameterMap = decoder.parameters();
                if(parameterMap != null){
                    for(String key : parameterMap.keySet()){
                        parameters.put(key, parameterMap.get(key).get(0));
                    }
                }
            }else if(HttpMethod.POST == method){
                HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
                String contentType = request.headers().get("Content-Type").trim();
                if(contentType.contains("x-www-form-urlencoded")){
                    List<InterfaceHttpData> parameterList = decoder.getBodyHttpDatas();
                    for (InterfaceHttpData param : parameterList) {
                        Attribute data = (Attribute) param;
                        try {
                            parameters.put(data.getName(), data.getValue());
                        } catch (IOException e) {
                            logger.error("parameter["+data.getName()+"] parse error: " + e.getMessage(), e);
                        }
                    }
                }else if(contentType.contains("application/json")){
                    try {
                        ByteBuf content = request.content();
                        byte[] reqContent = new byte[content.readableBytes()];
                        content.readBytes(reqContent);
                        String strContent = new String(reqContent, "UTF-8");

                        JSONObject jsonParams = JSONObject.parseObject(strContent);
                        for (Object key : jsonParams.keySet()) {
                            parameters.put(key.toString(), String.valueOf(jsonParams.get(key)));
                        }
                    } catch (UnsupportedEncodingException e) {
                        logger.error("json parameter parse error: " + e.getMessage(), e);
                    }
                }else if(contentType.contains("multipart/form-data")){
                    List<InterfaceHttpData> uploadInfoList = decoder.getBodyHttpDatas();
                    for(InterfaceHttpData httpData : uploadInfoList){
                        if(httpData instanceof Attribute){
                            Attribute attr = (Attribute) httpData;
                            try {
                                parameters.put(attr.getName(), attr.getValue());
                            } catch (IOException e) {
                                logger.error("parameter["+attr.getName()+"] parse error: " + e.getMessage(), e);
                            }
                        }else if(httpData instanceof FileUpload){
                            FileUpload file = (FileUpload) httpData;
                            parameters.put(file.getName(), file);
                        }
                    }
                }
            }
            return parameters;
        }
    }
}
