package service.serializedClasses;

import java.io.Serializable;

public interface BasicFileRequest extends Serializable, BasicRequest{
    String getServerPath();
    String getLocalPath();
}
