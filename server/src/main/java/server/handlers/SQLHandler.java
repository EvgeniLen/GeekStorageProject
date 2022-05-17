package server.handlers;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SQLHandler {
    private static Connection connection;
    private static PreparedStatement prInsert;
    private static PreparedStatement prSelectConf;
    private static PreparedStatement prSelectNickAndLogin;


    public static boolean connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:server/geekStorage.db");
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
        prSelectNickAndLogin = connection.prepareStatement("SELECT login FROM users WHERE login = ? AND password = ?");
        prInsert = connection.prepareStatement("INSERT INTO users (login, password) VALUES (?, ?)");
        prSelectConf = connection.prepareStatement("SELECT conf_type, value FROM configurations");
    }

    public static boolean getAutentificationResult(String login, String password) {
        boolean isAuthenticated = false;
        try {
            prSelectNickAndLogin.setString(1, login);
            prSelectNickAndLogin.setString(2, password);
            ResultSet result = prSelectNickAndLogin.executeQuery();
            if (result.next()) {
                isAuthenticated = true;
            }
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isAuthenticated;
    }

    public static boolean registration(String login, String password) {
        try {
            prInsert.setString(1, login);
            prInsert.setString(2,  password);
            prInsert.executeUpdate();
            return true;
        }  catch (SQLException e) {
            //e.printStackTrace();
            return false;
        }
    }

    public static Map<String, Long> getConfiguration() {
        Map<String, Long> map = new HashMap<>();
        try {
            ResultSet result = prSelectConf.executeQuery();

            while (result.next()){
                map.put(result.getString("conf_type"), result.getLong("value"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return map;
    }
}
