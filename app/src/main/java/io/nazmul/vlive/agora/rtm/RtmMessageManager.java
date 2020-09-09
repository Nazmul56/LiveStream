package io.nazmul.vlive.agora.rtm;

import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.SendMessageOptions;
import io.nazmul.vlive.agora.rtm.model.ChatMessage;
import io.nazmul.vlive.agora.rtm.model.GiftMessage;
import io.nazmul.vlive.agora.rtm.model.GiftRankMessage;
import io.nazmul.vlive.agora.rtm.model.NotificationMessage;
import io.nazmul.vlive.agora.rtm.model.OwnerStateMessage;
import io.nazmul.vlive.agora.rtm.model.PKMessage;
import io.nazmul.vlive.agora.rtm.model.PeerMessageData;
import io.nazmul.vlive.agora.rtm.model.SeatStateMessage;

public class RtmMessageManager implements RtmClientListener, RtmChannelListener {
    private static final String TAG = RtmMessageManager.class.getSimpleName();

    private static final int PEER_MSG_TYPE_CALL = 1;
    private static final int PEER_MSG_TYPE_PK = 2;

    public static final int CHANNEL_MSG_TYPE_CHAT = 1;

    // Users enter or leave the room
    private static final int CHANNEL_MSG_TYPE_NOTIFY = 2;

    // Where the UI needs to show the user rank of gift values
    private static final int CHANNEL_MSG_TYPE_GIFT_RANK = 3;

    // Notifies that the room owner has changed his state
    private static final int CHANNEL_MSG_CMD_OWNER_STATE = 4;

    // Notifies that the seats' states have changed,
    // for multi-hosted rooms only
    private static final int CHANNEL_MSG_TYPE_SEAT = 5;

    // Notifies the PK states, for PK rooms only
    private static final int CHANNEL_MSG_TYPE_PK = 6;

    private static final int CHANNEL_MSG_TYPE_GIFT = 7;

    private static final int CHANNEL_MSG_TYPE_LEAVE = 8;

    private static final int PEER_MSG_CMD_APPLY = 101;
    private static final int PEER_MSG_CMD_INVITE = 102;
    private static final int PEER_MSG_CMD_APPLY_REJECT = 103;
    private static final int PEER_MSG_CMD_INVITE_REJECT = 104;
    private static final int PEER_MSG_CMD_APPLY_ACCEPTED = 105;
    private static final int PEER_MSG_CMD_INVITE_ACCEPTED = 106;

    private static final int PEER_MSG_CMD_PK = 201;
    private static final int PEER_MSG_CMD_PK_REJECT = 202;
    private static final int PEER_MSG_CMD_PK_ACCEPT = 203;

    private volatile static RtmMessageManager sInstance;

    private RtmClient mRtmClient;
    private RtmChannel mRtmChannel;
    private SendMessageOptions mOptions;
    private List<RtmMessageListener> mMessageListeners;
    private Handler mHandler;

    private RtmMessageManager() {
        mOptions = new SendMessageOptions();
        mOptions.enableOfflineMessaging = false;
        mOptions.enableHistoricalMessaging = false;
        mMessageListeners = new ArrayList<>();
    }

    public static RtmMessageManager instance() {
        if (sInstance == null) {
            synchronized (RtmMessageManager.class) {
                if (sInstance == null) {
                    sInstance = new RtmMessageManager();
                }
            }
        }
        return sInstance;
    }

    public void init(RtmClient client) {
        mRtmClient = client;
    }

    public synchronized void joinChannel(String channel, ResultCallback<Void> callback) {
        if (mRtmChannel != null || mRtmClient == null) {
            return;
        }

        mRtmChannel = mRtmClient.createChannel(channel, this);
        mRtmChannel.join(callback);
    }

    public synchronized void leaveChannel(ResultCallback<Void> callback) {
        if (mRtmChannel == null) return;
        mRtmChannel.leave(callback);
        mRtmChannel.release();
        mRtmChannel = null;
    }

    private void sendPeerMessage(String userId, String message, ResultCallback<Void> callback) {
        if (mRtmClient == null) return;
        RtmMessage msg = mRtmClient.createMessage(message);
        mRtmClient.sendMessageToPeer(userId, msg, mOptions, callback);
    }

