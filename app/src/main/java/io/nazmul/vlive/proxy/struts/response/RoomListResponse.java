package io.nazmul.vlive.proxy.struts.response;

import java.util.List;

import io.nazmul.vlive.proxy.struts.model.RoomInfo;

public class RoomListResponse extends AbsResponse {
    public RoomList data;

    public static class RoomList {
        public int count;
        public int total;
        public String nextId;
        public List<RoomInfo> list;
    }
}
