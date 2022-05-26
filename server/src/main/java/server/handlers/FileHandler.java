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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHandler {
    private static final int MB_1 = 1_000_000;
    private static final String DIR = "ServerDirectory" + File.separator;
    private static final Map<ChannelHandlerContext, FileChannel> mapFileChannel = new HashMap<>();

    private String login;
    private ChannelHandlerContext channel;

    public void setChannel(ChannelHandlerContext channel) {
        this.channel = channel;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void createUserDirectory() {
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
            channel.writeAndFlush(new BasicResponse(ServiceMessages.ERROR_RFL));
            throw new RuntimeException(e);
        }
    }

    public void createFile(SendFileRequest fileRequest) {
        FileChannel fileChannel = null;
        try {
            if (mapFileChannel.containsKey(channel) || isSpaceAvailable(fileRequest.getFileInfo().getSize())) {
                Path path = Path.of(DIR, login, fileRequest.getServerPath());
                if (fileRequest.getFileInfo().getType().getName().equals("D")){
                    Files.createDirectories(path);
                    channel.writeAndFlush(new BasicResponse(ServiceMessages.F_SEND_OK));
                } else {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(fileRequest.getFile());
                    if (byteBuffer.hasRemaining()) {
                        fileChannel = getFileChannel(path);
                        fileChannel.write(byteBuffer);
                        byteBuffer.clear();
                    }
                    if (fileRequest.getFileInfo().getSize() == Files.size(path)) {
                        removeChannel(fileChannel);
                        channel.writeAndFlush(new BasicResponse(ServiceMessages.F_SEND_OK));
                    } else {
                        channel.writeAndFlush(new FilePartResponse(true));
                    }
                }
            } else {
                channel.writeAndFlush(new BasicResponse(ServiceMessages.EXCESS_Q));
            }
        } catch (IOException e) {
            channel.writeAndFlush(new FilePartResponse(false));
            throw new RuntimeException(e);
        }
    }

    private void removeChannel(FileChannel fileChannel) {
        try {
            if (fileChannel != null) {
                fileChannel.close();
            }
            mapFileChannel.remove(channel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSpaceAvailable(long size) {
        Path path = Path.of(DIR, login);
        try {
            return Server.getConf("quota") >= (getSizeDirectory(path) + size);
        } catch (IOException e) {
            return false;
        }
    }

    private long getSizeDirectory(Path path) throws IOException {
        long size;
        try (Stream<Path> walk = Files.walk(path)) {
            size = walk
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
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

    private FileChannel getFileChannel(Path path) throws FileNotFoundException {
        FileChannel fileChannel = mapFileChannel.get(channel);
        if (fileChannel == null) {
            RandomAccessFile fileForSend = new RandomAccessFile(path.toFile(), "rw");
            fileChannel = fileForSend.getChannel();
            mapFileChannel.put(channel, fileChannel);
        }
        return fileChannel;
    }

    public void sendFileToLocal(BasicFileRequest request, String typeR) {
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
                RandomAccessFile fileForSend = new RandomAccessFile(path.toFile(), "rw");
                ByteBuffer byteBuffer = ByteBuffer.allocate(MB_1);
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

    public void deleteFiles(String login, String serverPath, ChannelHandlerContext channel) {
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
        } catch (IOException e) {
            channel.writeAndFlush(new BasicResponse(ServiceMessages.ERROR_DF));
            throw new RuntimeException(e);
        }
    }

    private boolean calculateDepth(List<String> dir) {
        AtomicInteger srcDepth = new AtomicInteger();

        dir.stream()
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    int i = path.split("[\\\\/]").length;
                    if (srcDepth.get() < i){
                        srcDepth.set(i);
                    }
                });
        System.out.println(srcDepth.get());
        return Server.getConf("maxDepth") - (srcDepth.get() - 1) >= 0;
    }
}
