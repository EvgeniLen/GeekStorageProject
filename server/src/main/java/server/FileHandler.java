package server;

import io.netty.channel.ChannelHandlerContext;
import service.serializedClasses.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FileHandler {
    private static final String DIR = "ServerDirectory" + File.separator;


    public static void createUserDirectory(String login) {
        File directory = new File(DIR + login);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static List<FileInfo> getFilesAndDirectories(GetFileListRequest request) {
        try {
            return Files.list(Path.of(DIR + request.getLogin() + File.separator + request.getSubDirection()))
                    .map(FileInfo::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createFile(SendFileRequest fileRequest){
        try {
            Path path = Path.of(DIR, fileRequest.getLogin(), fileRequest.getServerPath());
            if (fileRequest.getFileInfo().getType().getName().equals("D")){
                Files.createDirectories(path);
            } else {
                FileOutputStream fos = new FileOutputStream(path.toString());
                fos.write(fileRequest.getFile());
                fos.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void getUploadFile(UploadFileRequest request, ChannelHandlerContext channel) {
        Path srcPath = Path.of(DIR, request.getLogin(), request.getServerPath());
        try {
            Files.walk(srcPath, 6)
                    //.filter(path -> !path.endsWith(srcPath.getFileName()))
                    .forEach(path -> {
                        byte[] data = new byte[0];
                        if (!path.toFile().isDirectory()) {
                            data = getFile(path);
                        }
                        Path dstPath = srcPath.getParent().relativize(path);
                        channel.writeAndFlush(new UploadFileResponse(data, request.getServerPath(), Paths.get(request.getLocalPath()).resolve(dstPath).toString(), new FileInfo(path)));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getMoveFile(MoveFileRequest request, ChannelHandlerContext channel) {
        Path srcPath = Path.of(DIR, request.getLogin(), request.getServerPath());
        try {
            Files.walk(srcPath, 6)
                    //.filter(path -> !path.endsWith(srcPath.getFileName()))
                    .forEach(path -> {
                        byte[] data = new byte[0];
                        if (!path.toFile().isDirectory()) {
                            data = getFile(path);
                        }
                        Path dstPath = srcPath.getParent().relativize(path);
                        channel.writeAndFlush(new MoveFileResponse(data, request.getServerPath(), Paths.get(request.getLocalPath()).resolve(dstPath).toString(), new FileInfo(path)));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        deleteFiles(srcPath);
    }
    public static boolean deleteFile(DelFileRequest request) {
        Path path = Path.of(DIR, request.getLogin(), request.getServerPath());
        deleteFiles(path);
        return true;
    }

    private static byte[] getFile(Path path){
        try {
            FileInputStream fileInputStream = new FileInputStream(path.toFile());
            byte[] data = new byte[fileInputStream.available()];
            fileInputStream.read(data);
            fileInputStream.close();
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteFiles(Path path){
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } else {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
