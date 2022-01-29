package server;

import java.sql.*;

public class DbAuthService implements AuthService{
    private Connection connection;
    private PreparedStatement prInsert;
    private PreparedStatement prSelectNickAndLogin;
    private PreparedStatement prSelectNickOrLogin;
    private PreparedStatement prSelectChange;
    private PreparedStatement prSelectNick;

    private void connect() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:server/chatUsers.db");
    }

    private void disconnect(){
        try {
            if (connection!= null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void prepareAllStatement() throws SQLException {
        prSelectNickAndLogin = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND password = ?");
        prSelectNickOrLogin = connection.prepareStatement("SELECT * FROM users WHERE login = ? OR nickname = ?");
        prSelectNick = connection.prepareStatement("SELECT * FROM users WHERE nickname = ?");
        prSelectChange = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?");
        prInsert = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?)");
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            connect();
            prepareAllStatement();

            prSelectNickAndLogin.setString(1, login);
            prSelectNickAndLogin.setString(2, password);
            ResultSet result = prSelectNickAndLogin.executeQuery();

            if (result.next()) {
                return result.getString("nickname");
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            disconnect();
        }

    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        try {
            connect();
            prepareAllStatement();
            prSelectNickOrLogin.setString(1, login);
            prSelectNickOrLogin.setString(2, nickname);
            if (prSelectNickOrLogin.executeQuery().next()){
                return false;
            }
            prInsert.setString(1, login);
            prInsert.setString(2,  password);
            prInsert.setString(3, nickname);
            prInsert.executeUpdate();
            return true;
        }  catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            disconnect();
        }
    }

    @Override
    public boolean changeNickname(String oldNickname, String newNickName) {
        try {
            connect();
            prepareAllStatement();
            prSelectNick.setString(1, newNickName);
            if (prSelectNickOrLogin.executeQuery().next()){
                return false;
            }
            prSelectChange.setString(1, newNickName);
            prSelectChange.setString(2, oldNickname);
            prSelectChange.executeUpdate();
            return true;
        }  catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            disconnect();
        }
    }


}
