package io.nazmul.vlive.proxy.struts.request;

public class PKRequest extends Request {
    public String token;
    public String myRoomId;
    public String targetRoomId;

    public PKRequest(String token, String myRoomId, String targetRoomId) {
        this.token = token;
        this.myRoomId = myRoomId;
        this.targetRoomId = targetRoomId;
    }
}
