package client;

import client.filePanels.*;
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
import java.io.IOException;
import java.net.URL;
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

    private String login;
    private String password;
    private Stage regStage;
    private RegController regController;
    private LocalFilePanelController localPC;
    private ServerFilePanelController serverPC;
    private Network network;
    private Stage stage;
    private Alert errorAlert;
    private ClientFileHandler fileHandler;
    private Path srcPath;
    private Path dstPath;

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

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textArea.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                network.close();
            });
        });
        network = new Network(this);
        fileHandler = ClientFileHandler.getHandler();
        setAuthenticated(false);
    }

    public void setAuthenticated(boolean authenticated) {
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
            fileHandler.setFilePanel(serverPC);
            fileHandler.setNetwork(network);
            fileHandler.setController(this);
            serverPC.setController(this);
        }
    }

    public void clickBtnAuth(ActionEvent actionEvent) {
        network.sendRequest(new AuthRequest(loginField.getText().trim(), passwordField.getText().trim()));
        login = loginField.getText().trim();
    }

    public void clickBtnReg(ActionEvent actionEvent){
        if (regStage == null){
            createRegWindow();
        }
        regStage.show();
    }

    public void btnExit(ActionEvent actionEvent) {
        network.close();
        Platform.exit();
    }

    public void btnLogOut(ActionEvent actionEvent) {
        loginField.clear();
        passwordField.clear();
        network.sendRequest(new LogOutRequest(login, password));
        setAuthenticated(false);
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

    public void copyButtonAction(ActionEvent actionEvent) {
        if (localPC.getSelectedFileName() == null && serverPC.getSelectedFileName() == null) {
            getAlert("Ни один файл для копирования не выбран.");
            return;
        }

        //Копировование с локала на сервер
        if (localPC.getSelectedFileName() != null) {
            srcPath = Paths.get(localPC.getCurrentPath(), localPC.getSelectedFileName());
            if (checkFile(localPC.getSelectedFileName(), serverPC)) {
                if (srcPath.toFile().isDirectory()) {
                    fileHandler.sendDirectory(srcPath, login, password);
                } else {
                    fileHandler.sendFiles(srcPath, login, password);
                }
            }
        } else if (serverPC.getSelectedFileName() != null) {
            srcPath = Paths.get(serverPC.getCurrentPath().equals(File.separator) ? "" : serverPC.getCurrentPath(), serverPC.getSelectedFileName());
            dstPath = Paths.get(localPC.getCurrentPath());
            if (checkFile(serverPC.getSelectedFileName(), localPC)) {
                network.sendRequest(new UploadFileRequest(login, password, srcPath.toString(), dstPath.toString()));
            }
        }
    }

    public void moveButtonAction(ActionEvent actionEvent) {
        if (localPC.getSelectedFileName() == null && serverPC.getSelectedFileName() == null) {
            getAlert("Ни один файл для перемещения не выбран.");
            return;
        }

        try {
            //Перемещение с локала на сервер
            if (localPC.getSelectedFileName() != null) {
                srcPath = Paths.get(localPC.getCurrentPath(), localPC.getSelectedFileName());
                fileHandler.sendFiles(srcPath, login, password);
                if (srcPath.toFile().isDirectory()) {
                    fileHandler.sendDirectory(srcPath, login, password);
                } else {
                    fileHandler.sendFiles(srcPath, login, password);
                }
                fileHandler.deleteFiles(srcPath);
                localPC.updateList(Paths.get(localPC.getCurrentPath()));
            } else if (serverPC.getSelectedFileName() != null) {
                srcPath = Paths.get(serverPC.getCurrentPath().equals(File.separator) ? "": serverPC.getCurrentPath(), serverPC.getSelectedFileName());
                dstPath = Paths.get(localPC.getCurrentPath());
                if (checkFile(serverPC.getSelectedFileName(), localPC)){
                    network.sendRequest(new MoveFileRequest(login, password, srcPath.toString(), dstPath.toString()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            getAlert("Не удалось переместить указанный файл.");
        }
    }

    public void delButtonAction(ActionEvent actionEvent) {
        if (localPC.getSelectedFileName() == null && serverPC.getSelectedFileName() == null) {
            getAlert("Ни один файл для удаления не выбран.");
            return;
        }
        try {
            if (localPC.getSelectedFileName() != null) {
                srcPath = Paths.get(localPC.getCurrentPath(), localPC.getSelectedFileName());
                fileHandler.deleteFiles(srcPath);
                localPC.updateList(Paths.get(localPC.getCurrentPath()));
            } else if (serverPC.getSelectedFileName() != null) {
                srcPath = Paths.get(serverPC.getCurrentPath().equals(File.separator) ? "" : serverPC.getCurrentPath(), serverPC.getSelectedFileName());
                network.sendRequest(new DelFileRequest(login, password, srcPath.toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            getAlert("Не удалось удалить выбранный файл.");
        }
    }

    public boolean checkFile(String name, BasicFilePanelController filePanelController){
        Alert alert = null;
        if (filePanelController.isFileExists(name) > 0) {
            alert = new Alert(Alert.AlertType.CONFIRMATION, String.format("В папке назначения уже есть файл \"%s\", продолжить выполнение?", name), ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
        }
        return alert == null || alert.getResult() == ButtonType.YES;
    }

    public void getAlertError(String textError){
        Platform.runLater(() -> getAlert(textError));
    }

    public void getAlert(String text) {
        if (errorAlert != null) {
            if (errorAlert.isShowing()) {
                return;
            }
        }
        errorAlert = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        errorAlert.showAndWait();
    }
}
