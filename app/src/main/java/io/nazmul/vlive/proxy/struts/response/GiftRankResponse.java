package io.nazmul.vlive.proxy.struts.response;

import java.util.List;

public class GiftRankResponse extends AbsResponse {
    public int total;
    public List<GiftInfo> data;

    public static class GiftInfo {
        public String userId;
        public String userName;
        public String avatar;
    }
}
