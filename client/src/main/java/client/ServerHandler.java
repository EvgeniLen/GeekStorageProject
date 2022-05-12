package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import service.ServiceMessages;
import service.serializedClasses.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    private Controller controller;


    public ServerHandler(Controller controller) {
        this.controller = controller;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        BasicResponse response = (BasicResponse) msg;
        System.out.println(response.getResponse());
        String responseText = response.getResponse();
        if (responseText.startsWith(ServiceMessages.AUTH_OK)) {
            controller.setAuthenticated(true);

            ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), ""));
            return;
        } else if (responseText.startsWith(ServiceMessages.REG_OK)) {
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
                Path path = Path.of(uploadFileResponse.getLocalPath());
                FileOutputStream fos = new FileOutputStream(path.toString());
                fos.write(uploadFileResponse.getFile());
                fos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            controller.getLocalPC().updateList(Path.of(uploadFileResponse.getLocalPath()).getParent());
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
