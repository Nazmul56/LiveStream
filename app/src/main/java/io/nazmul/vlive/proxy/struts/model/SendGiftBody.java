package io.nazmul.vlive.proxy.struts.model;

public class SendGiftBody {
    int giftId;
    int count;

    public SendGiftBody(int id, int count) {
        this.giftId = id;
        this.count = count;
    }
}
