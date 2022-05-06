package service.serializedClasses;

import java.io.Serializable;

public abstract class BasicAuth implements Serializable {
    private final String login;
    private final String password;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public BasicAuth(String login, String password) {
        this.login = login;
        this.password = password;
    }

}
