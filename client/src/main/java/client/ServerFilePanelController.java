package client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import service.serializedClasses.FileInfo;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ServerFilePanelController implements Initializable {
    @FXML
    public TableView<FileInfo> filesTable;
    @FXML
    public TextField patchField;

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Заполняем локальную таблицу
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setMaxWidth(24);

        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Имя");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        fileNameColumn.setMaxWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setMaxWidth(120);
        fileSizeColumn.setCellFactory(column -> new TableCell<FileInfo, Long>(){
            @Override
            protected void updateItem(Long aLong, boolean b) {
                super.updateItem(aLong, b);
                if (aLong == null || b){
                    setText(null);
                    setStyle("");
                } else {
                    String text = String.format("%,d bytes", aLong);
                    if (aLong == -1L) {
                        text = "[DIR]";
                    }
                    setText(text);
                }
            }
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setMaxWidth(240);

        filesTable.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);



        filesTable.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getClickCount() == 2){
                String dir = patchField.getText() + filesTable.getSelectionModel().getSelectedItem().getFilename();
                if (filesTable.getSelectionModel().getSelectedItem().getType().getName().equals("D")){
                    controller.sendFileListRequest(dir);
                }
            }
        });

        //updateList(Paths.get(DIR));
        //Заполняем локальную таблицу

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
        String dir = patchField.getText();
        dir = dir.substring(0, dir.indexOf(File.separator));
        controller.sendFileListRequest(dir);
    }

    public String getSelectedFileName(){
        if (!filesTable.isFocused()){
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public String getCurrentPath(){
        return patchField.getText();
    }
}
