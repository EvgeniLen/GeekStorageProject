package client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import service.serializedClasses.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class Controller  implements Initializable{
    @FXML
    public TextArea textArea;
    public VBox localPanel;
    public VBox serverPanel;
    public MenuBar menuBar;
    public HBox basicButtonsPanel;
    public HBox filesPanel;
    public TextField loginField;
    public PasswordField passwordField;
    public VBox authPanel;

    private boolean authenticated = false;
    private String login;
    private String password;
    public Stage regStage;
    private RegController regController;
    private LocalFilePanelController localPC;
    private ServerFilePanelController serverPC;
    private Network network;

    public RegController getRegController() {
        return regController;
    }

    public LocalFilePanelController getLocalPC() {
        return localPC;
    }

    public ServerFilePanelController getServerPC() {
        return serverPC;
    }

    public Network getNetwork() {
        return network;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        network = new Network(this);
        setAuthenticated(false);
    }

    public void clickBtnAuth(ActionEvent actionEvent) {
        network.sendRequest(new AuthRequest(loginField.getText().trim(), passwordField.getText().trim()));
        login = loginField.getText().trim();
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        textArea.setVisible(!authenticated);
        textArea.setManaged(!authenticated);
        filesPanel.setVisible(authenticated);
        filesPanel.setManaged(authenticated);
        menuBar.setVisible(authenticated);
        menuBar.setManaged(authenticated);
        basicButtonsPanel.setVisible(authenticated);
        basicButtonsPanel.setManaged(authenticated);

        textArea.clear();

        if (authenticated){
            login = loginField.getText().trim();
            password = passwordField.getText().trim();
            localPC = (LocalFilePanelController) localPanel.getProperties().get("ctrl");

            serverPC = (ServerFilePanelController) serverPanel.getProperties().get("ctrl");
            serverPC.setController(this);
        }
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
            regStage.setTitle("GeekStorage - Регистрация нового пользователя");
            regStage.setScene(new Scene(root, 500, 425));

            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.initStyle(StageStyle.UTILITY);

            regController = fxmlLoader.getController();
            regController.setNetwork(network);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void btnExit(ActionEvent actionEvent) {
        network.close();
        Platform.exit();
    }

    public void copyButtonAction(ActionEvent actionEvent) {
        if (localPC.getSelectedFileName() == null && serverPC.getSelectedFileName() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл для копирования не выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            //Копировование с локала на сервер
            if (localPC.getSelectedFileName() != null) {
                Path srcPath = Paths.get(localPC.getCurrentPath(), localPC.getSelectedFileName());
                Path dstPath = Paths.get(serverPC.getCurrentPath()).resolve(srcPath.getFileName().toString());

                FileInputStream fileInputStream = new FileInputStream(srcPath.toFile());
                byte[] data = new byte[fileInputStream.available()];
                fileInputStream.read(data);
                fileInputStream.close();

                network.sendRequest(new SendFileRequest(login, password, data, dstPath.toString(), srcPath.toString()));
                network.sendRequest(new GetFileListRequest(login, password, serverPC.getCurrentPath()));
            } else if (serverPC.getSelectedFileName() != null) {
                Path srcPath = Paths.get(serverPC.getCurrentPath().equals(File.separator) ? "": serverPC.getCurrentPath(), serverPC.getSelectedFileName());
                Path dstPath = Paths.get(localPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
                network.sendRequest(new UploadFileRequest(login, password, srcPath.toString(), dstPath.toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
            alert.showAndWait();
        }

    }

    public void moveButtonAction(ActionEvent actionEvent) {
        if (localPC.getSelectedFileName() == null && serverPC.getSelectedFileName() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл для перемещения не выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            //Перемещение с локала на сервер
            if (localPC.getSelectedFileName() != null) {
                Path srcPath = Paths.get(localPC.getCurrentPath(), localPC.getSelectedFileName());
                Path dstPath = Paths.get(serverPC.getCurrentPath()).resolve(srcPath.getFileName().toString());

                FileInputStream fileInputStream = new FileInputStream(srcPath.toFile());
                byte[] data = new byte[fileInputStream.available()];
                fileInputStream.read(data);
                fileInputStream.close();

                network.sendRequest(new SendFileRequest(login, password, data, dstPath.toString(), srcPath.toString()));
                network.sendRequest(new GetFileListRequest(login, password, serverPC.getCurrentPath()));
                if (srcPath.toFile().delete()){
                    localPC.updateList(Paths.get(localPC.getCurrentPath()));
                }
            } else if (serverPC.getSelectedFileName() != null) {
                Path srcPath = Paths.get(serverPC.getCurrentPath().equals(File.separator) ? "": serverPC.getCurrentPath(), serverPC.getSelectedFileName());
                Path dstPath = Paths.get(localPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
                System.out.println(dstPath);
                network.sendRequest(new MoveFileRequest(login, password, srcPath.toString(), dstPath.toString()));
                localPC.updateList(Paths.get(localPC.getCurrentPath()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void delButtonAction(ActionEvent actionEvent) {
        if (localPC.getSelectedFileName() == null && serverPC.getSelectedFileName() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл для удаления не выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        if (localPC.getSelectedFileName() != null) {
            Path srcPath = Paths.get(localPC.getCurrentPath(), localPC.getSelectedFileName());
            if (srcPath.toFile().delete()){
                localPC.updateList(Paths.get(localPC.getCurrentPath()));
            }
        } else if (serverPC.getSelectedFileName() != null) {
            Path srcPath = Paths.get(serverPC.getCurrentPath().equals(File.separator) ? "": serverPC.getCurrentPath(), serverPC.getSelectedFileName());
            network.sendRequest(new DelFileRequest(login, password, srcPath.toString()));
        }

    }

}
