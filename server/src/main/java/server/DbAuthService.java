package server;

import server.handlers.SQLHandler;

import java.util.Map;

public class DbAuthService implements AuthService{

    @Override
    public boolean getAutentificationResult(String login, String password) {
        return SQLHandler.getAutentificationResult(login, password);
    }

    @Override
    public boolean registration(String login, String password) {
       return SQLHandler.registration(login, password);
    }

    @Override
    public Map<String, Long> getConfiguration() {
        return SQLHandler.getConfiguration();
    }
}
