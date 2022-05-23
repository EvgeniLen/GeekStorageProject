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
    private final FileHandler fileHandler;
    private ChannelHandlerContext channel;
    private String login;
    

    /*private static final Map<Class<? extends BasicRequest>, Consumer<ChannelHandlerContext>> REQUEST_HANDLERS = new HashMap<>();

    static {
        REQUEST_HANDLERS.put(AuthRequest.class, channelHandlerContext -> {

        });
    }*/
    public ClientHandler(Server server) {
        this.server = server;
        fileHandler = new FileHandler();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        channel = ctx;
        BasicRequest request = (BasicRequest) msg;

        if (request instanceof AuthRequest authRequest) {
            login = authRequest.getLogin();
            logger.log(Level.FINE, String.format("Authentication request from %s", login));
            if (server.getAuthService().getAutentificationResult(login, DigestUtils.sha256Hex(authRequest.getPassword()))) {
                if (!server.isLoginAuthenticated(login)){
                    sendBasicMsg(ServiceMessages.AUTH_OK);
                    sendBasicMsg(String.format("%s:%d", ServiceMessages.CONF_MAXDEPTH, Server.getConf("maxDepth")));
                    fileHandler.createUserDirectory(login); // создание серверной директории пользователя, если нет
                    server.addClient(login, authRequest.getPassword());
                    logger.log(Level.INFO, "Client " + login + " authenticated");
                }else {
                    sendBasicMsg(ServiceMessages.AUTH_ALR);
                }
            } else {
                sendBasicMsg(ServiceMessages.AUTH_NO);
            }
        }else if (request instanceof LogOutRequest){
            server.removeClient(login);
            logger.log(Level.INFO, String.format("Client %s - disconnected", login));
            login = null;
        }else if (request instanceof RegRequest regRequest){
            logger.log(Level.FINE, String.format("Registration request from %s", regRequest.getLogin()));
            if (server.getAuthService().registration(regRequest.getLogin(), DigestUtils.sha256Hex(regRequest.getPassword()))) {
                fileHandler.createUserDirectory(regRequest.getLogin()); // создание серверной директории пользователя, если нет
                sendBasicMsg(ServiceMessages.REG_OK);
            } else {
                sendBasicMsg(ServiceMessages.REG_NO);
            }
        }else if (request instanceof GetFileListRequest){
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("GetFileList request from %s", (request.getLogin())));
                List<FileInfo> listFiles = fileHandler.getFilesAndDirectories((GetFileListRequest) request);
                GetFileListResponse getFileListResponse = new GetFileListResponse(((GetFileListRequest) request).getSubDirection(), listFiles);
                channel.writeAndFlush(getFileListResponse);
            }
        } else if (request instanceof SendFileRequest){
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("SendFile request from %s", (request.getLogin())));
                fileHandler.createFile((SendFileRequest) request, channel);
            }
        } else if (request instanceof UploadFileRequest){
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("UploadFile request from %s", (request.getLogin())));
                fileHandler.sendFileToLocal((UploadFileRequest)request, channel, "uploadFile");
            }
        } else if (request instanceof MoveFileRequest){
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("MoveFile request from %s", (request.getLogin())));
                fileHandler.sendFileToLocal((MoveFileRequest)request, channel, "moveFile");
                fileHandler.deleteFiles(request.getLogin(), ((MoveFileRequest) request).getServerPath());
            }
        } else if (request instanceof DelFileRequest){
            if (server.checkAuthorization(request)){
                logger.log(Level.FINE, String.format("DelFile request from %s", (request.getLogin())));
                if (fileHandler.deleteFiles(request.getLogin(), ((DelFileRequest) request).getServerPath())){
                    sendBasicMsg("delFile");
                }
            }
        }

    }

    public void sendBasicMsg(String msg){
        BasicResponse basicResponse = new BasicResponse(msg);
        channel.writeAndFlush(basicResponse);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.INFO, "Client connected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.FINE, "Client disconnected: " + ctx.channel().remoteAddress());
        server.removeClient(login);
        login = null;
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //logger.log(Level.FINE, "Error, Client disconnected: " + ctx.channel().remoteAddress());
        //ctx.close();
    }
}
