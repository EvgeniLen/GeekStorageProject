package client.filePanels;

import client.Controller;
import client.Network;
import service.serializedClasses.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientFileHandler {
    private static final int MB_2 = 2 * 1_000_000;
    private static final Map<String, FileChannel> mapFileChannel = new HashMap<>();
    private static ClientFileHandler handler;
    private ServerFilePanelController serverPC;
    private int maxDepth;
    private Network network;
    private Controller controller;
    private boolean needDelete;
    private String login;
    private String password;
    private Path localPath;
    private Path srcPatch;
    private int fileNumber;
    private FileChannel fileChannel;
    private Map<Path, Path> directories = new HashMap<>();

    private SendFileRequest repeatSendFile;

    public void setFileNumber(int fileNumber) {
        this.fileNumber = fileNumber;
    }

    public void incFileNumber() {
        mapFileChannel.remove(localPath.toString());
        this.fileNumber++;
    }

    public static ClientFileHandler getHandler(){
        if (handler == null){
            handler = new ClientFileHandler();
        }
        return handler;
    }

    public void setProperties(ServerFilePanelController serverPC, Network network, Controller controller, String login, String password) {
        this.serverPC = serverPC;
        this.controller = controller;
        this.network = network;
        this.login = login;
        this.password = password;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    private void sweep(){
        fileNumber = 0;
        directories.clear();
        try {
            if (fileChannel != null) {
                fileChannel.close();
            }
            if (needDelete) {
                deleteFiles(srcPatch);
                controller.getLocalPC().updateList(Paths.get(controller.getLocalPC().getCurrentPath()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendFiles() {
        if (fileNumber >= directories.size()) {
            sweep();
            return;
        }
        localPath = new ArrayList<>(directories.keySet()).get(fileNumber);
        splitFile(localPath, (data) -> {
            SendFileRequest sendFileRequest = new SendFileRequest(login, password, data, directories.get(localPath).toString(), new FileInfo(localPath));
            repeatSendFile = sendFileRequest;
            try {
                network.getChannel().writeAndFlush(sendFileRequest).sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void repeatSendFile(){
        try {
            network.getChannel().writeAndFlush(repeatSendFile).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void splitFile(Path path, Consumer<byte[]> filePartCons) {
        byte[] filePart;
        try {
            if (!Files.isDirectory(path)){
                ByteBuffer byteBuffer = ByteBuffer.allocate(MB_2);
                fileChannel = getFileChannel(path.toString());
                while (fileChannel.read(byteBuffer) != -1) {
                    filePart = new byte[byteBuffer.flip().remaining()];
                    byteBuffer.get(filePart);
                    byteBuffer.clear();
                    filePartCons.accept(filePart);
                    return;
                }
            }else {
                filePartCons.accept(new byte[0]);
            }
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

    private int calculateDepth(Path srcPath) throws IOException {
        AtomicInteger srcDepth = new AtomicInteger();
        try (Stream<Path> paths = Files.walk(srcPath)){
            paths.filter(Files::isDirectory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        int i = path.toString().split("[\\\\/]").length;
                        if (srcDepth.get() < i){
                            srcDepth.set(i);
                        }
                    });
        }
        return (srcDepth.get() - 1) + serverPC.getDepth();
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

    public void getPermission(Path srcPath) throws IOException {
        int depth = calculateDepth(srcPath);
        if (maxDepth - depth >= 0) {
            try (Stream<Path> paths = Files.walk(srcPath)) {
                directories = paths
                        .collect(Collectors.toMap(p1 -> p1, p2 -> Paths.get(serverPC.getCurrentPath()).resolve(srcPath.getParent().relativize(p2))));
            }
            GetPermissionRequest permissionRequest = new GetPermissionRequest(login, password, depth, calculateSize(srcPath));
            network.getChannel().writeAndFlush(permissionRequest);
        } else {
            controller.getAlertError("В отправляемой директории превышено разрешенное\nкол-во вложенных директорий.\nУменьшите кол-во директорий для отправки.");
        }
    }

    public void NeedDeletePatch(Path srcPath, boolean b) {
        this.srcPatch = srcPath;
        this.needDelete = b;
    }
}
