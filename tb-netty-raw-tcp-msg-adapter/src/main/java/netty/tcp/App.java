package netty.tcp;

import java.net.SocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * Hello world!
 */
public final class App {
    public static class Utils {
        public static final void log(String content) {
            System.out.println(content);
        }

        public static void log(SocketAddress remoteAddress, String string) {
            log(remoteAddress.toString() + ": " + string);
        }
    }

    private App() {
    }

    /**
     * Says hello to the world.
     * 
     * @param args The arguments of the program.
     * @throws InterruptedException
     */
    public static void main(String[] args) {
        try {
            new SimpleNettyServerBootstrap().start(6060);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    static class SimpleNettyServerBootstrap {

        void start(int port) throws InterruptedException {
            Utils.log("Starting server at: " + port);
            EventLoopGroup bossGroup = new NioEventLoopGroup();	// Event loop group for accepting and establishing client connections
            EventLoopGroup workerGroup = new NioEventLoopGroup(); // Event loop group for handling established connections
            try {
                ServerBootstrap b = new ServerBootstrap();	// Create a server bootstrap
                b.group(bossGroup, workerGroup)	// Setting group in server bootstrap
                        .channel(NioServerSocketChannel.class) // Configure Socket channel, since we are writing raw TCP server we will make TCP connection
                        .childHandler(new SimpleTCPChannelInitializer()) // Setting our custom channel initializer
                        .childOption(ChannelOption.SO_KEEPALIVE, true); // We need our connection to establish with Keep_Alive flag.
    
                // Bind and start to accept incoming connections.
                ChannelFuture f = b.bind(port).sync();	// Connect to given port
                if(f.isSuccess()) Utils.log("Server started successfully"); // Check if connection is successfully made
                f.channel().closeFuture().sync(); // Waiting for connection to close, this call is blocking
            } finally {
                Utils.log("Stopping server");
                workerGroup.shutdownGracefully(); // When server is shutting down, shutdown worker event loop group
                bossGroup.shutdownGracefully(); // When server is shutting down, shutdown boss event loop group
            }
        }
    }

    static public class SimpleTCPChannelInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast(new StringDecoder());// 
            socketChannel.pipeline().addLast(new MessageForward()); 
            socketChannel.pipeline().addLast(new SimpleTCPChannelHandler()); 
            socketChannel.pipeline().addLast(new StringEncoder()); // setting encoders/decoders.
        }
    }

    static public class SimpleTCPChannelHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            Utils.log(ctx.channel().remoteAddress(), "Channel Active");
        }
    
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
            Utils.log(ctx.channel().remoteAddress(), s.endsWith("\n") ? s.substring(0, s.length()-1): s);

            ctx.channel().writeAndFlush("Thanks! We got your message from " + ctx.channel().remoteAddress() + "\n");
        }
    
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            Utils.log(ctx.channel().remoteAddress(), "Channel Inactive");
        }
    }
}
