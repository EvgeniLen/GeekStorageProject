package service.serializedClasses;

public class GetFileListRequest extends BasicAuth implements BasicRequest {
    private final String subDirection;

    public GetFileListRequest(String login, String password, String subDirection) {
        super(login, password);
        this.subDirection = subDirection;
    }

    public String getSubDirection() {
        return subDirection;
    }

    @Override
    public String getType() {
        return "getFileList";
    }
}
