package client.filePanels;

public interface BasicFilePanelController {
    int isFileExists(String name);
    String getCurrentPath();
    String getSelectedFileName();
}
