package netty.tcp;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

public class MessageForward extends SimpleChannelInboundHandler<String> {
    private static final Map<String, String> IP_TOKEN_MAP = new HashMap();
    private static final String MSG_SUFFIX = "&^!";
    private static final String USER_KEY = "\"userkey\":";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (null == msg) {
            return;
        }
        msg = msg.trim();
        if (msg.endsWith(MSG_SUFFIX)) {
            msg = msg.substring(0, msg.length() - MSG_SUFFIX.length());
            if (msg.contains(USER_KEY)) {
                setUserKey(ctx, msg);
            } else if (msg.contains("\"data\"")) {
                forwardData(ctx, msg);
            }
        }

    }

    private static String getHostName(ChannelHandlerContext ctx) {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        return remoteAddress.getHostName();
    }

    private static void forwardData(ChannelHandlerContext ctx, String msg) {
        if (IP_TOKEN_MAP.containsKey(getHostName(ctx))) {
            String token = IP_TOKEN_MAP.get(getHostName(ctx));
            String url = String.format("http://localhost:8080/api/v1/%1s/telemetry", token);
            msg = msg.substring(msg.indexOf("["), msg.lastIndexOf("]") + 1);
            msg = msg.replaceAll("\"Name\":", "");
            String data = msg.replaceAll(",\"Value\":", ":");
            System.out.println("about to forward data: " + data +" to " + url);

            try {
                sendPostRequest(url, data);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Finished forwarding data!");
        } else {
            System.out.println("No token exists for the device " + getHostName(ctx) + ", no data will be forwarded!");
        }
    }

    private static void setUserKey(ChannelHandlerContext ctx, String msg) {
        int idx = msg.indexOf(USER_KEY) + USER_KEY.length();
        msg = msg.substring(idx).trim().substring(1);

        String token = msg.substring(0, msg.indexOf("\""));

        IP_TOKEN_MAP.put(getHostName(ctx), token);
        System.out.println(getHostName(ctx) + " with token " + token + " has been added to map!");
    }

    private static void sendPostRequest(String url, String content) throws URISyntaxException, InterruptedException {
        URI uri = new URI(url);
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();

        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).handler(new HttpSnoopClientInitializer());

            // Make the connection attempt.
            Channel ch = b.connect(host, port).sync().channel();

            // Prepare the HTTP request.
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
                    uri.getRawPath());
            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

            ByteBuf bbuf = Unpooled.copiedBuffer(content, StandardCharsets.UTF_8);
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, bbuf.readableBytes());
            request.content().clear().writeBytes(bbuf);

            // Send the HTTP request.
            ch.writeAndFlush(request);
            // Wait for the server to close the connection.
            ch.closeFuture().sync();
        } finally {
            // Shut down executor threads to exit.
            group.shutdownGracefully();
        }
    }

    public static class HttpSnoopClientInitializer extends ChannelInitializer<SocketChannel> {
        public HttpSnoopClientInitializer() {

        }

        @Override
        public void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();

            p.addLast(new HttpClientCodec());

            // Remove the following line if you don't want automatic content decompression.
            p.addLast(new HttpContentDecompressor());

            // Uncomment the following line if you don't want to handle HttpContents.
            // p.addLast(new HttpObjectAggregator(1048576));

            p.addLast(new HttpSnoopClientHandler());
        }
    }

    public static class HttpSnoopClientHandler extends SimpleChannelInboundHandler<HttpObject> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;

                System.err.println("STATUS: " + response.status());
                System.err.println("VERSION: " + response.protocolVersion());
                System.err.println();

                if (!response.headers().isEmpty()) {
                    for (CharSequence name : response.headers().names()) {
                        for (CharSequence value : response.headers().getAll(name)) {
                            System.err.println("HEADER: " + name + " = " + value);
                        }
                    }
                    System.err.println();
                }

                if (HttpUtil.isTransferEncodingChunked(response)) {
                    System.err.println("CHUNKED CONTENT {");
                } else {
                    System.err.println("CONTENT {");
                }
            }
            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;

                System.err.print(content.content().toString(CharsetUtil.UTF_8));
                System.err.flush();

                if (content instanceof LastHttpContent) {
                    System.err.println("} END OF CONTENT");
                    ctx.close();
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}