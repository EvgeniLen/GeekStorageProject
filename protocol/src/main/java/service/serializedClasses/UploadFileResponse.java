package service.serializedClasses;

import java.nio.file.Path;

public class UploadFileResponse extends BasicResponse {
    private byte[] file;
    private String serverPath;
    private String localPath;

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
