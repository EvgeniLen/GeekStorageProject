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
            textArea.appendText(result.split("\\s", 3)[2]);
        }else {
            textArea.appendText(result.split("\\s", 2)[1]);
        }

    }

    public void clickBtnChange(ActionEvent actionEvent) {
        textArea.clear();
        String newNick = newNickname.getText().trim();

        String error = "";
        if (controller.getNickname().equals(newNick.trim())){
            error = "Укажите никнейм который отличается от старого.\n";
        }  else if (newNick.trim().equals("")) {
            error = "Укажите новый никнейм.\n";
        }

        if (error.length()>0) {
            textArea.appendText(error);
            return;
        }
        controller.tryToChange(newNick);
    }
}
