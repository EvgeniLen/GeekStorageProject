package service.serializedClasses;

public class UploadFileResponse extends BasicResponse {
    private final byte[] file;
    private final String serverPath;
    private final String localPath;

    public UploadFileResponse(byte[] file, String serverPath, String localPath) {
        super("uploadFile");
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
