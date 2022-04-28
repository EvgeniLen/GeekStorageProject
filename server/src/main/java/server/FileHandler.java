package server;

import com.sun.nio.sctp.SendFailedNotification;
import service.serializedClasses.FileInfo;
import service.serializedClasses.SendFileRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public static List<FileInfo> getFilesAndDirectories(String login, String subDirectory) {
        try {
            return Files.list(Path.of(DIR + login + File.separator + subDirectory))
                    .map(FileInfo::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createFile(SendFileRequest fileRequest){

        try {
            String path = fileRequest.getPath();
            FileOutputStream fos = new FileOutputStream(DIR + path);
            fos.write(fileRequest.getFile());
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