    private void sendChannelMessage(String message, ResultCallback<Void> callback) {
        if (mRtmChannel == null) return;
        RtmMessage msg = mRtmClient.createMessage(message);
        mRtmChannel.sendMessage(msg, mOptions, callback);
    }

    public void apply(String peerId, String nickname, String userId, int coindex, ResultCallback<Void> callback) {
        String json = getPeerMessageDataJson(PEER_MSG_TYPE_CALL, nickname, userId, PEER_MSG_CMD_APPLY, coindex);
        sendPeerMessage(peerId, json, callback);
    }

    public void invite(String peerId, String nickname, String userId, int coindex, ResultCallback<Void> callback) {
        String json = getPeerMessageDataJson(PEER_MSG_TYPE_CALL, nickname, userId, PEER_MSG_CMD_INVITE, coindex);
        sendPeerMessage(peerId, json, callback);
    }

    public void acceptApplication(String peerId, String nickname, String userId, int coindex, ResultCallback<Void> callback) {
        String json = getPeerMessageDataJson(PEER_MSG_TYPE_CALL, nickname, userId, PEER_MSG_CMD_APPLY_ACCEPTED);
        sendPeerMessage(peerId, json, callback);
    }

    public void acceptInvitation(String peerId, String nickname, String userId, int coindex, ResultCallback<Void> callback) {
        String json = getPeerMessageDataJson(PEER_MSG_TYPE_CALL, nickname, userId, PEER_MSG_CMD_INVITE_ACCEPTED);
        sendPeerMessage(peerId, json, callback);
    }

    public void rejectApplication(String peerId, String nickname, String userId, ResultCallback<Void> callback) {
        String json = getPeerMessageDataJson(PEER_MSG_TYPE_CALL, nickname, userId, PEER_MSG_CMD_APPLY_REJECT);
        sendPeerMessage(peerId, json, callback);
    }

    public void rejectInvitation(String peerId, String nickname, String userId, ResultCallback<Void> callback) {
        String json = getPeerMessageDataJson(PEER_MSG_TYPE_CALL, nickname, userId, PEER_MSG_CMD_INVITE_REJECT);
        sendPeerMessage(peerId, json, callback);
    }

    public void applyPk(String peerId, String nickname, String roomId, ResultCallback<Void> callback) {
        String json = getPkPeerMessageDataJson(PEER_MSG_TYPE_PK, nickname, PEER_MSG_CMD_PK, roomId);
        sendPeerMessage(peerId, json, callback);
    }

    public void acceptPk(String peerId, String nickname, String userId, ResultCallback<Void> callback) {
        String json = getPeerMessageDataJson(PEER_MSG_TYPE_PK, nickname, userId, PEER_MSG_CMD_PK_ACCEPT);
        sendPeerMessage(peerId, json, callback);
    }

    public void rejectPk(String peerId, String nickname, String userId, ResultCallback<Void> callback) {
        String json = getPeerMessageDataJson(PEER_MSG_TYPE_PK, nickname, userId, PEER_MSG_CMD_PK_REJECT);
        sendPeerMessage(peerId, json, callback);
    }

    private String getPeerMessageDataJson(int cmd, String nickname, String userId, int operate, int coindex) {
        PeerMessageData data = new PeerMessageData(cmd, nickname, userId, operate, coindex);
        return new GsonBuilder().create().toJson(data);
    }

    private String getPkPeerMessageDataJson(int cmd, String nickname, int operate, String roomId) {
        PeerMessageData data = new PeerMessageData(cmd, nickname, operate, roomId);
        return new GsonBuilder().create().toJson(data);
    }

    private String getPeerMessageDataJson(int cmd, String nickname, String userId, int operate) {
        PeerMessageData data = new PeerMessageData(cmd, nickname, userId, operate, 0);
        return new GsonBuilder().create().toJson(data);
    }

    public void sendChatMessage(String userId, String nickname, String content, ResultCallback<Void> callback) {
        String json = getChatMessageJsonString(userId, nickname, content);
        sendChannelMessage(json, callback);
    }

    private String getChatMessageJsonString(String userId, String nickname, String content) {
        ChatMessage data = new ChatMessage(userId, nickname, content);
        return new GsonBuilder().create().toJson(data);
    }

    public void registerMessageHandler(RtmMessageListener handler) {
        mMessageListeners.add(handler);
    }

