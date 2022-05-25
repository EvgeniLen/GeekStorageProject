package service.serializedClasses;


public class SendFileRequest extends BasicAuth implements BasicRequest {
    private final FileInfo fileInfo;
    private final byte[] file;
    private final String serverPath;

    public SendFileRequest(String login, String password, byte[] file, String serverPath, FileInfo fileInfo) {
        super(login, password);
        this.file = file;
        this.serverPath = serverPath;
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

    public FileInfo getFileInfo() {
        return fileInfo;
    }

}

