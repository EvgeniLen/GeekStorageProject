package client.filePanels;

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
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalFilePanelController implements Initializable, BasicFilePanelController {
    public static final String DIR = "ClientDirectory" + File.separator;
    public TableView<FileInfo> filesTable;
    public TextField patchField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Заполняем локальную таблицу
        PanelActions.initializePanel(filesTable);

        filesTable.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getClickCount() == 2){
                Path path = Paths.get(patchField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                if (Files.isDirectory(path)){
                    updateList(path);
                }
            }
        });

        updateList(Paths.get(DIR));
    }

    public void updateList(Path path){
        try {
            patchField.setText(path.normalize().toString());
            filesTable.getItems().clear();
            try (Stream<Path> paths = Files.list(path)){
                filesTable.getItems().addAll(paths
                        .map(FileInfo::new)
                        .collect(Collectors.toList()));
            }
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

    @Override
    public int isFileExists(String name) {
        return filesTable.getItems()
                .filtered(fileInfo -> fileInfo.getFilename().equals(name))
                .size();
    }
}
