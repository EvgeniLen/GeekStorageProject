package service.serializedClasses;

public class MoveFileResponse extends BasicResponse {
    private final FileInfo fileInfo;
    private final byte[] file;
    private final String serverPath;
    private final String localPath;

    public MoveFileResponse(byte[] file, String serverPath, String localPath, FileInfo fileInfo) {
        super("moveFile");
        this.file = file;
        this.serverPath = serverPath;
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
