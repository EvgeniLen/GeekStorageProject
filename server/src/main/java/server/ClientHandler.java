package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.codec.digest.DigestUtils;
import service.ServiceMessages;
import service.serializedClasses.*;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private final Server server;
    private ChannelHandlerContext channel;
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
        channel = ctx;
        BasicRequest request = (BasicRequest) msg;

        if (request instanceof AuthRequest) {
            AuthRequest authRequest = (AuthRequest) request;
            login = authRequest.getLogin();
            logger.log(Level.FINE, String.format("Authentication request from %s", login));
            if (server.getAuthService().getAutentificationResult(login, DigestUtils.sha256Hex(authRequest.getPassword()))) {
                sendBasicMsg(ServiceMessages.AUTH_OK);
                FileHandler.createUserDirectory(login); // создание серверной директории пользователя, если нет
                server.addClient(login, authRequest.getPassword());
                //server.subscribe(this);
                logger.log(Level.INFO, "Client " + login + " authenticated");
            } else {
                sendBasicMsg(ServiceMessages.AUTH_NO);
            }
        } else if (request instanceof RegRequest){
            RegRequest regRequest = (RegRequest) request;
            logger.log(Level.FINE, String.format("Registration request from %s", regRequest.getLogin()));
            if (server.getAuthService().registration(regRequest.getLogin(), DigestUtils.sha256Hex(regRequest.getPassword()))) {
                FileHandler.createUserDirectory(regRequest.getLogin()); // создание серверной директории пользователя, если нет
                sendBasicMsg(ServiceMessages.REG_OK);
            } else {
                sendBasicMsg(ServiceMessages.REG_NO);
            }
        }else if (request instanceof GetFileListRequest){
            GetFileListRequest getFLRequest = (GetFileListRequest) request;
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("GetFileList request from %s", (getFLRequest.getLogin())));
                List<FileInfo> listFiles = FileHandler.getFilesAndDirectories(getFLRequest);
                GetFileListResponse getFileListResponse = new GetFileListResponse(getFLRequest.getSubDirection(), listFiles);
                channel.writeAndFlush(getFileListResponse);
            }
        } else if (request instanceof SendFileRequest){
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("SendFile request from %s", (request.getLogin())));
                FileHandler.createFile((SendFileRequest) request);
            }
        } else if (request instanceof UploadFileRequest){
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("UploadFile request from %s", (request.getLogin())));
                byte[] data = FileHandler.getUploadFile((UploadFileRequest)request);
                channel.writeAndFlush(new UploadFileResponse(data, ((UploadFileRequest) request).getServerPath(), ((UploadFileRequest) request).getLocalPath()));
            }
        } else if (request instanceof MoveFileRequest){
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("MoveFile request from %s", (request.getLogin())));
                byte[] data = FileHandler.getMoveFile((MoveFileRequest)request);
                channel.writeAndFlush(new MoveFileResponse(data, ((MoveFileRequest) request).getServerPath(), ((MoveFileRequest) request).getLocalPath()));
            }
        } else if (request instanceof DelFileRequest){
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("DelFile request from %s", (request.getLogin())));
                if (FileHandler.delFile((DelFileRequest) request)){
                    channel.writeAndFlush(new DelFileResponse(((DelFileRequest) request).getServerPath()));
                }
            }
        }

    }

    public String getLogin() {
        return login;
    }

    public void sendBasicMsg(String msg){
        BasicResponse basicResponse = new BasicResponse(msg);
        channel.writeAndFlush(basicResponse);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.INFO, "Client connected: " + ctx.channel().remoteAddress());
        //server.getChannels().add(ctx.channel());
    }

    @Override
    public void channelInactive(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.FINE, "Client disconnected: " + ctx.channel().remoteAddress());
        server.removeClient(login);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //logger.log(Level.FINE, "Error, Client disconnected: " + ctx.channel().remoteAddress());
        //ctx.close();
    }
}
