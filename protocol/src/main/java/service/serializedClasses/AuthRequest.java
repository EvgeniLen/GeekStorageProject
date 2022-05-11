package service.serializedClasses;

public class AuthRequest extends BasicAuth implements BasicRequest {
    public AuthRequest(String login, String password) {
        super(login, password);
    }

    @Override
    public String getType() {
        return "auth";
    }
}
