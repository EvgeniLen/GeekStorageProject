package service.serializedClasses;

import java.io.Serializable;

public class BasicResponse implements Serializable {
    private final String response;

    public BasicResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }
}
