package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import service.ServiceMessages;
import service.serializedClasses.*;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private Server server;
    private ChannelHandlerContext channelHandlerContext;
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
        channelHandlerContext = ctx;
        BasicRequest request = (BasicRequest) msg;
        logger.log(Level.FINE, "Request Type = " + request.getType());

        if (request instanceof AuthRequest) {
            AuthRequest authRequest = (AuthRequest) request;
            login = authRequest.getLogin();
            logger.log(Level.FINE, String.format("Authentication request from %s", login));
            boolean result = server.getAuthService().getAutentificationResult(login, authRequest.getPassword());
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
            logger.log(Level.FINE, String.format("GetFileList request from %s", ((GetFileListRequest) request).getLogin()));
            List<FileInfo> listFiles = FileHandler.getFilesAndDirectories((GetFileListRequest) request);
            GetFileListResponse getFileListResponse = new GetFileListResponse(((GetFileListRequest) request).getSubDirection(), listFiles);
            channelHandlerContext.writeAndFlush(getFileListResponse);
        } else if (request instanceof SendFileRequest){
            //надо добавить проверку логин\пароля
            FileHandler.createFile((SendFileRequest) request);
        } else if (request instanceof UploadFileRequest){
            //надо добавить проверку логин\пароля
            byte[] data = FileHandler.getFile((UploadFileRequest)request);
            channelHandlerContext.writeAndFlush(new UploadFileResponse(data, ((UploadFileRequest) request).getServerPath(), ((UploadFileRequest) request).getLocalPath()));
        }
    }

    public String getLogin() {
        return login;
    }

    public void sendBasicMsg(String msg){
        BasicResponse basicResponse = new BasicResponse(msg);
        channelHandlerContext.writeAndFlush(basicResponse);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.INFO, "Client connected: " + ctx.channel().remoteAddress());
        server.getChannels().add(ctx.channel());
    }

    @Override
    public void channelInactive(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.FINE, "Client disconnected: " + ctx.channel().remoteAddress());
        server.getChannels().remove(ctx.channel());
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        server.getChannels().remove(ctx.channel());
        logger.log(Level.FINE, "Error, Client disconnected: " + ctx.channel().remoteAddress());
        ctx.close();
    }
}
