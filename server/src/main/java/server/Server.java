package server;

import service.ServiceMessages;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static ServerSocket server;
    private static Socket socket;
    private static final int PORT = 8189;

    static {
        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("server/logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() {
        if (!SQLHandler.connect()) {
            throw new RuntimeException("Не удалось подключиться к БД");
        }
        authService = new DbAuthService();
        clients = new CopyOnWriteArrayList<>();
        ExecutorService es = Executors.newCachedThreadPool();

        try {
            server = new ServerSocket(PORT);
            logger.log(Level.INFO, "Server started!");
            //System.out.println("Server started!");

            while (true) {
                socket = server.accept();
                logger.log(Level.INFO, "Client connected: " + socket.getRemoteSocketAddress());
                //System.out.println("Client connected: " + socket.getRemoteSocketAddress());
                new ClientHandler(this, socket, es);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            SQLHandler.disconnect();
            es.shutdown();
            logger.log(Level.SEVERE, "Server stop");
            //System.out.println("Server stop");
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public void broadcastMsg(ClientHandler sender, String msg){
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void sendPrivateMsg(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[ %s ] to [ %s ]: %s", sender.getNickname(), receiver, msg);
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(receiver)) {
                c.sendMsg(message);
                if (!sender.getNickname().equals(receiver)) {
                    sender.sendMsg(message);
                }
                return;
            }
        }
        sender.sendMsg("not found user: " + receiver);
    }

    public boolean isLoginAuthenticated(String login){
        for (ClientHandler client : clients) {
            if (client.getLogin().equals(login)){
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList(){
        StringBuilder sb = new StringBuilder(ServiceMessages.ClLIST);
        for (ClientHandler client : clients) {
            sb.append(" ").append(client.getNickname());
        }

        String message = sb.toString();

        for (ClientHandler client : clients) {
            client.sendMsg(message);
        }
    }

    public AuthService getAuthService() {
        return authService;
    }
}
