package server;

import io.netty.channel.ChannelHandlerContext;
import service.ServiceMessages;
import service.serializedClasses.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileHandler {
    private static final int MB_5 = 5 * 1_000_000;
    private static final String DIR = "ServerDirectory" + File.separator;
    private static final Map<ChannelHandlerContext, FileChannel> mapFileChannel = new HashMap<>();


    public void createUserDirectory(String login) {
        File directory = new File(DIR + login);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public List<FileInfo> getFilesAndDirectories(GetFileListRequest request) {
        try {
            return Files.list(Path.of(DIR + request.getLogin() + File.separator + request.getSubDirection()))
                    .map(FileInfo::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createFile(SendFileRequest fileRequest, ChannelHandlerContext channel){
        Path path = Path.of(DIR, fileRequest.getLogin());
        try {
            if (Server.getConf("quota") >= (Files.size(path) + fileRequest.getFileInfo().getSize())){
                path = Path.of(path.toString(), fileRequest.getServerPath());
                if (fileRequest.getFileInfo().getType().getName().equals("D")){
                    Files.createDirectories(path);
                } else {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(fileRequest.getFile());
                    FileChannel fileChannel = null;
                    if (byteBuffer.hasRemaining()){
                        fileChannel = getFileChannel(channel, path);
                        fileChannel.write(byteBuffer);
                        byteBuffer.clear();
                    }

                    if (fileRequest.getFileInfo().getSize() == Files.size(path)){
                        if (fileChannel != null) {
                            fileChannel.close();
                        }
                        mapFileChannel.remove(channel);
                    }
                }
            } else {
               channel.writeAndFlush(new BasicResponse(ServiceMessages.EXCESS_Q));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileChannel getFileChannel(ChannelHandlerContext channel, Path path) throws FileNotFoundException {
        FileChannel fileChannel = mapFileChannel.get(channel);
        if (fileChannel == null) {
            RandomAccessFile fileForSend = new RandomAccessFile(path.toFile(), "rw");
            fileChannel = fileForSend.getChannel();
            mapFileChannel.put(channel, fileChannel);
        }
        return fileChannel;
    }

    public void sendFileToLocal(BasicFileRequest request, ChannelHandlerContext channel, String typeR) {
        Path srcPath = Path.of(DIR, request.getLogin(), request.getServerPath());
        try {
            Files.walk(srcPath, (int) Server.getConf("maxDepth"))
                    .forEach(path -> {
                        byte[] data = new byte[0];
                        Path dstPath = srcPath.getParent().relativize(path);
                        if (!path.toFile().isDirectory()) {
                            try {
                                RandomAccessFile fileForSend = new RandomAccessFile(path.toFile(), "rw");
                                ByteBuffer byteBuffer = ByteBuffer.allocate(MB_5);
                                FileChannel fileChannel = fileForSend.getChannel();

                                while (fileChannel.read(byteBuffer) != -1) {
                                    byteBuffer.flip();
                                    data = new byte[byteBuffer.remaining()];
                                    byteBuffer.get(data);
                                    channel.writeAndFlush(new FileResponse(typeR, data, Paths.get(request.getLocalPath()).resolve(dstPath).toString(), new FileInfo(path)));
                                    byteBuffer.clear();
                                }
                                fileForSend.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            channel.writeAndFlush(new FileResponse(typeR, data, Paths.get(request.getLocalPath()).resolve(dstPath).toString(), new FileInfo(path)));
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteFiles(String login, String serverPath){
        Path path = Path.of(DIR, login, serverPath);
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } else {
                Files.delete(path);
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
