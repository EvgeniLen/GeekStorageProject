package service.serializedClasses;


import java.nio.file.Path;

public class SendFileRequest implements BasicRequest{
    private String login;
    private String password;
    //private FileInfo fileInfo;
    private byte[] file;

    private String path;

    public SendFileRequest(String login, String password, byte[] file, String path) {
        this.login = login;
        this.password = password;
        this.file = file;
        this.path = path;
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

    public String getPath() {
        return path;
    }

    @Override
    public String getType() {
        return "sendFile";
    }
}
