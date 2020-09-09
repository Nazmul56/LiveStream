package io.nazmul.vlive.ui.live;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;

import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.List;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.nazmul.vlive.Config;
import io.agora.vlive.R;
import io.nazmul.vlive.agora.rtm.model.GiftRankMessage;
import io.nazmul.vlive.agora.rtm.model.NotificationMessage;
import io.nazmul.vlive.proxy.ClientProxy;
import io.nazmul.vlive.proxy.struts.model.UserProfile;
import io.nazmul.vlive.proxy.struts.request.CreateRoomRequest;
import io.nazmul.vlive.proxy.struts.request.Request;
import io.nazmul.vlive.proxy.struts.request.RoomRequest;
import io.nazmul.vlive.proxy.struts.request.SendGiftRequest;
import io.nazmul.vlive.proxy.struts.response.AudienceListResponse;
import io.nazmul.vlive.proxy.struts.response.CreateRoomResponse;
import io.nazmul.vlive.proxy.struts.response.EnterRoomResponse;
import io.nazmul.vlive.proxy.struts.response.Response;
import io.nazmul.vlive.ui.actionsheets.BackgroundMusicActionSheet;
import io.nazmul.vlive.ui.actionsheets.BeautySettingActionSheet;
import io.nazmul.vlive.ui.actionsheets.GiftActionSheet;
import io.nazmul.vlive.ui.actionsheets.LiveRoomUserListActionSheet;
import io.nazmul.vlive.ui.actionsheets.LiveRoomSettingActionSheet;
import io.nazmul.vlive.ui.actionsheets.LiveRoomToolActionSheet;
import io.nazmul.vlive.ui.actionsheets.VoiceActionSheet;
import io.nazmul.vlive.ui.components.GiftAnimWindow;
import io.nazmul.vlive.ui.components.LiveBottomButtonLayout;
import io.nazmul.vlive.ui.components.LiveMessageEditLayout;
import io.nazmul.vlive.ui.components.LiveRoomMessageList;
import io.nazmul.vlive.ui.components.LiveRoomUserLayout;
import io.nazmul.vlive.ui.components.RtcStatsView;
import io.nazmul.vlive.utils.GiftUtil;
import io.nazmul.vlive.utils.Global;

