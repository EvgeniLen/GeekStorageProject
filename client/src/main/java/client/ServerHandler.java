package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import service.ServiceMessages;
import service.serializedClasses.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    private Controller controller;

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
        } else if (responseText.equals(ServiceMessages.AUTH_NO)) {
            controller.textArea.appendText("Неверный логин / пароль\n");
            return;
        } else if (responseText.startsWith(ServiceMessages.REG_OK) ||
                    responseText.startsWith(ServiceMessages.REG_NO)) {
            controller.getRegController().regStatus(responseText);
            return;
        } else if ("returnFileList".equals(responseText)) {
            GetFileListResponse getFileListResponse = (GetFileListResponse) msg;
            List<FileInfo> fileList = getFileListResponse.getFiles();
            controller.getServerPC().setNetwork(controller.getNetwork());
            controller.getServerPC().updateList(getFileListResponse.getSubDirection(), fileList);
            return;
        } else if ("uploadFile".equals(responseText)) {
            UploadFileResponse uploadFileResponse = (UploadFileResponse) response;
            try {
                System.out.println(uploadFileResponse.getLocalPath());
                Path path = Path.of(uploadFileResponse.getLocalPath());
                if (uploadFileResponse.getFileInfo().getType().getName().equals("D")){
                    Files.createDirectories(path);
                } else {
                    FileOutputStream fos = new FileOutputStream(path.toString());
                    fos.write(uploadFileResponse.getFile());
                    fos.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            controller.getLocalPC().updateList(Paths.get(controller.getLocalPC().getCurrentPath()));
            return;
        } else if ("moveFile".equals(responseText)) {
            MoveFileResponse moveFileResponse  = (MoveFileResponse ) response;

            try {
                System.out.println(moveFileResponse.getLocalPath());
                Path path = Path.of(moveFileResponse.getLocalPath());
                if (moveFileResponse.getFileInfo().getType().getName().equals("D")){
                    Files.createDirectories(path);
                } else {
                    FileOutputStream fos = new FileOutputStream(path.toString());
                    fos.write(moveFileResponse.getFile());
                    fos.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            controller.getLocalPC().updateList(Paths.get(controller.getLocalPC().getCurrentPath()));
            ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), controller.getServerPC().getCurrentPath()));
            return;
        } else if ("delFile".equals(responseText)) {
            ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), controller.getServerPC().getCurrentPath()));
            return;
        } else if ("sendFile".equals(responseText)) {
            ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), controller.getServerPC().getCurrentPath()));
            return;
        }
    }

    private String getPathWithoutFile(String path){
        Path p = Paths.get(path).getParent();
        if (p != null) {
            return p.toString();
        } else {
            return "";
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
