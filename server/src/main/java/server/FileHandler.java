package server;

import service.serializedClasses.FileInfo;
import service.serializedClasses.GetFileListRequest;
import service.serializedClasses.UploadFileRequest;
import service.serializedClasses.SendFileRequest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
            System.out.println(fileRequest.getServerPath());

            Path path = Path.of(DIR, fileRequest.getLogin(), fileRequest.getServerPath());
            FileOutputStream fos = new FileOutputStream(path.toString());
            fos.write(fileRequest.getFile());
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static byte[] getFile(UploadFileRequest request) {
        Path path = Path.of(DIR, request.getLogin(), request.getServerPath());
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
}
