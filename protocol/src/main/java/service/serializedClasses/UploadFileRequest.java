package service.serializedClasses;



public class UploadFileRequest implements BasicRequest{
    private final String login;
    private final String password;
    private final String serverPath;
    private final String localPath;

    public UploadFileRequest(String login, String password, String serverPath, String localPath) {
        this.login = login;
        this.password = password;
        this.serverPath = serverPath;
        this.localPath = localPath;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
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
