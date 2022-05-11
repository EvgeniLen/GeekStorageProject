package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.commons.codec.digest.DigestUtils;
import service.serializedClasses.AuthRequest;
import service.serializedClasses.BasicRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final int PORT = 45081;
    private static final int MB_20 = 20 * 1_000_000;

    static {
        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("server/logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Map<String, String> clients;
    private final AuthService authService;

    public Map<String, String> getClients() {
        return clients;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public Server() {
        if (!SQLHandler.connect()) {
            throw new RuntimeException("Не удалось подключиться к БД");
        }
        authService = new DbAuthService();
        clients = new ConcurrentHashMap<>();

        //netty
        EventLoopGroup boosGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Server server = this;
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boosGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {

                            socketChannel.pipeline()
                                    .addLast(
                                            new ObjectDecoder(MB_20, ClassResolvers.cacheDisabled(null)),
                                            new ObjectEncoder(),
                                            new ClientHandler(server)
                                    );
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(PORT).sync();
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            SQLHandler.disconnect();
            logger.log(Level.SEVERE, "Server stop");

            workerGroup.shutdownGracefully();
            boosGroup.shutdownGracefully();
        }
    }

    public void addClient(String login, String password){
        clients.put(login, DigestUtils.sha256Hex(password));
    }

    public boolean checkAuthorization(BasicRequest request){
        return clients.get(request.getLogin()).equals(DigestUtils.sha256Hex(request.getPassword()));
    }

    public void removeClient(String login){
        if (clients.size() > 0 && clients.containsKey(login)){
            clients.remove(login);
        }
    }

    /*public boolean isLoginAuthenticated(String login){
        for (ClientHandler client : clients) {
            if (client.getLogin().equals(login)){
                return true;
            }
        }
        return false;
    }*/


}
