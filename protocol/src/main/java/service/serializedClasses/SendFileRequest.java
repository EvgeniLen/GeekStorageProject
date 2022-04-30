package service.serializedClasses;


import java.nio.file.Path;

public class SendFileRequest implements BasicRequest {
    private String login;
    private String password;
    //private FileInfo fileInfo;
    private byte[] file;

    private String serverPath;
    private String localPath;

    public SendFileRequest(String login, String password, byte[] file, String serverPath, String localPath) {
        this.login = login;
        this.password = password;
        this.file = file;
        this.serverPath = serverPath;
        this.localPath = localPath;
    }

    @Override
    public String getType() {
        return "sendFile";
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public byte[] getFile() {
        return file;
    }

    public String getServerPath() {
        return serverPath;
    }

    public String getLocalPath() {
        return localPath;
    }
}

