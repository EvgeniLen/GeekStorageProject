package client.filePanels;

import client.Controller;
import client.Network;
import service.serializedClasses.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientFileHandler {
    private static final int MB_1 = 1_000_000;
    private static final Map<String, FileChannel> mapFileChannel = new HashMap<>();
    private static ClientFileHandler handler;
    private ServerFilePanelController serverPC;
    private int maxDepth;
    private Network network;
    private Controller controller;
    private RandomAccessFile fileForSend;
    private int part;
    private Path srcPath;

    public Path getSrcPath() {
        return srcPath;
    }

    public static ClientFileHandler getHandler(){
        if (handler == null){
            handler = new ClientFileHandler();
        }
        return handler;
    }

    public void setFilePanel(ServerFilePanelController serverPC) {
        this.serverPC = serverPC;
    }
    public void setController(Controller controller) {
        this.controller = controller;
    }


    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void sendFiles(Path srcPath, String login, String password) {
        try (Stream<Path> paths = Files.walk(srcPath)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        part = 0;
                        Path dstPath = Paths.get(serverPC.getCurrentPath()).resolve(srcPath.getParent().relativize(path));
                        splitFile(path, (data) -> {
                            SendFileRequest sendFileRequest = new SendFileRequest(login, password, data, dstPath.toString(), new FileInfo(path));
                            try {
                                network.getChannel().writeAndFlush(sendFileRequest).sync();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    });
            GetFileListRequest fileListRequest = new GetFileListRequest(login, password, serverPC.getCurrentPath());
            network.sendRequest(fileListRequest);
        } catch (IOException e) {
            controller.getAlertError("Не удалось скопировать файл.");
            throw new RuntimeException(e);
        }
    }

    public void splitFile(Path path, Consumer<byte[]> filePartCons) {
        byte[] bytes;
        try {
            fileForSend = new RandomAccessFile(path.toFile(), "rw");
            ByteBuffer byteBuffer = ByteBuffer.allocate(MB_1);
            FileChannel fileChannel = fileForSend.getChannel();
            while (fileChannel.read(byteBuffer, MB_1 * part) != -1) {
                bytes = new byte[byteBuffer.flip().remaining()];
                byteBuffer.get(bytes);
                filePartCons.accept(bytes);
                byteBuffer.clear();
                part++;
            }
            fileForSend.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileChannel getFileChannel(String path) throws FileNotFoundException {
        FileChannel fileChannel = mapFileChannel.get(path);
        if (fileChannel == null) {
            RandomAccessFile fileForSend = new RandomAccessFile(path, "rw");
            fileChannel = fileForSend.getChannel();
            mapFileChannel.put(path, fileChannel);
        }
        return fileChannel;
    }

    public boolean getServerFile(FileResponse response){
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

    public void sendDirectory(Path srcPath, String login, String password) {
        this.srcPath = srcPath;
        try {
            List<String> directories;
            if (maxDepth - calculateDepth(srcPath) >= 0) {
                try (Stream<Path> paths = Files.walk(srcPath)) {
                    directories = paths
                            .filter(Files::isDirectory)
                            .sorted(Comparator.naturalOrder())
                            .map(path -> Paths.get(serverPC.getCurrentPath()).resolve(srcPath.getParent().relativize(path)).toString())
                            .collect(Collectors.toList());
                }
                SendDirectoriesRequest directoriesRequest = new SendDirectoriesRequest(login, password, directories, calculateSize(srcPath));
                network.getChannel().writeAndFlush(directoriesRequest).sync();
            } else {
                controller.getAlertError("В отправляемой директории превышено разрешенное\nкол-во вложенных директорий.\nУменьшите кол-во директорий для отправки.");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private int calculateDepth(Path srcPath) throws IOException {
        int srcDepth;
        try (Stream<Path> paths = Files.walk(srcPath)){
            srcDepth = (int) paths.filter(Files::isDirectory)
                    .map(Path::getParent)
                    .distinct()
                    .count();
        }
        return (srcDepth-1) + serverPC.getDepth();
    }

    private long calculateSize(Path srcPath) throws IOException {
        long size;
        try (Stream<Path> walk = Files.walk(srcPath)) {
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

    public void deleteFiles(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> paths = Files.walk(path)) {
                paths.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } else {
            Files.delete(path);
        }
    }
}
