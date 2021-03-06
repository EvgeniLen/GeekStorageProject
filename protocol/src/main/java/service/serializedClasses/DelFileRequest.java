package service.serializedClasses;

public class DelFileRequest extends BasicAuth implements BasicRequest{
    private final String serverPath;

    public DelFileRequest(String login, String password, String serverPath) {
        super(login, password);
        this.serverPath = serverPath;
    }

    public String getServerPath() {
        return serverPath;
    }


    @Override
    public String getType() {
        return "deleteFile";
    }
}
