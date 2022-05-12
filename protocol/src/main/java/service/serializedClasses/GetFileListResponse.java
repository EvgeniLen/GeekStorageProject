package service.serializedClasses;

import java.util.List;
import java.util.Map;

public class GetFileListResponse extends BasicResponse {
    private String subDirection;
    private List<FileInfo> files;



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
