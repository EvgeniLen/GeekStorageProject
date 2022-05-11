package service.serializedClasses;

public class SendFileRequest extends BasicAuth implements BasicRequest {
    private final FileInfo fileInfo;
    private final byte[] file;
    private final String serverPath;
    private final String localPath;

    public SendFileRequest(String login, String password, byte[] file, String serverPath, String localPath, FileInfo fileInfo) {
        super(login, password);
        this.file = file;
        this.serverPath = serverPath;
        this.localPath = localPath;
        this.fileInfo = fileInfo;
    }

    @Override
    public String getType() {
        return "sendFile";
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

    public FileInfo getFileInfo() {
        return fileInfo;
    }
}

