package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import service.ServiceMessages;
import service.serializedClasses.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private static final Map<String, FileChannel> mapFileChannel = new HashMap<>();
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
        } else if (responseText.startsWith(ServiceMessages.CONF_MAXDEPTH)) {
            controller.setMaxDepth(Integer.parseInt(responseText.split(":")[1]));
            return;
        } else if (responseText.equals(ServiceMessages.AUTH_NO)) {
            controller.textArea.appendText("Неверный логин / пароль.\n");
            return;
        } else if (responseText.equals(ServiceMessages.AUTH_ALR)) {
            controller.textArea.appendText("Под таким логином уже зашли в программу.\n");
            return;
        } else if (responseText.equals(ServiceMessages.EXCESS_Q)) {
            controller.getAlertQuota();
            return;
        } else if (responseText.equals(ServiceMessages.REG_OK) ||
                    responseText.equals(ServiceMessages.REG_NO)) {
            controller.getRegController().regStatus(responseText);
            return;
        } else if ("returnFileList".equals(responseText)) {
            GetFileListResponse getFileListResponse = (GetFileListResponse) msg;
            List<FileInfo> fileList = getFileListResponse.getFiles();
            controller.getServerPC().setNetwork(controller.getNetwork());
            controller.getServerPC().updateList(getFileListResponse.getSubDirection(), fileList);
            return;
        } else if ("uploadFile".equals(responseText)) {
            if (getServerFile((FileResponse ) response)){
                controller.getLocalPC().updateList(Paths.get(controller.getLocalPC().getCurrentPath()));
            }
            return;
        } else if ("moveFile".equals(responseText)) {
            if (getServerFile((FileResponse ) response)){
                getServerFile((FileResponse ) response);
                controller.getLocalPC().updateList(Paths.get(controller.getLocalPC().getCurrentPath()));
                ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), controller.getServerPC().getCurrentPath()));
            }
            return;
        } else if ("delFile".equals(responseText)) {
            ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), controller.getServerPC().getCurrentPath()));
            return;
        } else if ("sendFile".equals(responseText)) {
            ctx.writeAndFlush(new GetFileListRequest(controller.getLogin(), controller.getPassword(), controller.getServerPC().getCurrentPath()));
            return;
        }
    }

    private FileChannel getFileChannel(String path) throws FileNotFoundException {
        FileChannel fileChannel = mapFileChannel.get(path);
        if (fileChannel == null) {
            RandomAccessFile fileForSend = new RandomAccessFile(path, "rw");
            fileChannel = fileForSend.getChannel();
            mapFileChannel.put(path, fileChannel);
        }
        return fileChannel;
    }

    private boolean getServerFile(FileResponse response){
        try {
            Path path = Path.of(response.getLocalPath());
            if (response.getFileInfo().getType().getName().equals("D")){
                Files.createDirectories(path);
            } else {
                ByteBuffer byteBuffer = ByteBuffer.wrap(response.getFile());
                FileChannel fileChannel = null;
                if (byteBuffer.hasRemaining()){
                    fileChannel = getFileChannel(path.toString());
                    fileChannel.write(byteBuffer);
                    byteBuffer.clear();
                }

                if (response.getFileInfo().getSize() == Files.size(path)){
                    if (fileChannel != null) {
                        fileChannel.close();
                    }
                    mapFileChannel.remove(path.toString());
                    return true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return false;
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
