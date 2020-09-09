package io.nazmul.vlive.ui.live;

import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;

import com.elvishew.xlog.XLog;

import io.agora.rtc.Constants;
import io.agora.rtc.video.ChannelMediaInfo;
import io.agora.rtc.video.ChannelMediaRelayConfiguration;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.nazmul.vlive.Config;
import io.agora.vlive.R;
import io.nazmul.vlive.agora.rtm.model.PKMessage;
import io.nazmul.vlive.proxy.struts.model.SeatInfo;
import io.nazmul.vlive.proxy.struts.request.PKRequest;
import io.nazmul.vlive.proxy.struts.request.Request;
import io.nazmul.vlive.proxy.struts.request.RoomRequest;
import io.nazmul.vlive.proxy.struts.response.EnterRoomResponse;
import io.nazmul.vlive.proxy.struts.response.Response;
import io.nazmul.vlive.proxy.struts.response.RoomListResponse;
import io.nazmul.vlive.proxy.struts.response.StartStopPkResponse;
import io.nazmul.vlive.ui.actionsheets.LiveRoomToolActionSheet;
import io.nazmul.vlive.ui.actionsheets.PkRoomListActionSheet;
import io.nazmul.vlive.ui.components.CameraTextureView;
import io.nazmul.vlive.ui.components.LiveBottomButtonLayout;
import io.nazmul.vlive.ui.components.LiveHostNameLayout;
import io.nazmul.vlive.ui.components.LiveMessageEditLayout;
import io.nazmul.vlive.ui.components.PkLayout;
import io.nazmul.vlive.utils.UserUtil;
import io.nazmul.vlive.ui.BaseActivity;

