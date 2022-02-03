package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import service.ServiceMessages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextArea textArea;
    public TextField textField;
    public TextField loginField;
    public PasswordField passwordField;
    public HBox msgPanel;
    public HBox authPanel;
    public ListView<String> clientList;
    public MenuButton menuButton;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final String ADDRESS = "localhost";
    private final int PORT = 8189;

    private boolean authenticated;
    private String nickname;
    private String login;
    private String fileNameHistory;
    private Stage stage;
    public Stage chStage;
    public ChangeController chController;
    public Stage regStage;
    private RegController regController;

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                System.out.println("Bye");
                if (socket != null && !socket.isClosed()){
                    try {
                        out.writeUTF(ServiceMessages.END);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    public void connect(){
        try {
            socket = new Socket(ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true){
                        String str = in.readUTF();
                        if (str.startsWith("/")){
                            if (str.equals(ServiceMessages.END)) {
                                break;
                            }
                            if (str.startsWith(ServiceMessages.AUTH_OK)) {
                                nickname = str.split("\\s")[1];
                                setAuthenticated(true);
                                break;
                            }
                            if (str.startsWith(ServiceMessages.REG)){
                                regController.regStatus(str);
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                    //цикл работы
                    while (authenticated){
                        String str = in.readUTF();
                        if (str.startsWith("/")){
                            if (str.equals(ServiceMessages.END)){
                                setAuthenticated(false);
                                break;
                            }
                            if (str.startsWith(ServiceMessages.ClLIST)){
                                String[] token = str.split("\\s");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                            if (str.startsWith(ServiceMessages.CH_OK)) {
                                nickname = str.split("\\s")[1];
                                setTitle(nickname);
                                chController.changeStatus(str);
                            } else if (str.startsWith(ServiceMessages.CH_NO)){
                                chController.changeStatus(str);
                            }

                        } else {
                            textArea.appendText(str + "\n");
                            LocalHistory.writeHistory(fileNameHistory, str + "\n");
                        }
                    }
                } catch (IOException e){
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clickBtnAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()){
            connect();
        }

        try {
            String msg = String.format("%s %s %s", ServiceMessages.AUTH, loginField.getText().trim(), passwordField.getText().trim());
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clickBtnSendText(ActionEvent actionEvent) {
        if (textField.getText().length() > 0){
            try {
                out.writeUTF(textField.getText());
                textField.clear();
                textField.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
        menuButton.setVisible(authenticated);
        menuButton.setManaged(authenticated);

        if (!authenticated) {
            nickname = "";
        }

        setTitle(nickname);
        textArea.clear();

        if (authenticated){
            login = loginField.getText().trim();
            fileNameHistory = LocalHistory.getFileName(login);
            List<String> history = LocalHistory.getHistory(fileNameHistory);

            Platform.runLater(() -> {
                for (int i = history.size() > 100? history.size() - 100 : 0; i < history.size(); i++) {
                    textArea.appendText(history.get(i) + "\n");
                }
            });
        }
    }

    private void setTitle(String nickname) {
        String title;
        if (nickname.equals("")){
            title = "GeekChat";
        }else {
            title = String.format("GeekChat - %s", nickname);
        }
        Platform.runLater(() -> {
            stage.setTitle(title);
        });
    }

    public void clickClientList(MouseEvent mouseEvent) {
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textField.setText("/w " + receiver + " ");
    }

    public void clickBtnReg(ActionEvent actionEvent){
        if (regStage == null){
            createRegWindow();
        }
        regStage.show();
    }

    private void createRegWindow(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("GeekChat registration");
            regStage.setScene(new Scene(root, 500, 425));

            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.initStyle(StageStyle.UTILITY);

            regController = fxmlLoader.getController();
            regController.setController(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToReg(String login, String password, String nickname){
        if (socket == null || socket.isClosed()) {
            connect();
        }
        String msg = String.format("%s %s %s %s", ServiceMessages.REG, login, password, nickname);
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToChange(String oldNickname, String newNickname){
        if (socket == null || socket.isClosed()) {
            connect();
        }
        String msg = String.format("%s %s %s", ServiceMessages.CH_NICK, oldNickname, newNickname);
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createChangeNicknameWindow(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/changeNickname.fxml"));
            Parent root = fxmlLoader.load();
            chStage = new Stage();
            chStage.setTitle("GeekChat change nickname");
            chStage.setScene(new Scene(root, 500, 425));

            chStage.initModality(Modality.APPLICATION_MODAL);
            chStage.initStyle(StageStyle.UTILITY);

            chController = fxmlLoader.getController();
            chController.setController(this);
            chController.oldNickname.setText(nickname);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clickChangeName(Event event) {
        if (chStage == null) {
            createChangeNicknameWindow();
        } else {
            chController.oldNickname.setText(nickname);
            chController.newNickname.clear();
            chController.textArea.clear();
        }
        chController.newNickname.requestFocus();
        chStage.show();
    }
}
