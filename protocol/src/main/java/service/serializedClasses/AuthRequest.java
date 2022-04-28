package service.serializedClasses;

public class AuthRequest implements BasicRequest {
    private String login;
    private String password;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public AuthRequest(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    public String getType() {
        return "auth";
    }
}
