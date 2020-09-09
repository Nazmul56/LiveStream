package io.nazmul.vlive.agora.rtm.model;

public class PeerMessageData {
    public int cmd;
    public PeerMessage data;

    public PeerMessageData(int cmd, String account, String userId, int operate, int coindex) {
        this.cmd = cmd;
        data = new PeerMessage(account, userId, operate, coindex);
    }

    public PeerMessageData(int cmd, String account, String userId, int operate) {
        this.cmd = cmd;
        data = new PeerMessage(account, userId, operate, 0);
    }

    public PeerMessageData(int cmd, String account ,int operate, String roomId) {
        this.cmd = cmd;
        data = new PeerMessage(account, operate, roomId);
    }

    public static class PeerMessage {
        public String account;
        public String userId;
        public int operate;
        public int coindex;
        public String pkRoomId;

        public PeerMessage(String account, String userId, int operate, int coindex) {
            this.account = account;
            this.userId = userId;
            this.operate = operate;
            this.coindex = coindex;
        }

        public PeerMessage(String account, int operate, String pkRoomId) {
            this.account = account;
            this.operate = operate;
            this.pkRoomId = pkRoomId;
        }
    }
}
