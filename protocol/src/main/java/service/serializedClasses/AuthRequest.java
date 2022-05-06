package service.serializedClasses;

public class AuthRequest extends BasicAuth implements BasicRequest {
    public String getLogin() {
        return super.getLogin();
    }

    public  String getPassword() {
        return super.getPassword();
    }

    public AuthRequest(String login, String password) {
        super(login, password);
    }

    @Override
    public String getType() {
        return "auth";
    }
}
