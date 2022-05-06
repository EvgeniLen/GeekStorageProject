package service.serializedClasses;

public class MoveFileResponse extends BasicResponse {
    private final byte[] file;
    private final String serverPath;
    private final String localPath;

    public MoveFileResponse(byte[] file, String serverPath, String localPath) {
        super("moveFile");
        this.file = file;
        this.serverPath = serverPath;
        this.localPath = localPath;
    }

    public byte[] getFile() {
        return file;
    }

    public String getServerPath() {
        return serverPath;
    }

    public String getLocalPath() {
        return localPath;
    }
}
