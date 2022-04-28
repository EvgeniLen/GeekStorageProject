package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import service.ServiceMessages;
import service.serializedClasses.BasicResponse;
import service.serializedClasses.FileInfo;
import service.serializedClasses.GetFileListRequest;
import service.serializedClasses.GetFileListResponse;

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
        new Thread(() -> {
            if (responseText.startsWith(ServiceMessages.AUTH_OK)) {
                controller.setAuthenticated(true);

                ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), ""));
            } else if (responseText.startsWith(ServiceMessages.REG_OK)) {
                controller.getRegController().regStatus(responseText);
            } else if ("returnFileList".equals(responseText)) {
                GetFileListResponse getFileListResponse = (GetFileListResponse) msg;
                List<FileInfo> fileList = getFileListResponse.getFiles();
                controller.getServerPC().setController(controller);
                controller.getServerPC().updateList(getFileListResponse.getSubDirection(), fileList);
                //for (FileInfo fileInfo : fileList) {
               //    System.out.println(fileInfo.getFilename());
               // }
            }
        }).start();

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
