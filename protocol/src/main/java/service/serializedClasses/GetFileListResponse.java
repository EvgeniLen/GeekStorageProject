package service.serializedClasses;

import java.util.List;

public class GetFileListResponse extends BasicResponse {
    private final String subDirection;
    private final List<FileInfo> files;



    public GetFileListResponse(String subDirection, List<FileInfo> files) {
        super("returnFileList");
        this.subDirection = subDirection;
        this.files = files;
    }

    public String getSubDirection() {
        return subDirection;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

}
