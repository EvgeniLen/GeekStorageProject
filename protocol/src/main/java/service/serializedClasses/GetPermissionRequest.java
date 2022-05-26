package service.serializedClasses;


public class GetPermissionRequest extends BasicAuth implements BasicRequest {
    private final int depth;
    private final long size;


    public GetPermissionRequest(String login, String password, int depth, long size) {
        super(login, password);
        this.depth = depth;
        this.size = size;
    }

    @Override
    public String getType() {
        return "getPermission";
    }

    public int getDepth() {
        return depth;
    }
    public long getSize() {
        return size;
    }

}

