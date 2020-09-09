package io.nazmul.vlive.agora.rtm;

import java.util.List;
import java.util.Map;

import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelMember;
import io.nazmul.vlive.agora.rtm.model.GiftRankMessage;
import io.nazmul.vlive.agora.rtm.model.NotificationMessage;
import io.nazmul.vlive.agora.rtm.model.PKMessage;
import io.nazmul.vlive.agora.rtm.model.SeatStateMessage;

public interface RtmMessageListener {
    void onRtmConnectionStateChanged(int state, int reason);

    void onRtmTokenExpired();

    void onRtmPeersOnlineStatusChanged(Map<String, Integer> map);

    void onRtmMemberCountUpdated(int memberCount);

    void onRtmAttributesUpdated(List<RtmChannelAttribute> attributeList);

    void onRtmMemberJoined(RtmChannelMember rtmChannelMember);

    void onRtmMemberLeft(RtmChannelMember rtmChannelMember);

    void onRtmInvitedByOwner(String peerId, String nickname, int index);

    void onRtmAppliedForSeat(String peerId, String nickname, String userId, int index);

    void onRtmInvitationAccepted(String peerId, String nickname, int index);

    void onRtmApplicationAccepted(String peerId, String nickname, int index);

    void onRtmInvitationRejected(String peerId, String nickname);

    void onRtmApplicationRejected(String peerId, String nickname);

    void onRtmPkReceivedFromAnotherHost(String peerId, String nickname, String roomId);

    void onRtmPkAcceptedByTargetHost(String peerId, String nickname);

    void onRtmPkRejectedByTargetHost(String peerId, String nickname);

    void onRtmChannelMessageReceived(String peerId, String nickname, String content);

    void onRtmChannelNotification(int total, List<NotificationMessage.NotificationItem> list);

    void onRtmRoomGiftRankChanged(int total, List<GiftRankMessage.GiftRankItem> list);

    void onRtmOwnerStateChanged(String userId, String userName, int uid, int enableAudio, int enableVideo);

    void onRtmSeatStateChanged(List<SeatStateMessage.SeatStateMessageDataItem> data);

    void onRtmPkStateChanged(PKMessage.PKMessageData messageData);

    void onRtmGiftMessage(String fromUserId, String fromUserName, String toUserId, String toUserName, int giftId);

    void onRtmLeaveMessage();
}
