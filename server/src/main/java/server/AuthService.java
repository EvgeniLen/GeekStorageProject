package server;

import java.util.Map;

public interface AuthService {
    /**
     * Результат аутентификации
     * true - прошел
     * false - не прошел
     * */
    boolean getAutentificationResult(String login, String password);

    /**
     * Регистрация нового пользователя
     * при успешной регистрации (логин занят) вернет true
     * иначе вернет false
     * */
    boolean registration(String login, String password);

    /**
     * Получить настройки из базы
     * возвращает map, ключ - значение
     * */
    Map<String, Long> getConfiguration();

}
