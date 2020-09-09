package io.nazmul.vlive.agora.rtm.model;

public class PKMessage extends AbsRtmMessage {
    public PKMessageData data;

    public static class PKMessageData {
        public int state;
        public int result;
        public String pkRoomId;
        public String pkChannelName;
        public long pkStartTime;
        public int pkRoomRank;
        public int hostRoomRank;
        public long countDown;
        public PkRoomOwner pkRoomOwner;
        public RelayConfig relayConfig;
    }

    public static class PkRoomOwner {
        public String userId;
        public int uid;
        public String userName;
    }

    public static class RelayConfig {
        public ChannelRelayInfo local;
        public ChannelRelayInfo remote;
        public ChannelRelayInfo proxy;
    }

    public static class ChannelRelayInfo {
        public String channelName;
        public int uid;
        public String token;
    }
}
