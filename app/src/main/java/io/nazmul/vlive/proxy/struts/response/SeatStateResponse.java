package io.nazmul.vlive.proxy.struts.response;

import java.util.List;

public class SeatStateResponse extends AbsResponse {
    public List<SeatInfo> data;

    public class SeatInfo {
        public int no;
        public String userId;
        public String userName;
        public int uid;
        public int state;
    }
}
