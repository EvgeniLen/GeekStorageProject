package service.serializedClasses;

public class LogOutRequest extends BasicAuth implements BasicRequest {
    public LogOutRequest(String login, String password) {
        super(login, password);
    }

    @Override
    public String getType() {
        return "logOut";
    }
}
