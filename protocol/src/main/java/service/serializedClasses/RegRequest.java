package service.serializedClasses;

public class RegRequest extends BasicAuth implements BasicRequest{
    public RegRequest(String login, String password) {
        super(login, password);
    }

    @Override
    public String getType() {
        return "reg";
    }
}
