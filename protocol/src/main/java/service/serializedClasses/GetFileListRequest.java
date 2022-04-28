package service.serializedClasses;

public class GetFileListRequest implements BasicRequest {
    private String login;
    private String password;
    private String subDirection;



    public GetFileListRequest(String login, String password, String subDirection) {
        this.login = login;
        this.password = password;
        this.subDirection = subDirection;
    }

    public String getPassword() {
        return password;
    }

    public String getLogin() {
        return login;
    }

    public String getSubDirection() {
        return subDirection;
    }

    @Override
    public String getType() {
        return "getFileList";
    }
}
