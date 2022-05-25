package service.serializedClasses;


import java.util.List;

public class SendDirectoriesRequest extends BasicAuth implements BasicRequest {
    private final List<String>  directories;
    private final long size;

    public SendDirectoriesRequest(String login, String password, List<String> directories, long size) {
        super(login, password);
        this.directories = directories;
        this.size = size;
    }

    @Override
    public String getType() {
        return "sendDirectories";
    }

    public List<String> getDirectories() {
        return directories;
    }

    public long getSize() {
        return size;
    }
}

