package server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import service.ServiceMessages;
import service.serializedClasses.*;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private static final List<Channel> channels = new ArrayList<>();

    private Server server;
    private ChannelHandlerContext ChannelHandlerContext;
    private String login;

    /*private static final Map<Class<? extends BasicRequest>, Consumer<ChannelHandlerContext>> REQUEST_HANDLERS = new HashMap<>();

    static {
        REQUEST_HANDLERS.put(AuthRequest.class, channelHandlerContext -> {

        });
    }*/
    public ClientHandler(Server server) {
        this.server = server;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ChannelHandlerContext = ctx;
        BasicRequest request = (BasicRequest) msg;
        logger.log(Level.FINE, "Request Type = " + request.getType());

        if (request instanceof AuthRequest) {
            AuthRequest authRequest = (AuthRequest) request;
            logger.log(Level.FINE, String.format("Authentication request from %s", authRequest.getLogin()));
            boolean result = server.getAuthService().getAutentificationResult(authRequest.getLogin(), authRequest.getPassword());
            login = authRequest.getLogin();
            if (result) {
                sendBasicMsg(ServiceMessages.AUTH_OK);
                FileHandler.createUserDirectory(login); // создание серверной директории пользователя, если нет
                server.subscribe(this);
                logger.log(Level.INFO, "Client " + login + " authenticated");
            } else {
                sendBasicMsg("Неверный логин / пароль");
            }
        } else if (request instanceof RegRequest){
            RegRequest regRequest = (RegRequest) request;
            logger.log(Level.FINE, String.format("Registration request from %s", regRequest.getLogin()));
            if (server.getAuthService().registration(regRequest.getLogin(), regRequest.getPassword())) {
                FileHandler.createUserDirectory(regRequest.getLogin()); // создание серверной директории пользователя, если нет
                sendBasicMsg(ServiceMessages.REG_OK);
            } else {
                sendBasicMsg(ServiceMessages.REG_NO);
            }
        }else if (request instanceof GetFileListRequest){
            //надо добавить проверку логин\пароля
            GetFileListRequest getFileListRequest = (GetFileListRequest) request;

            List<FileInfo> listFiles = FileHandler.getFilesAndDirectories(getFileListRequest.getLogin(), getFileListRequest.getSubDirection());
            GetFileListResponse getFileListResponse = new GetFileListResponse(getFileListRequest.getSubDirection(), listFiles);
            ChannelHandlerContext.writeAndFlush(getFileListResponse);
        } else if (request instanceof SendFileRequest){
            //надо добавить проверку логин\пароля
            SendFileRequest sendFileRequest = (SendFileRequest) request;
            System.out.println(sendFileRequest.getPath());

            FileHandler.createFile(sendFileRequest);



        }
    }

    public String getLogin() {
        return login;
    }

    public void sendBasicMsg(String msg){
        BasicResponse basicResponse = new BasicResponse(msg);
        ChannelHandlerContext.writeAndFlush(basicResponse);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.INFO, "Client connected: " + ctx.channel().remoteAddress());
        channels.add(ctx.channel());
    }

    @Override
    public void channelInactive(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.FINE, "Client disconnected: " + ctx.channel().remoteAddress());
        channels.remove(ctx.channel());
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        channels.remove(ctx.channel());
        logger.log(Level.FINE, "Error, Client disconnected: " + ctx.channel().remoteAddress());
        ctx.close();
    }
}
