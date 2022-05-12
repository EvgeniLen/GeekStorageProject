package service.serializedClasses;

public class RegRequest implements BasicRequest{
    private String login;
    private String password;

    public RegRequest(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String getType() {
        return "reg";
    }
}
