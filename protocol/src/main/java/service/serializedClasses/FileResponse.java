package service.serializedClasses;

public class FileResponse extends BasicResponse {
    private final FileInfo fileInfo;
    private final byte[] file;
    private final String localPath;

    public FileResponse(String typeResponse, byte[] file, String localPath, FileInfo fileInfo) {
        super(typeResponse);
        this.file = file;
        this.localPath = localPath;
        this.fileInfo = fileInfo;
    }

    public byte[] getFile() {
        return file;
    }

    public String getLocalPath() {
        return localPath;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }
}
