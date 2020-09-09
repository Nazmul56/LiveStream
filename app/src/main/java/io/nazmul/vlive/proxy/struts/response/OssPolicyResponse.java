package io.nazmul.vlive.proxy.struts.response;

public class OssPolicyResponse extends AbsResponse {
    public OssPolicyInfo data;

    public class OssPolicyInfo {
        public String accessKey;
        public String host;
        public String policy;
        public String signature;
        public long expire;
        public String callback;
        public String dir;
    }
}
