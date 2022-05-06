package service.serializedClasses;

public class SendFileRequest extends BasicAuth implements BasicRequest {
    //private FileInfo fileInfo;
    private final byte[] file;
    private final String serverPath;
    private final String localPath;

    public SendFileRequest(String login, String password, byte[] file, String serverPath, String localPath) {
        super(login, password);
        this.file = file;
        this.serverPath = serverPath;
        this.localPath = localPath;
    }

    @Override
    public String getType() {
        return "sendFile";
    }

    public String getLogin() {
        return super.getLogin();
    }

    public String getPassword() {
        return super.getPassword();
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

