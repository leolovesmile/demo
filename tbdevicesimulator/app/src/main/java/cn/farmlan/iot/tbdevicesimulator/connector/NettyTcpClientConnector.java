package cn.farmlan.iot.tbdevicesimulator.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;

public class NettyTcpClientConnector {


    private final Bootstrap bootstrap;
    private final Observable<String> serverMsg;
    private String host;
    private int port;
    private NioEventLoopGroup workerGroup;

    public NettyTcpClientConnector(String host, int port) {
        this.host = host;
        this.port = port;
        this.workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

        serverMsg = Observable.<String>create(source -> {
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch)
                        throws Exception {
                    ch.pipeline().addLast(new StringDecoder(),
                            new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                    source.onNext(msg);
                                }
                            },
                            new StringEncoder());
                }
            });
        }).doFinally(() -> workerGroup.shutdownGracefully());
    }

    public @NonNull Observable<String> connectAndSend(Observable<String> msgs) {
        return msgs.map(msg->{
            ChannelFuture f = bootstrap.connect(host, port).sync();
            f.channel().writeAndFlush(msg).sync();

//            f.channel().closeFuture().sync();
            return msg;
        });
    }

    public Observable<String> getServerMsg() {
        return serverMsg;
    }
}
