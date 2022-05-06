package service.serializedClasses;

public class DelFileResponse extends BasicResponse {
    private final String serverPath;

    public DelFileResponse(String serverPath) {
        super("delFile");
        this.serverPath = serverPath;
    }

    public String getServerPath() {
        return serverPath;
    }
}