    public void removeMessageHandler(RtmMessageListener handler) {
        mMessageListeners.remove(handler);
    }

    public void setCallbackThread(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void onConnectionStateChanged(int state, int reason) {
        for (RtmMessageListener handler : mMessageListeners) {
            handler.onRtmConnectionStateChanged(state, reason);
        }
    }

    @Override
    public void onMessageReceived(RtmMessage rtmMessage, final String peerId) {
        // Where peer to peer messages are received.
        XLog.d("Peer message from " + peerId + " msg:" + rtmMessage.getText());

        PeerMessageData message = new GsonBuilder().create().
                fromJson(rtmMessage.getText(), PeerMessageData.class);

        if (mHandler != null) {
            mHandler.post(() -> handlePeerMessages(message, peerId,
                    message.data.account, message.data.userId, message.data.coindex));
        } else {
            handlePeerMessages(message, peerId,
                    message.data.account, message.data.userId, message.data.coindex);
        }
    }

    private void handlePeerMessages(PeerMessageData message, String peerId, String nickname, String userId, int index) {
        String name = TextUtils.isEmpty(nickname) ? peerId : nickname;
        for (RtmMessageListener listener : mMessageListeners) {
            switch (message.data.operate) {
                case PEER_MSG_CMD_APPLY:
                    listener.onRtmAppliedForSeat(peerId, name, userId, index);
                    break;
                case PEER_MSG_CMD_INVITE:
                    listener.onRtmInvitedByOwner(peerId, name, index);
                    break;
                case PEER_MSG_CMD_APPLY_ACCEPTED:
                    listener.onRtmApplicationAccepted(peerId, name, index);
                    break;
                case PEER_MSG_CMD_INVITE_ACCEPTED:
                    listener.onRtmInvitationAccepted(peerId, name, index);
                    break;
                case PEER_MSG_CMD_APPLY_REJECT:
                    listener.onRtmApplicationRejected(peerId, name);
                    break;
                case PEER_MSG_CMD_INVITE_REJECT:
                    listener.onRtmInvitationRejected(peerId, name);
                    break;
                case PEER_MSG_CMD_PK:
                    listener.onRtmPkReceivedFromAnotherHost(peerId, name, message.data.pkRoomId);
                    break;
                case PEER_MSG_CMD_PK_ACCEPT:
                    listener.onRtmPkAcceptedByTargetHost(peerId, name);
                    break;
                case PEER_MSG_CMD_PK_REJECT:
                    listener.onRtmPkRejectedByTargetHost(peerId, name);
                    break;
            }
        }
    }

    @Override
    public void onTokenExpired() {
        for (RtmMessageListener listener : mMessageListeners) {
            listener.onRtmTokenExpired();
        }
    }

    @Override
    public void onPeersOnlineStatusChanged(Map<String, Integer> map) {

    }

    @Override
    public void onMemberCountUpdated(int memberCount) {

    }

    @Override
    public void onAttributesUpdated(List<RtmChannelAttribute> attributeList) {
        for (RtmMessageListener listener : mMessageListeners) {
            listener.onRtmAttributesUpdated(attributeList);
        }
    }

    @Override
    public void onMessageReceived(RtmMessage rtmMessage, RtmChannelMember fromMember) {
        // Where channel messages are received
        boolean error = false;
        String json = rtmMessage.getText();

        XLog.d("Channel message: " + rtmMessage.getText());

        Gson gson = new Gson();
        int cmd = -1;
        try {
            JSONObject obj = new JSONObject(json);
            cmd = obj.getInt("cmd");

            for (final RtmMessageListener listener : mMessageListeners) {
                switch (cmd) {
                    case CHANNEL_MSG_TYPE_CHAT:
                        ChatMessage chatMessage = gson.fromJson(json, ChatMessage.class);
                        handleChatMessage(listener, chatMessage);
                        break;
                    case CHANNEL_MSG_TYPE_NOTIFY:
                        NotificationMessage notification = gson.fromJson(json, NotificationMessage.class);
                        handleNotificationMessage(listener, notification);
                        break;
                    case CHANNEL_MSG_TYPE_GIFT_RANK:
                        GiftRankMessage rankMessage = gson.fromJson(json, GiftRankMessage.class);
                        handleGiftRankMessage(listener, rankMessage);
                        break;
                    case CHANNEL_MSG_CMD_OWNER_STATE:
                        OwnerStateMessage ownerMessage = gson.fromJson(json, OwnerStateMessage.class);
                        handleOwnerStateMessage(listener, ownerMessage);
                        break;
                    case CHANNEL_MSG_TYPE_SEAT:
                        SeatStateMessage seat = gson.fromJson(json, SeatStateMessage.class);
                        handleSeatStateMessage(listener, seat);
                        break;
                    case CHANNEL_MSG_TYPE_PK:
                        PKMessage pkMessage = gson.fromJson(json, PKMessage.class);
                        handlePKMessage(listener, pkMessage);
                        break;
                    case CHANNEL_MSG_TYPE_GIFT:
                        GiftMessage giftMessage = gson.fromJson(json, GiftMessage.class);
                        handleGiftMessage(listener, giftMessage);
                        break;
                    case CHANNEL_MSG_TYPE_LEAVE:
                        handleLeaveMessage(listener);
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleChatMessage(@NonNull RtmMessageListener listener, ChatMessage message) {
        if (mHandler != null) {
            mHandler.post(() -> listener.onRtmChannelMessageReceived(
                    message.data.fromUserId, message.data.fromUserName, message.data.message));
        } else {
            listener.onRtmChannelMessageReceived(message.data.fromUserId, message.data.fromUserName, message.data.message);
        }
    }

    private void handleNotificationMessage(@NonNull RtmMessageListener listener, NotificationMessage message) {
        if (mHandler != null) {
            mHandler.post(() -> listener.onRtmChannelNotification(message.data.total, message.data.list));
        } else {
            listener.onRtmChannelNotification(message.data.total, message.data.list);
        }
    }

    private void handleGiftRankMessage(@NonNull RtmMessageListener listener, GiftRankMessage message) {
        if (mHandler != null) {
            mHandler.post(() -> listener.onRtmRoomGiftRankChanged(message.data.total, message.data.list));
        } else {
            listener.onRtmRoomGiftRankChanged(message.data.total, message.data.list);
        }
    }

    private void handleOwnerStateMessage(@NonNull RtmMessageListener listener, OwnerStateMessage message) {
        OwnerStateMessage.OwnerState data = message.data;
        if (mHandler != null) {
            mHandler.post(() -> listener.onRtmOwnerStateChanged(data.userId, data.userName, data.uid, data.enableAudio, data.enableVideo));
        } else {
            listener.onRtmOwnerStateChanged(data.userId, data.userName, data.uid, data.enableAudio, data.enableVideo);
        }
    }

    private void handleSeatStateMessage(@NonNull RtmMessageListener listener, SeatStateMessage message) {
        List<SeatStateMessage.SeatStateMessageDataItem> data = message.data;
        if (mHandler != null) {
            mHandler.post(() -> listener.onRtmSeatStateChanged(data));
        } else {
            listener.onRtmSeatStateChanged(data);
        }
    }

    private void handlePKMessage(@NonNull RtmMessageListener listener, PKMessage message) {
        PKMessage.PKMessageData data = message.data;
        if (mHandler != null) {
            mHandler.post(() -> listener.onRtmPkStateChanged(message.data));
        } else {
            listener.onRtmPkStateChanged(message.data);
        }
    }

    private void handleGiftMessage(@NonNull RtmMessageListener listener, GiftMessage message) {
        GiftMessage.GiftMessageData data = message.data;
        if (mHandler != null) {
            mHandler.post(() -> listener.onRtmGiftMessage(data.fromUserId, data.fromUserName, data.toUserId, data.toUserName, data.giftId));
        } else {
            listener.onRtmGiftMessage(data.fromUserId, data.fromUserName, data.toUserId, data.toUserName, data.giftId);
        }
    }

    private void handleLeaveMessage(@NonNull RtmMessageListener listener) {
        if (mHandler != null) {
            mHandler.post(listener::onRtmLeaveMessage);
        } else {
            listener.onRtmLeaveMessage();
        }
    }

    @Override
    public void onMemberJoined(RtmChannelMember rtmChannelMember) {

    }

    @Override
    public void onMemberLeft(RtmChannelMember rtmChannelMember) {

    }
}
