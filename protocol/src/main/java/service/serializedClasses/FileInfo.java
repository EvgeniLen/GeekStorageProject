package service.serializedClasses;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FileInfo implements Serializable {
    public enum FileType {
        FILE("F"), DIRECTORY("D");
        private String name;

        FileType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private Byte[] file;
    private String filename;
    private FileType type;
    private long size;
    private LocalDateTime lastModified;

    public String getFilename() {
        return filename;
    }


    public FileType getType() {
        return type;
    }


    public long getSize() {
        return size;
    }


    public LocalDateTime getLastModified() {
        return lastModified;
    }


    public FileInfo(Path path) {
        try {
            this.filename = path.getFileName().toString();
            this.size = Files.size(path);
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            if (this.type == FileType.DIRECTORY) {
                this.size = -1L;
            }
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(0));
        } catch (IOException e) {
            throw new RuntimeException("Unable to create file info from path");
        }
    }
}
