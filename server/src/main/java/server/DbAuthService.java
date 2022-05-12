package server;

public class DbAuthService implements AuthService{

    @Override
    public boolean getAutentificationResult(String login, String password) {
        return SQLHandler.getAutentificationResult(login, password);
    }

    @Override
    public boolean registration(String login, String password) {
       return SQLHandler.registration(login, password);
    }


}
