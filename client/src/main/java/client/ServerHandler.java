package client;

import client.filePanels.FileActions;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import service.ServiceMessages;
import service.serializedClasses.*;

import java.nio.file.Paths;
import java.util.List;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    private final Controller controller;

    public ServerHandler(Controller controller) {
        this.controller = controller;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){
        BasicResponse response = (BasicResponse) msg;
        String responseText = response.getResponse();
        if (responseText.equals(ServiceMessages.AUTH_OK)) {
            controller.setAuthenticated(true);
            ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), ""));
            return;
        } else if (responseText.startsWith(ServiceMessages.CONF_MAX_DEPTH)) {
            controller.setMaxDepth(Integer.parseInt(responseText.split(":")[1]));
            return;
        } else if (responseText.equals(ServiceMessages.AUTH_NO)) {
            controller.textArea.appendText("Неверный логин / пароль.\n");
            return;
        } else if (responseText.equals(ServiceMessages.AUTH_ALR)) {
            controller.textArea.appendText("Под таким логином уже зашли в программу.\n");
            return;
        } else if (responseText.equals(ServiceMessages.EXCESS_Q)) {
            controller.getAlertError("На серверном хранилищие превышена квота, отправка файлов невозможна. Удалите ненужные файлы.");
            return;
        } else if (responseText.equals(ServiceMessages.ERROR)) {
            controller.getAlertError("На сервере произошла ошибка.");
            return;
        } else if (responseText.equals(ServiceMessages.REG_OK) ||
                    responseText.equals(ServiceMessages.REG_NO)) {
            controller.getRegController().regStatus(responseText);
            return;
        } else if (responseText.equals(ServiceMessages.RETURN_FILE_L)) {
            GetFileListResponse getFileListResponse = (GetFileListResponse) msg;
            List<FileInfo> fileList = getFileListResponse.getFiles();
            controller.getServerPC().setNetwork(controller.getNetwork());
            controller.getServerPC().updateList(getFileListResponse.getSubDirection(), fileList);
            return;
        } else if (responseText.equals(ServiceMessages.UPLOAD_FILE)) {
            if (FileActions.getServerFile((FileResponse ) response)){
                controller.getLocalPC().updateList(Paths.get(controller.getLocalPC().getCurrentPath()));
            }
            return;
        } else if (responseText.equals(ServiceMessages.MOVE_FILE)) {
            if (FileActions.getServerFile((FileResponse ) response)){
                controller.getLocalPC().updateList(Paths.get(controller.getLocalPC().getCurrentPath()));
                ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), controller.getServerPC().getCurrentPath()));
            }
            return;
        } else if (responseText.equals(ServiceMessages.DEL_FILE)) {
            ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), controller.getServerPC().getCurrentPath()));
            return;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
