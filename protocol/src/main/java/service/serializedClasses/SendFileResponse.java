package service.serializedClasses;

public class SendFileResponse extends BasicResponse {
    private final String serverPath;

    public SendFileResponse(String serverPath) {
        super("sendFile");
        this.serverPath = serverPath;
    }

    public String getServerPath() {
        return serverPath;
    }
}
