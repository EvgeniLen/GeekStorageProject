package service.serializedClasses;

public class RegRequest extends BasicAuth implements BasicRequest{
    public RegRequest(String login, String password) {
        super(login, password);
    }

    public String getLogin() {
        return super.getLogin();
    }

    public String getPassword() {
        return super.getPassword();
    }

    @Override
    public String getType() {
        return "reg";
    }
}
