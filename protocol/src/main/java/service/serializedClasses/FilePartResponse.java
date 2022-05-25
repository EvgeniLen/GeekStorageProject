package service.serializedClasses;

public class FilePartResponse extends BasicResponse {

    private final boolean success;

    public FilePartResponse(boolean success) {
        super("uploadFileResult");
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
