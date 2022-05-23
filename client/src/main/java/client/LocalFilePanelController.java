package client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import service.serializedClasses.FileInfo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class LocalFilePanelController implements Initializable, BasicFilePanelController {
    public static final String DIR = "ClientDirectory" + File.separator;
    public TableView<FileInfo> filesTable;
    public TextField patchField;

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
                Path path = Paths.get(patchField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                if (Files.isDirectory(path)){
                    updateList(path);
                }
            }
        });

        updateList(Paths.get(DIR));
        //Заполняем локальную таблицу

    }

    public void updateList(Path path){
        try {
            patchField.setText(path.normalize().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path)
                    .map(FileInfo::new)
                    .collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void patchUpAction(ActionEvent actionEvent) {
        Path upperPatch = Paths.get(patchField.getText()).getParent();
        if (upperPatch != null) {
            updateList(upperPatch);
        }
    }

    public String getSelectedFileName(){
        if (!filesTable.isFocused() || filesTable.getSelectionModel().getSelectedItem() == null){
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public String getCurrentPath(){
        return patchField.getText();
    }

    @Override
    public int isFileExists(String name) {
        return filesTable.getItems()
                .filtered(fileInfo -> fileInfo.getFilename().equals(name)).size();
    }
}
