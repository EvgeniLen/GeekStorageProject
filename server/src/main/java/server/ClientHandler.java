package server;

import service.ServiceMessages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket, ExecutorService es) {
        this.server = server;
        this.socket = socket;

        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            es.execute(() -> {
                try {
                    //цикл аутентификации
                    socket.setSoTimeout(120000);
                    while (true) {
                        String str = in.readUTF();
                        if (str.equals(ServiceMessages.END)) {
                            logger.log(Level.FINE, String.format("%s sent a command %s", nickname, str));
                            sendMsg(ServiceMessages.END);
                            break;
                        }
                        if (str.startsWith(ServiceMessages.AUTH)) {
                            String[] token = str.split("\\s", 3);
                            if (token.length < 3) {
                                continue;
                            }
                            logger.log(Level.FINE, String.format("Sent a command %s", str));
                            String newNick = server.getAuthService().getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    authenticated = true;
                                    sendMsg(ServiceMessages.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    logger.log(Level.INFO, "Client " + nickname + " authenticated");
                                    //System.out.println("Client: " + nickname + " authenticated");
                                    break;
                                } else {
                                    sendMsg("С этим логином уже зашли в чат");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }

                        if (str.startsWith(ServiceMessages.REG)) {
                            String[] token = str.split(" ", 4);
                            if (token.length < 4) {
                                continue;
                            }
                            logger.log(Level.FINE, String.format("Sent a command %s", str));
                            if (server.getAuthService().registration(token[1], token[2], token[3])) {
                                sendMsg(ServiceMessages.REG_OK);
                            } else {
                                sendMsg(ServiceMessages.REG_NO);
                            }
                        }

                    }
                    //цикл работы
                    socket.setSoTimeout(0);
                    while (authenticated) {
                        String str = in.readUTF();
                        if (str.equals(ServiceMessages.END)) {
                            logger.log(Level.FINE, String.format("%s sent a command %s", nickname, str));
                            sendMsg(ServiceMessages.END);
                            break;
                        }

                        if (str.startsWith("/w ")) {
                            String[] msg = str.split("\\s", 3);
                            if (msg.length < 3) {
                                continue;
                            }
                            if (msg[2].length() > 0) {
                                logger.log(Level.FINER, String.format("%s sent a private message to %s '%s'", nickname, msg[1], msg[2]));
                                server.sendPrivateMsg(this, msg[1], msg[2]);
                            }

                        } else if (str.startsWith(ServiceMessages.CH_NICK)) {
                            String[] token = str.split("\\s+", 2);
                            if (token.length < 2) {
                                continue;
                            }
                            if (token[1].contains(" ")){
                                sendMsg(ServiceMessages.CH_NO + " " + "Ник не может содержать пробелов");
                                continue;
                            }
                            logger.log(Level.FINE, String.format("Sent a command %s", str));
                            if (server.getAuthService().changeNickname(this.nickname, token[1])) {
                                nickname = token[1];
                                sendMsg(ServiceMessages.CH_OK + " " + nickname + " Ваш ник изменен на " + nickname);
                                server.broadcastClientList();
                            } else {
                                sendMsg(ServiceMessages.CH_NO + " Не удалось изменить ник. Ник " + token[1] + " уже существует");
                            }
                        } else {
                            logger.log(Level.FINER, String.format("%s sent a message '%s'", login, str));
                            server.broadcastMsg(this, str);
                        }
                    }
                } catch (SocketTimeoutException e){
                    sendMsg(ServiceMessages.END);
                } catch (IOException e){
                    e.printStackTrace();
                } finally {
                    logger.log(Level.INFO,"Client " + nickname + " disconnect!");
                    //System.out.println("Client disconnect!");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getLogin() {
        return login;
    }

    public void sendMsg(String msg){
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }
}
