package service.serializedClasses;

import java.io.Serializable;

public interface BasicRequest extends Serializable{
    String getType();
    String getLogin();
    String getPassword();
}
