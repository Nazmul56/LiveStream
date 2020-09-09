package io.nazmul.vlive.proxy.struts.request;

public class RoomRequest extends Request {
    public String token;
    public String roomId;

    public RoomRequest(String token, String roomId) {
        this.token = token;
        this.roomId = roomId;
    }

    public RoomRequest() {

    }
}
