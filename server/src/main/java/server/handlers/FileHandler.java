package server.handlers;

import io.netty.channel.ChannelHandlerContext;
import server.Server;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        try (Stream<Path> paths = Files.list(Path.of(DIR + request.getLogin() + File.separator + request.getSubDirection()))) {
            return paths
                    .map(FileInfo::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createFile(SendFileRequest fileRequest, ChannelHandlerContext channel){
        Path path = Path.of(DIR, fileRequest.getLogin());
        try {
            if (mapFileChannel.containsKey(channel) || Server.getConf("quota") >= (getSizeDirectory(path) + fileRequest.getFileInfo().getSize())){
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

    private long getSizeDirectory(Path path) throws IOException {
        long size;
        try (Stream<Path> walk = Files.walk(path)){
            size = walk
                    .filter(Files::isRegularFile)
                    .mapToLong(p ->{
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        }
        return size;
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
        try (Stream<Path> paths = Files.walk(srcPath)) {
            paths.forEach(path -> {
                Path dstPath = srcPath.getParent().relativize(path);
                splitFile(path, data -> channel.writeAndFlush(new FileResponse(typeR, data, Paths.get(request.getLocalPath()).resolve(dstPath).toString(), new FileInfo(path))));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void splitFile(Path path, Consumer<byte[]> filePartCons) {
        byte[] bytes = new byte[0];

        try {
            if (!path.toFile().isDirectory()) {
                RandomAccessFile fileForSend = null;

                fileForSend = new RandomAccessFile(path.toFile(), "rw");

                ByteBuffer byteBuffer = ByteBuffer.allocate(MB_5);
                FileChannel fileChannel = fileForSend.getChannel();
                while (fileChannel.read(byteBuffer) != -1) {
                    bytes = new byte[byteBuffer.flip().remaining()];
                    byteBuffer.get(bytes);
                    filePartCons.accept(bytes);
                    byteBuffer.clear();
                }
                fileForSend.close();
            } else {
                filePartCons.accept(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteFiles(String login, String serverPath) {
        Path path = Path.of(DIR, login, serverPath);
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    paths.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            } else {
                Files.delete(path);
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
