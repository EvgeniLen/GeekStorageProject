package server;

import java.sql.*;

public class SQLHandler {
    private static Connection connection;
    private static PreparedStatement prInsert;
    private static PreparedStatement prSelectNickAndLogin;
    private static PreparedStatement prSelectChange;


    public static boolean connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:server/chatUsers.db");
            prepareAllStatement();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void disconnect(){
        try {
            prInsert.close();
            prSelectChange.close();
            prSelectNickAndLogin.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (connection!= null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void prepareAllStatement() throws SQLException {
        prSelectNickAndLogin = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND password = ?");
        prSelectChange = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?");
        prInsert = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?)");
    }

    public static String getNicknameByLoginAndPassword(String login, String password) {
        String nick = null;
        try {
            prSelectNickAndLogin.setString(1, login);
            prSelectNickAndLogin.setString(2, password);
            ResultSet result = prSelectNickAndLogin.executeQuery();

            if (result.next()) {
                nick = result.getString("nickname");
            }
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nick;
    }

    public static boolean registration(String login, String password, String nickname) {
        try {
            prInsert.setString(1, login);
            prInsert.setString(2,  password);
            prInsert.setString(3, nickname);
            prInsert.executeUpdate();
            return true;
        }  catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean changeNickname(String oldNickname, String newNickName) {
        try {
            prSelectChange.setString(1, newNickName);
            prSelectChange.setString(2, oldNickname);
            prSelectChange.executeUpdate();
            return true;
        }  catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
