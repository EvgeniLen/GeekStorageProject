package client.filePanels;

import client.Controller;
import client.Network;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import service.serializedClasses.FileInfo;
import service.serializedClasses.GetFileListRequest;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ServerFilePanelController implements Initializable, BasicFilePanelController {
    @FXML
    public TableView<FileInfo> filesTable;
    @FXML
    public TextField patchField;

    private Controller controller;
    private Network network;
    private int depth = 0;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Заполняем серверную таблицу
        PanelActions.initializePanel(filesTable);

        filesTable.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getClickCount() == 2){
                String dir = (patchField.getText().endsWith("\\") ? patchField.getText(): patchField.getText() + File.separator) + filesTable.getSelectionModel().getSelectedItem().getFilename();
                if (filesTable.getSelectionModel().getSelectedItem().getType().getName().equals("D")){
                    depth++;
                    network.sendRequest(new GetFileListRequest(controller.getLogin(), controller.getPassword(), dir));
                }
            }
        });
    }

    public void updateList(String subDirection, List<FileInfo> files){
        if (subDirection.equals("")) {
            subDirection = File.separator;
        }
        patchField.setText(subDirection);
        filesTable.getItems().clear();
        filesTable.getItems().addAll(files);
        filesTable.sort();
    }

    public void patchUpAction(ActionEvent actionEvent) {
        Path upperPatch = Paths.get(patchField.getText()).getParent();
        if (upperPatch != null) {
            depth--;
            network.sendRequest(new GetFileListRequest(controller.getLogin(), controller.getPassword(), upperPatch.toString()));
        }

    }

    @Override
    public String getSelectedFileName(){
        if (!filesTable.isFocused() || filesTable.getSelectionModel().getSelectedItem() == null){
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    @Override
    public String getCurrentPath(){
        return patchField.getText();
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public int isFileExists(String name){
        return filesTable.getItems()
                .filtered(fileInfo -> fileInfo.getFilename().equals(name))
                .size();
    }
}