public class HostPKLiveActivity extends LiveRoomActivity
        implements View.OnClickListener, PkRoomListActionSheet.OnPkRoomSelectedListener {
    private static final String TAG = HostPKLiveActivity.class.getSimpleName();

    private static final int PK_RESULT_DISPLAY_LAST = 2000;

    private static final int PK_STATE_NOT_PK = 0;
    private static final int PK_STATE_START = 1;
    private static final int PK_STATE_IN_PK = 2;
    private static final int PK_STATE_STOP = 3;

    private RelativeLayout mLayout;
    private FrameLayout mVideoNormalLayout;
    private LiveHostNameLayout mNamePad;
    private PkRoomListActionSheet mPkRoomListActionSheet;
    private AppCompatImageView mStartPkButton;
    private PkLayout mPkLayout;
    private boolean mTopLayoutCalculated;

    private String pkRoomId;
    private boolean mPkStarted;
    private boolean mBroadcastStarted;

    // When the owner returns to his room and the room
    // is in pk mode before he left, the owner needs to
    // start pk mode. But he also needs to join rtc channel
    // first. This pending request records the case.
    private boolean mPendingStartPkRequest;

    private PKMessage.RelayConfig mPendingPkConfig;

    private int mMessageListHeightInNormalMode;

    private ResultCallback<Void> mMessageResultCallback = new ResultCallback<Void>() {
        @Override
        public void onSuccess(Void aVoid) {

        }

        @Override
        public void onFailure(ErrorInfo errorInfo) {
            showLongToast("Message error:" + errorInfo.getErrorDescription());
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBar(false);
    }

    @Override
    protected void onPermissionGranted() {
        initUI();
        super.onPermissionGranted();
    }

    private void initUI() {
        mMessageListHeightInNormalMode = getResources().
                getDimensionPixelOffset(R.dimen.live_message_list_height);

        setContentView(R.layout.activity_pk_host_in);

        mLayout = findViewById(R.id.live_room_pk_room_layout);
        mVideoNormalLayout = findViewById(R.id.live_pk_video_normal_layout);
        mNamePad = findViewById(R.id.pk_host_in_name_pad);
        mNamePad.init();

        participants = findViewById(R.id.pk_host_in_participant);
        participants.init();
        participants.setUserLayoutListener(this);

        messageList = findViewById(R.id.message_list);
        messageList.init();

        bottomButtons = findViewById(R.id.pk_host_in_bottom_layout);
        bottomButtons.init();
        bottomButtons.setLiveBottomButtonListener(this);
        bottomButtons.setRole(isOwner ? LiveBottomButtonLayout.ROLE_OWNER :
                isHost ? LiveBottomButtonLayout.ROLE_HOST :
                        LiveBottomButtonLayout.ROLE_AUDIENCE);

        findViewById(R.id.live_bottom_btn_close).setOnClickListener(this);
        findViewById(R.id.live_bottom_btn_more).setOnClickListener(this);
        findViewById(R.id.live_bottom_btn_fun1).setOnClickListener(this);
        findViewById(R.id.live_bottom_btn_fun2).setOnClickListener(this);

        mStartPkButton = findViewById(R.id.start_pk_button);
        mStartPkButton.setOnClickListener(this);

        messageEditLayout = findViewById(R.id.message_edit_layout);
        messageEditText = messageEditLayout.findViewById(LiveMessageEditLayout.EDIT_TEXT_ID);

        mPkLayout = findViewById(R.id.pk_host_layout);

        // At the initialization phase, the room is considered to
        // be in single-broadcast mode.
        // Whether the room is already in PK mode or not depends
        // on the information returned in the "enter room" response.
        setupUIMode(false, isOwner);
        setupSingleBroadcastBehavior(isOwner, !isOwner, !isOwner);

        // If I am the room owner, I will start single broadcasting
        // right now and do not need to start in "enter room" response
        if (isOwner) mBroadcastStarted = true;

        rtcStatsView = findViewById(R.id.host_pk_rtc_stats);
        rtcStatsView.setCloseListener(view -> rtcStatsView.setVisibility(View.GONE));
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        View topLayout = findViewById(R.id.pk_host_in_top_participant_layout);
        if (topLayout != null && !mTopLayoutCalculated) {
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) topLayout.getLayoutParams();
            params.topMargin += systemBarHeight;
            topLayout.setLayoutParams(params);
            mTopLayoutCalculated = true;
        }
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

            ownerId = response.data.room.owner.userId;
            ownerRtcUid = response.data.room.owner.uid;

            // Determine if I am the owner of a host here because
            // I may leave the room unexpectedly and come once more.
            String myId = config().getUserProfile().getUserId();
            if (!isOwner && myId.equals(response.data.room.owner.userId)) {
                isOwner = true;
            }

            // Result from server if the channel is in PK mode
            mPkStarted = response.data.room.pk.state == PK_STATE_IN_PK;
            if (mPkStarted) pkRoomId = response.data.room.pk.pkRoomId;

            runOnUiThread(() -> {
                mNamePad.setName(response.data.room.owner.userName);
                mNamePad.setIcon(UserUtil.getUserRoundIcon(getResources(),
                        response.data.room.owner.userId));

                participants.reset(response.data.room.currentUsers,
                        response.data.room.rankUsers);

                if (!mPkStarted) {
                    boolean audioMuted = config().isAudioMuted();
                    boolean videoMuted = config().isVideoMuted();

                    if (isOwner && !mBroadcastStarted) {
                        // I created this room and I left this room unexpectedly
                        // not long ago.
                        // This time I came from room list as an audience at first,
                        // but from the server response, I know that this is my room.
                        // I can start my broadcasting right now if not muted.
                        audioMuted = response.data.room.owner.enableAudio !=
                                SeatInfo.User.USER_AUDIO_ENABLE;
                        videoMuted = response.data.room.owner.enableVideo !=
                                SeatInfo.User.USER_VIDEO_ENABLE;
                    }

                    setupUIMode(false, isOwner);
                    setupSingleBroadcastBehavior(isOwner, audioMuted, videoMuted);
                    mBroadcastStarted = true;
                } else {
                    mBroadcastStarted = false;
                    mPendingStartPkRequest = true;
                    mPendingPkConfig = response.data.room.pk.relayConfig;
                    setupUIMode(true, isOwner);
                    setupPkBehavior(isOwner, response.data.room.pk.countDown,
                            response.data.room.pk.pkRoomOwner.userName,
                            response.data.room.pk.relayConfig,
                            response.data.room.pk.pkRoomOwner.uid);
                    updatePkGiftRank(response.data.room.pk.hostRoomRank,
                            response.data.room.pk.pkRoomRank);
                }

                joinRtcChannel();
                joinRtmChannel();
            });
        }
    }

    private void setupUIMode(boolean isPkMode, boolean isOwner) {
        if (isPkMode) {
            mLayout.setBackgroundResource(R.drawable.dark_background);
            mStartPkButton.setVisibility(View.GONE);
            mVideoNormalLayout.setVisibility(View.GONE);
            mPkLayout.setVisibility(View.VISIBLE);
            mPkLayout.setHost(isOwner);
        } else {
            mLayout.setBackground(null);
            mStartPkButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            mPkLayout.removeResult();
            mPkLayout.getLeftVideoLayout().removeAllViews();
            mPkLayout.getRightVideoLayout().removeAllViews();
            mPkLayout.setVisibility(View.GONE);
            mVideoNormalLayout.setVisibility(View.VISIBLE);
        }

        setupMessageListLayout(isPkMode);
        bottomButtons.setRole(isOwner ? LiveBottomButtonLayout.ROLE_OWNER
                : LiveBottomButtonLayout.ROLE_AUDIENCE);
        bottomButtons.setBeautyEnabled(config().isBeautyEnabled());
    }

    /**
     * Must be called after the desirable UI mode is already set up
     */
    private void setupPkBehavior(boolean isOwner, long remaining,
                                 String remoteName, PKMessage.RelayConfig config, int remoteUidForAudience) {
        myRtcRole = isOwner ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
        rtcEngine().setClientRole(myRtcRole);

        mPkLayout.setHost(isOwner);
        mPkLayout.setOtherHostName(remoteName);
        mPkLayout.startCountDownTimer(remaining);
        if (!isOwner) {
            mPkLayout.setOnClickGotoPeerChannelListener(view -> enterAnotherPkRoom(pkRoomId));
        }

        if (isOwner) {
            startCameraCapture();
            CameraTextureView cameraTextureView = new CameraTextureView(this);
            mPkLayout.getLeftVideoLayout().removeAllViews();
            mPkLayout.getLeftVideoLayout().addView(cameraTextureView);
            SurfaceView remoteSurfaceView = setupRemoteVideo(config.remote.uid);
            mPkLayout.getRightVideoLayout().removeAllViews();
            mPkLayout.getRightVideoLayout().addView(remoteSurfaceView);
            rtcEngine().muteLocalAudioStream(false);
            rtcEngine().muteLocalVideoStream(false);
            config().setAudioMuted(false);
            config().setVideoMuted(false);
        } else {
            SurfaceView surfaceView = setupRemoteVideo(ownerRtcUid);
            mPkLayout.getLeftVideoLayout().removeAllViews();
            mPkLayout.getLeftVideoLayout().addView(surfaceView);
            surfaceView.setZOrderMediaOverlay(true);
            SurfaceView remoteSurfaceView = setupRemoteVideo(remoteUidForAudience);
            mPkLayout.getRightVideoLayout().removeAllViews();
            mPkLayout.getRightVideoLayout().addView(remoteSurfaceView);
            remoteSurfaceView.setZOrderMediaOverlay(true);
        }
    }

    /**
     * Must be called after the desirable UI mode is already set up
     */
    private void setupSingleBroadcastBehavior(boolean isOwner, boolean audioMuted, boolean videoMuted) {
        myRtcRole = isOwner ? Constants.CLIENT_ROLE_BROADCASTER
                : Constants.CLIENT_ROLE_AUDIENCE;
        rtcEngine().setClientRole(myRtcRole);

        if (isOwner) {
            startCameraCapture();
            CameraTextureView cameraTextureView = new CameraTextureView(this);
            mVideoNormalLayout.addView(cameraTextureView);
        } else {
            SurfaceView surfaceView = setupRemoteVideo(ownerRtcUid);
            mVideoNormalLayout.removeAllViews();
            mVideoNormalLayout.addView(surfaceView);
        }

        config().setAudioMuted(audioMuted);
        config().setVideoMuted(videoMuted);
        rtcEngine().muteLocalAudioStream(audioMuted);
        rtcEngine().muteLocalVideoStream(videoMuted);
        bottomButtons.setRole(isOwner ? LiveBottomButtonLayout.ROLE_OWNER : LiveBottomButtonLayout.ROLE_AUDIENCE);
    }

    private void startMediaRelay(PKMessage.RelayConfig config) {
        ChannelMediaRelayConfiguration relayConfig = new ChannelMediaRelayConfiguration();
        relayConfig.setSrcChannelInfo(toChannelMediaInfo(config.local));
        relayConfig.setDestChannelInfo(config.proxy.channelName, toChannelMediaInfo(config.proxy));
        rtcEngine().startChannelMediaRelay(relayConfig);
    }

    private ChannelMediaInfo toChannelMediaInfo(PKMessage.ChannelRelayInfo proxy) {
        return new ChannelMediaInfo(proxy.channelName, proxy.token, proxy.uid);
    }

    @Override
    public void onRtcJoinChannelSuccess(String channel, int uid, int elapsed) {
        if (isOwner && mPendingStartPkRequest && mPendingPkConfig != null) {
            startMediaRelay(mPendingPkConfig);
            mPendingStartPkRequest = false;
        }
    }

    private void setupMessageListLayout(boolean isPkMode) {
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) messageList.getLayoutParams();
        if (isPkMode) {
            params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            params.addRule(RelativeLayout.BELOW, R.id.pk_host_layout);
        } else {
            params.height = mMessageListHeightInNormalMode;
            params.removeRule(RelativeLayout.BELOW);
        }
        messageList.setLayoutParams(params);
    }

    private void stopPkMode(boolean isOwner) {
        rtcEngine().stopChannelMediaRelay();
        setupUIMode(false, isOwner);
        setupSingleBroadcastBehavior(isOwner,
                config().isAudioMuted(),
                config().isVideoMuted());
    }

    private void enterAnotherPkRoom(String roomId) {
        rtcEngine().leaveChannel();
        leaveRtmChannel(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {

            }
        });

        sendRequest(Request.LEAVE_ROOM, new RoomRequest(
                config().getUserProfile().getToken(), this.roomId));

        enterRoom(roomId);
    }

    private void updatePkGiftRank(int mine, int other) {
        if (mPkStarted && mPkLayout.getVisibility() == View.VISIBLE) {
            mPkLayout.setPoints(mine, other);
        }
    }

    @Override
    public void onRtcChannelMediaRelayStateChanged(int state, int code) {
        if (state == Constants.RELAY_STATE_CONNECTING) {
            XLog.d("channel media relay is connecting");
        } else if (state == Constants.RELAY_STATE_RUNNING) {
            XLog.d("channel media relay is running");
        } else if (state == Constants.RELAY_STATE_FAILURE) {
            XLog.e("channel media relay fails");
        }
    }

    @Override
    public void onRtcChannelMediaRelayEvent(int code) {

    }

    @Override
    public void onRoomListResponse(RoomListResponse response) {
        super.onRoomListResponse(response);
        if (mPkRoomListActionSheet != null && mPkRoomListActionSheet.isShown()) {
            runOnUiThread(() -> mPkRoomListActionSheet.appendUsers(response.data));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.live_bottom_btn_close:
                onBackPressed();
                break;
            case R.id.live_bottom_btn_more:
                LiveRoomToolActionSheet toolSheet = (LiveRoomToolActionSheet) showActionSheetDialog(
                        BaseActivity.ACTION_SHEET_TOOL, tabIdToLiveType(tabId), isOwner, true, this);
                toolSheet.setEnableInEarMonitoring(inEarMonitorEnabled);
                break;
            case R.id.live_bottom_btn_fun1:
                if (isOwner) {
                    showActionSheetDialog(BaseActivity.ACTION_SHEET_BG_MUSIC, tabIdToLiveType(tabId), true, true, this);
                } else {
                    showActionSheetDialog(BaseActivity.ACTION_SHEET_GIFT, tabIdToLiveType(tabId), false, true, this);
                }
                break;
            case R.id.live_bottom_btn_fun2:
                // this button is hidden when current user is not host.
                if (isOwner) {
                    showActionSheetDialog(BaseActivity.ACTION_SHEET_BEAUTY, tabIdToLiveType(tabId), true, true, this);
                }
                break;
            case R.id.dialog_positive_button:
                closeDialog();
                finish();
                break;
            case R.id.start_pk_button:
                if (isOwner) {
                    mPkRoomListActionSheet = (PkRoomListActionSheet)
                            showActionSheetDialog(BaseActivity.ACTION_SHEET_PK_ROOM_LIST, tabIdToLiveType(tabId), true, true, this);
                    mPkRoomListActionSheet.setup(proxy(), config().getUserProfile().getToken());
                    mPkRoomListActionSheet.requestMorePkRoom();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        //if (isOwner && mPkStarted) {

        //} else {
            super.onBackPressed();
        //}
    }

    @Override
    public void finish() {
        super.finish();
        bottomButtons.clearStates(application());
    }

    @Override
    public void onPkRoomListActionSheetRoomSelected(int position, String roomId, int uid) {
        // The owner has selected another host, and send a
        // peer message to get the other host's permission.
        pkRoomId = roomId;
        Config.UserProfile profile = config().getUserProfile();
        // uid is used both in rtc and rtm engine.
        getMessageManager().applyPk(String.valueOf((long) uid), UserUtil.getUserText(
                profile.getUserId(), profile.getUserName()), this.roomId, mMessageResultCallback);
        dismissActionSheetDialog();
    }

    @Override
    public void onRtmPkReceivedFromAnotherHost(String peerId, String nickname, String pkRoomId) {
        // Received a pk request from another host,
        // here show a dialog to make a decision.
        String title = getResources().getString(R.string.live_room_pk_room_receive_pk_request_title);
        String messageFormat = getResources().getString(R.string.live_room_pk_room_receive_pk_request_message);
        String message = String.format(messageFormat, nickname);
        final Config.UserProfile profile = config().getUserProfile();

        runOnUiThread(() -> curDialog = showDialog(title, message,
                R.string.dialog_positive_button_accept, R.string.dialog_negative_button_refuse,
                view -> {
                    getMessageManager().acceptPk(peerId, UserUtil.getUserText(
                            profile.getUserId(), profile.getUserName()),
                            profile.getUserId(), mMessageResultCallback);

                    PKRequest request = new PKRequest(profile.getToken(), this.roomId, pkRoomId);
                    proxy().sendRequest(Request.PK_START_STOP, request);
                    closeDialog();
                },
                view -> {
                    getMessageManager().rejectPk(peerId, UserUtil.getUserText(
                            profile.getUserId(), profile.getUserName()),
                            profile.getUserId(), mMessageResultCallback);
                    closeDialog();
                }));
    }

    @Override
    public void onStartStopPkResponse(StartStopPkResponse response) {

    }

    @Override
    public void onRtmPkAcceptedByTargetHost(String peerId, String nickname) {
        runOnUiThread(() -> showShortToast(getResources().getString(R.string.live_room_pk_room_pk_invitation_accepted)));
    }

    @Override
    public void onRtmPkRejectedByTargetHost(String peerId, String nickname) {
        runOnUiThread(() -> showShortToast(getResources().getString(R.string.live_room_pk_room_pk_invitation_rejected)));
    }

    @Override
    public void onRtmPkStateChanged(PKMessage.PKMessageData messageData) {
        runOnUiThread(() -> {
            if (messageData.state == PK_STATE_START) {
                mPkStarted = true;
                pkRoomId = messageData.pkRoomId;
                setupUIMode(true, isOwner);
                setupPkBehavior(isOwner, messageData.countDown, messageData.pkRoomOwner.userName,
                        messageData.relayConfig, messageData.relayConfig.remote.uid);
                startMediaRelay(messageData.relayConfig);
                updatePkGiftRank(messageData.hostRoomRank, messageData.pkRoomRank);
            } else if (messageData.state == PK_STATE_STOP) {
                mPkLayout.setResult(messageData.result);
                new Handler(getMainLooper()).postDelayed(() -> stopPkMode(isOwner), PK_RESULT_DISPLAY_LAST);
                mPkStarted = false;
                showShortToast(getResources().getString(R.string.pk_ends));
            } else if (mPkStarted && messageData.state == PK_STATE_IN_PK) {
                updatePkGiftRank(messageData.hostRoomRank, messageData.pkRoomRank);
            }
        });
    }
}
