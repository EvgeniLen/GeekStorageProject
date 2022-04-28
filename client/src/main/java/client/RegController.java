package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import service.ServiceMessages;


public class RegController {

    @FXML
    public TextField loginField;
    public PasswordField passwordField;
    public TextArea textArea;

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void clickBtnReg(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        controller.tryToReg(login, password);
    }

    public void regStatus(String result){
        if (result.equals(ServiceMessages.REG_OK)){
            textArea.appendText("Регистрация прошла успешно\n");
        } else {
            textArea.appendText("Регистрация не получилась. Логин занят\n");
        }
    }

}
