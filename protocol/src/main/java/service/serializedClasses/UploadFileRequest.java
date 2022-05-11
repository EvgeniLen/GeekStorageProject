package service.serializedClasses;

public class UploadFileRequest extends BasicAuth implements BasicRequest{
    private final String serverPath;
    private final String localPath;

    public UploadFileRequest(String login, String password, String serverPath, String localPath) {
        super(login, password);
        this.serverPath = serverPath;
        this.localPath = localPath;
    }

    public String getServerPath() {
        return serverPath;
    }

    public String getLocalPath() {
        return localPath;
    }

    @Override
    public String getType() {
        return "getUploadFile";
    }
}