public abstract class LiveRoomActivity extends LiveBaseActivity implements
        BeautySettingActionSheet.BeautyActionSheetListener,
        LiveRoomSettingActionSheet.LiveRoomSettingActionSheetListener,
        BackgroundMusicActionSheet.BackgroundMusicActionSheetListener,
        GiftActionSheet.GiftActionSheetListener,
        LiveRoomToolActionSheet.LiveRoomToolActionSheetListener,
        VoiceActionSheet.VoiceActionSheetListener,
        LiveBottomButtonLayout.LiveBottomButtonListener,
        TextView.OnEditorActionListener,
        LiveRoomUserLayout.UserLayoutListener,
        LiveRoomUserListActionSheet.OnUserSelectedListener {

    private static final String TAG = LiveRoomActivity.class.getSimpleName();
    private static final int IDEAL_MIN_KEYBOARD_HEIGHT = 200;
    private static final int MIN_ONLINE_MUSIC_INTERVAL = 100;

    private Rect mDecorViewRect;
    private int mInputMethodHeight;

    // UI components of a live room
    protected LiveRoomUserLayout participants;
    protected LiveRoomMessageList messageList;
    protected LiveBottomButtonLayout bottomButtons;
    protected LiveMessageEditLayout messageEditLayout;
    protected AppCompatEditText messageEditText;
    protected RtcStatsView rtcStatsView;
    protected Dialog curDialog;

    protected InputMethodManager mInputMethodManager;

    private LiveRoomUserListActionSheet mRoomUserActionSheet;

    // Rtc Engine requires that the calls of startAudioMixing
    // should not be too frequent if online musics are played.
    // The interval is better not to be fewer than 100 ms.
    private volatile long mLastMusicPlayedTimeStamp;

    private boolean mActivityFinished;
    protected boolean inEarMonitorEnabled;
    private boolean mHeadsetWithMicrophonePlugged;

    private BroadcastReceiver mHeadPhoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AudioManager.ACTION_HEADSET_PLUG.equals(action)) {
                boolean plugged = intent.getIntExtra("state", -1) == 1;
                boolean hasMic = intent.getIntExtra("microphone", -1) == 1;
                mHeadsetWithMicrophonePlugged = plugged && hasMic;
                XLog.d("Wired headset is plugged：" + mHeadsetWithMicrophonePlugged);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().getViewTreeObserver()
                .addOnGlobalLayoutListener(this::detectKeyboardLayout);

        mInputMethodManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        IntentFilter headPhoneFilter = new IntentFilter();
        headPhoneFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadPhoneReceiver, headPhoneFilter);
    }

    @Override
    protected void onPermissionGranted() {
        if (getIntent().getBooleanExtra(Global.Constants.KEY_CREATE_ROOM, false)) {
            createRoom();
        } else {
            enterRoom(roomId);
        }
    }

    private void detectKeyboardLayout() {
        Rect rect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

        if (mDecorViewRect == null) {
            mDecorViewRect = rect;
        }

        int diff = mDecorViewRect.height() - rect.height();

        // The global layout listener may be invoked several
        // times when the activity is launched, we need to care
        // about the value of detected input method height to
        // filter out the cases that are not desirable.
        if (diff == mInputMethodHeight) {
            // The input method is still shown
            return;
        }

        if (diff > IDEAL_MIN_KEYBOARD_HEIGHT && mInputMethodHeight == 0) {
            mInputMethodHeight = diff;
            onInputMethodToggle(true, diff);
        } else if (mInputMethodHeight > 0) {
            onInputMethodToggle(false, mInputMethodHeight);
            mInputMethodHeight = 0;
        }
    }

    protected void onInputMethodToggle(boolean shown, int height) {
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) messageEditLayout.getLayoutParams();
        int change = shown ? height : -height;
        params.bottomMargin += change;
        messageEditLayout.setLayoutParams(params);

        if (shown) {
            messageEditText.requestFocus();
            messageEditText.setOnEditorActionListener(this);
        } else {
            messageEditLayout.setVisibility(View.GONE);
        }
    }

    private void createRoom() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.token = config().getUserProfile().getToken();
        request.type = getChannelTypeByTabId();
        request.roomName = roomName;
        int imageId = getIntent().getIntExtra(Global.Constants.KEY_VIRTUAL_IMAGE, -1);
        request.avatar = virtualImageIdToName(imageId);
        sendRequest(Request.CREATE_ROOM, request);
    }

    protected String virtualImageIdToName(int id) {
        switch (id) {
            case 0: return "dog";
            case 1: return "girl";
            default: return null;
        }
    }

    protected int virtualImageNameToId(String name) {
        if ("dog".equals(name)) {
            return 0;
        } else if ("girl".equals(name)) {
            return 1;
        } else {
            return -1;
        }
    }

    private int getChannelTypeByTabId() {
        switch (tabId) {
            case Config.LIVE_TYPE_MULTI_HOST:
                return ClientProxy.ROOM_TYPE_HOST_IN;
            case Config.LIVE_TYPE_PK_HOST:
                return ClientProxy.ROOM_TYPE_PK;
            case Config.LIVE_TYPE_SINGLE_HOST:
                return ClientProxy.ROOM_TYPE_SINGLE;
            case Config.LIVE_TYPE_VIRTUAL_HOST:
                return ClientProxy.ROOM_TYPE_VIRTUAL_HOST;
        }
        return -1;
    }

    @Override
    public void onCreateRoomResponse(CreateRoomResponse response) {
        roomId = response.data;
        enterRoom(roomId);
    }

    protected void enterRoom(String roomId) {
        RoomRequest request = new RoomRequest(config().getUserProfile().getToken(), roomId);
        sendRequest(Request.ENTER_ROOM, request);
    }

    @Override
    public void onEnterRoomResponse(EnterRoomResponse response) {
        if (response.code == Response.SUCCESS) {
            Config.UserProfile profile = config().getUserProfile();
            profile.setRtcToken(response.data.user.rtcToken);
            profile.setRtmToken(response.data.user.rtmToken);
            profile.setAgoraUid(response.data.user.uid);

            rtcChannelName = response.data.room.channelName;
            roomId = response.data.room.roomId;
            roomName = response.data.room.roomName;

            joinRtcChannel();
            joinRtmChannel();

            initUserCount(response.data.room.currentUsers,
                    response.data.room.rankUsers);
        }
    }

    private void initUserCount(final int total, final List<EnterRoomResponse.RankInfo> rankUsers) {
        runOnUiThread(() -> participants.reset(total, rankUsers));
    }

    @Override
    public void onActionSheetBeautyEnabled(boolean enabled) {
        bottomButtons.setBeautyEnabled(enabled);
        enablePreProcess(enabled);
    }

    @Override
    public void onActionSheetBlurSelected(float blur) {
        setBlurValue(blur);
    }

    @Override
    public void onActionSheetWhitenSelected(float whiten) {
        setWhitenValue(whiten);
    }

    @Override
    public void onActionSheetCheekSelected(float cheek) {
        setCheekValue(cheek);
    }

    @Override
    public void onActionSheetEyeEnlargeSelected(float eye) {
        setEyeValue(eye);
    }

    @Override
    public void onActionSheetResolutionSelected(int index) {
        config().setResolutionIndex(index);
        setVideoConfiguration();
    }

    @Override
    public void onActionSheetFrameRateSelected(int index) {
        config().setFrameRateIndex(index);
        setVideoConfiguration();
    }

    @Override
    public void onActionSheetBitrateSelected(int bitrate) {
        config().setVideoBitrate(bitrate);
        setVideoConfiguration();
    }

    @Override
    public void onActionSheetSettingBackPressed() {
        dismissActionSheetDialog();
    }

    @Override
    public void onActionSheetMusicSelected(int index, String name, String url) {
        long now = System.currentTimeMillis();
        if (now - mLastMusicPlayedTimeStamp > MIN_ONLINE_MUSIC_INTERVAL) {
            rtcEngine().startAudioMixing(url, false, false, -1);
            bottomButtons.setMusicPlaying(true);
            mLastMusicPlayedTimeStamp = now;
        }
    }

    @Override
    public void onActionSheetMusicStopped() {
        rtcEngine().stopAudioMixing();
        bottomButtons.setMusicPlaying(false);
    }

    @Override
    public void onActionSheetGiftSend(String name, int index, int value) {
        dismissActionSheetDialog();
        SendGiftRequest request = new SendGiftRequest(config().
                getUserProfile().getToken(), roomId, index);
        sendRequest(Request.SEND_GIFT, request);
    }

    /**
     *
     * @param monitor the ideal monitoring state to be checked
     * @return true if the current audio route is wired or wire-less
     * headset with microphone, the audio route can be enabled.
     * Returns true if the state is allowed to be changed.
     */
    @Override
    public boolean onActionSheetEarMonitoringClicked(boolean monitor) {
        if (monitor) {
            if (mHeadsetWithMicrophonePlugged) {
                rtcEngine().enableInEarMonitoring(true);
                inEarMonitorEnabled = true;
                return true;
            } else {
                showShortToast(getResources().getString(R.string.in_ear_monitoring_failed));
                // In ear monitor state does not change here.
                return false;
            }
        } else {
            rtcEngine().enableInEarMonitoring(false);
            // It is always allowed to disable the in-ear monitoring.
            inEarMonitorEnabled = false;
            return true;
        }
    }

    @Override
    public void onActionSheetRealDataClicked() {
        if (rtcStatsView != null) {
            runOnUiThread(() -> {
                int visibility = rtcStatsView.getVisibility();
                if (visibility == View.VISIBLE) {
                    rtcStatsView.setVisibility(View.GONE);
                } else if (visibility == View.GONE) {
                    rtcStatsView.setVisibility(View.VISIBLE);
                    rtcStatsView.setLocalStats(0, 0, 0, 0);
                }

                // Only clicking data button will dismiss
                // the action sheet dialog.
                dismissActionSheetDialog();
            });
        }
    }

    @Override
    public void onActionSheetSettingClicked() {
        showActionSheetDialog(ACTION_SHEET_VIDEO, tabIdToLiveType(tabId), isHost, false, this);
    }

    @Override
    public void onActionSheetRotateClicked() {
        switchCamera();
    }

    @Override
    public void onActionSheetVideoClicked(boolean muted) {
        if (isHost || isOwner) {
            rtcEngine().muteLocalVideoStream(muted);
            config().setVideoMuted(muted);
        }
    }

    @Override
    public void onActionSheetSpeakerClicked(boolean muted) {
        if (isHost || isOwner) {
            rtcEngine().muteLocalAudioStream(muted);
            config().setAudioMuted(muted);
        }
    }

    @Override
    public void onActionSheetAudioRouteSelected(int type) {

    }

    @Override
    public void onActionSheetAudioRouteEnabled(boolean enabled) {

    }

    @Override
    public void onActionSheetAudioBackPressed() {

        dismissActionSheetDialog();
    }

    @Override
    public void onLiveBottomLayoutShowMessageEditor() {
        if (messageEditLayout != null) {
            messageEditLayout.setVisibility(View.VISIBLE);
            messageEditText.requestFocus();
            mInputMethodManager.showSoftInput(messageEditText, 0);
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            Editable editable = messageEditText.getText();
            if (TextUtils.isEmpty(editable)) {
                showShortToast(getResources().getString(R.string.live_send_empty_message));
            } else {
                sendChatMessage(editable.toString());
                messageEditText.setText("");
            }

            mInputMethodManager.hideSoftInputFromWindow(messageEditText.getWindowToken(), 0);
            return true;
        }
        return false;
    }

    private void sendChatMessage(String content) {
        Config.UserProfile profile = config().getUserProfile();
        getMessageManager().sendChatMessage(profile.getUserId(),
                profile.getUserName(), content, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {

            }
        });
        messageList.addMessage(LiveRoomMessageList.MSG_TYPE_CHAT, profile.getUserName(), content);
    }

    protected boolean isCurDialogShowing() {
        return curDialog != null && curDialog.isShowing();
    }

    protected void closeDialog() {
        if (isCurDialogShowing()) {
            curDialog.dismiss();
        }
    }

    @Override
    public void onUserLayoutShowUserList(View view) {
        // Show all user info list
        mRoomUserActionSheet = (LiveRoomUserListActionSheet)
                showActionSheetDialog(ACTION_SHEET_ROOM_USER, tabIdToLiveType(tabId), isHost, true, this);
        mRoomUserActionSheet.setup(proxy(), this, roomId, config().getUserProfile().getToken());
        mRoomUserActionSheet.requestMoreAudience();
    }

    @Override
    public void onAudienceListResponse(AudienceListResponse response) {
        List<UserProfile> userList = new ArrayList<>();
        for (AudienceListResponse.AudienceInfo info : response.data.list) {
            UserProfile profile = new UserProfile();
            profile.setUserId(info.userId);
            profile.setUserName(info.userName);
            profile.setAvatar(info.avatar);
            userList.add(profile);
        }

        if (mRoomUserActionSheet != null && mRoomUserActionSheet.getVisibility() == View.VISIBLE) {
            runOnUiThread(() -> mRoomUserActionSheet.appendUsers(userList));
        }
    }

    @Override
    public void onActionSheetUserListItemSelected(String userId, String userName) {
        // Called when clicking an online user's name, and want to see the detail
    }

    @Override
    public void onRtmChannelMessageReceived(String peerId, String nickname, String content) {
        runOnUiThread(() -> messageList.addMessage(LiveRoomMessageList.MSG_TYPE_CHAT, nickname, content));
    }

    @Override
    public void onRtmRoomGiftRankChanged(int total, List<GiftRankMessage.GiftRankItem> list) {
        // The rank of user sending gifts has changed. The client
        // needs to update UI in this callback.
        if (list == null) return;

        List<EnterRoomResponse.RankInfo> rankList = new ArrayList<>();
        for (GiftRankMessage.GiftRankItem item : list) {
            EnterRoomResponse.RankInfo info = new EnterRoomResponse.RankInfo();
            info.userId = item.userId;
            info.userName = item.userName;
            info.avatar = item.avatar;
            rankList.add(info);
        }

        runOnUiThread(() -> participants.reset(rankList));
    }

    @Override
    public void onRtmGiftMessage(String fromUserId, String fromUserName, String toUserId, String toUserName, int giftId) {
        runOnUiThread(() -> {
            String from = TextUtils.isEmpty(fromUserName) ? fromUserId : fromUserName;
            String to = TextUtils.isEmpty(toUserName) ? toUserId : toUserName;
            messageList.addMessage(LiveRoomMessageList.MSG_TYPE_GIFT, from, to, giftId);

            GiftAnimWindow window = new GiftAnimWindow(LiveRoomActivity.this, R.style.gift_anim_window);
            window.setAnimResource(GiftUtil.getGiftAnimRes(giftId));
            window.show();
        });
    }

    @Override
    public void onRtmChannelNotification(int total, List<NotificationMessage.NotificationItem> list) {
        // User enter & leave notifications.
        runOnUiThread(() -> {
            // update room user count
            participants.reset(total);
            for (NotificationMessage.NotificationItem item : list) {
                messageList.addMessage(LiveRoomMessageList.MSG_TYPE_SYSTEM, item.userName, "", item.state);
            }
        });
    }

    @Override
    public void onRtmLeaveMessage() {
        runOnUiThread(this::leaveRoom);
    }

    @Override
    public void onStart() {
        super.onStart();
        if ((isOwner || isHost) && !config().isVideoMuted()) {
            startCameraCapture();
        }
    }

    @Override
    public void onRtcJoinChannelSuccess(String channel, int uid, int elapsed) {
        XLog.d("onRtcJoinChannelSuccess:" + channel + " uid:" + (uid & 0xFFFFFFFFL));
    }

    @Override
    public void onRtcRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
        XLog.d("onRtcRemoteVideoStateChanged: " + (uid & 0xFFFFFFFFL) +
                " state:" + state + " reason:" + reason);
    }

    @Override
    public void onRtcStats(IRtcEngineEventHandler.RtcStats stats) {
        runOnUiThread(() -> {
            if (rtcStatsView != null && rtcStatsView.getVisibility() == View.VISIBLE) {
                rtcStatsView.setLocalStats(stats.rxKBitRate,
                        stats.rxPacketLossRate, stats.txKBitRate,
                        stats.txPacketLossRate);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if ((isHost || isOwner) && !config().isVideoMuted()
                && !mActivityFinished) {
            // If now the app goes to background, stop the camera
            // capture if the host is displaying his video.
            stopCameraCapture();
        }
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    private void showExitDialog() {
        int titleRes;
        int messageRes;
        if (isHost || isOwner) {
            titleRes = R.string.finish_broadcast_title_owner;
            messageRes = R.string.finish_broadcast_message_owner;
        } else {
            titleRes = R.string.finish_broadcast_title_audience;
            messageRes = R.string.finish_broadcast_message_audience;
        }
        curDialog = showDialog(titleRes, messageRes, view -> leaveRoom());
    }

    private void leaveRoom() {
        leaveRoom(roomId);
        finish();
        closeDialog();
        dismissActionSheetDialog();
    }

    protected void leaveRoom(String roomId) {
        sendRequest(Request.LEAVE_ROOM, new RoomRequest(
                config().getUserProfile().getToken(), roomId));
    }

    @Override
    public void finish() {
        super.finish();
        mActivityFinished = true;
        stopCameraCapture();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mHeadPhoneReceiver);
    }

    @Override
    public void onResponseError(int requestType, int error, String message) {
        XLog.e("request:" + requestType + " error:" + error + " msg:" + message);
        runOnUiThread(() -> showLongToast("request type: "+
                Request.getRequestString(requestType) + " " + message));
    }
}
