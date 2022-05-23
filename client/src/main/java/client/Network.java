package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import service.serializedClasses.BasicRequest;

public class Network {
    private final String ADDRESS = "localhost";
    private final int PORT = 45081;
    public static final int MB_20 = 20 * 1_000_000;

    private Channel channel;

    public Network(Controller controller) {
        new Thread(() -> {
            EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(eventLoopGroup);
                bootstrap.channel(NioSocketChannel.class);
                bootstrap.remoteAddress(ADDRESS, PORT);
                bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        socketChannel.pipeline().addLast(
                                new ObjectDecoder(MB_20, ClassResolvers.cacheDisabled(null)),
                                new ObjectEncoder(),
                                new ServerHandler(controller)
                        );
                    }
                });
                ChannelFuture channelFuture =  bootstrap.connect().sync();
                channel = channelFuture.channel();
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
              eventLoopGroup.shutdownGracefully();
             }
        }).start();

    }

    public void sendRequest(BasicRequest br) {
        channel.writeAndFlush(br);
    }

    public void close(){
        channel.close();
    }
}
