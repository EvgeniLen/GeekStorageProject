package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import service.ServiceMessages;

public class ChangeController {
    @FXML
    public TextField oldNickname;
    public TextField newNickname;
    public TextArea textArea;

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void changeStatus(String result){
        if (result.startsWith(ServiceMessages.CH_OK)) {
            oldNickname.setText(controller.getNickname());
            newNickname.clear();
            textArea.appendText("Смена никнейма прошла успешно.\n");
        }else {
            textArea.appendText("Смена никнейма не удалась. Возможно указанный никнейм уже занят.\n");
        }

    }

    public void clickBtnChange(ActionEvent actionEvent) {
        textArea.clear();
        String oldNick = oldNickname.getText().trim();
        String newNick = newNickname.getText().trim();

        String error = "";
        if (controller.getNickname().equals(newNick.trim())){
            error = "Укажите никнейм который отличается от старого.\n";
        } else if (oldNick.trim().equals("")) {
            error = "Укажите старый никнейм.\n";
        } else if (!controller.getNickname().equals(oldNick.trim())){
            error = oldNick + " - это не ваш никнейм.\n";
        } else if (newNick.trim().equals("")) {
            error = "Укажите новый никнейм.\n";
        }

        if (error.length()>0) {
            textArea.appendText(error);
            oldNickname.setText(controller.getNickname());
            return;
        }
        controller.tryToChange(oldNick, newNick);
    }
}
