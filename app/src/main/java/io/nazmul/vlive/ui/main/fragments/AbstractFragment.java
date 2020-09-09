package io.nazmul.vlive.ui.main.fragments;

import androidx.fragment.app.Fragment;

import io.nazmul.vlive.AgoraLiveApplication;
import io.nazmul.vlive.Config;
import io.nazmul.vlive.proxy.ClientProxyListener;
import io.nazmul.vlive.proxy.struts.response.AppVersionResponse;
import io.nazmul.vlive.proxy.struts.response.AudienceListResponse;
import io.nazmul.vlive.proxy.struts.response.CreateRoomResponse;
import io.nazmul.vlive.proxy.struts.response.CreateUserResponse;
import io.nazmul.vlive.proxy.struts.response.EditUserResponse;
import io.nazmul.vlive.proxy.struts.response.EnterRoomResponse;
import io.nazmul.vlive.proxy.struts.response.GiftListResponse;
import io.nazmul.vlive.proxy.struts.response.GiftRankResponse;
import io.nazmul.vlive.proxy.struts.response.LeaveRoomResponse;
import io.nazmul.vlive.proxy.struts.response.LoginResponse;
import io.nazmul.vlive.proxy.struts.response.ModifySeatStateResponse;
import io.nazmul.vlive.proxy.struts.response.ModifyUserStateResponse;
import io.nazmul.vlive.proxy.struts.response.MusicListResponse;
import io.nazmul.vlive.proxy.struts.response.OssPolicyResponse;
import io.nazmul.vlive.proxy.struts.response.RefreshTokenResponse;
import io.nazmul.vlive.proxy.struts.response.RoomListResponse;
import io.nazmul.vlive.proxy.struts.response.SeatStateResponse;
import io.nazmul.vlive.proxy.struts.response.SendGiftResponse;
import io.nazmul.vlive.proxy.struts.response.StartStopPkResponse;
import io.nazmul.vlive.ui.main.MainActivity;

public abstract class AbstractFragment extends Fragment implements ClientProxyListener {
    protected AgoraLiveApplication application() {
        return (AgoraLiveApplication) getContext().getApplicationContext();
    }

    MainActivity getContainer() {
        return (MainActivity) getActivity();
    }

    protected Config config() {
        return application().config();
    }

    @Override
    public void onAppVersionResponse(AppVersionResponse response) {

    }

    @Override
    public void onRefreshTokenResponse(RefreshTokenResponse refreshTokenResponse) {

    }

    @Override
    public void onOssPolicyResponse(OssPolicyResponse response) {

    }

    @Override
    public void onMusicLisResponse(MusicListResponse response) {

    }

    @Override
    public void onGiftListResponse(GiftListResponse response) {

    }

    @Override
    public void onRoomListResponse(RoomListResponse response) {

    }

    @Override
    public void onCreateUserResponse(CreateUserResponse response) {

    }

    @Override
    public void onLoginResponse(LoginResponse response) {

    }

    @Override
    public void onEditUserResponse(EditUserResponse response) {

    }

    @Override
    public void onCreateRoomResponse(CreateRoomResponse response) {

    }

    @Override
    public void onEnterRoomResponse(EnterRoomResponse response) {

    }

    @Override
    public void onLeaveRoomResponse(LeaveRoomResponse response) {

    }

    @Override
    public void onAudienceListResponse(AudienceListResponse response) {

    }

    @Override
    public void onRequestSeatStateResponse(SeatStateResponse response) {

    }

    @Override
    public void onModifyUserStateResponse(ModifyUserStateResponse response) {

    }

    @Override
    public void onModifySeatStateResponse(ModifySeatStateResponse response) {

    }

    @Override
    public void onSendGiftResponse(SendGiftResponse response) {

    }

    @Override
    public void onGiftRankResponse(GiftRankResponse response) {

    }

    @Override
    public void onStartStopPkResponse(StartStopPkResponse response) {

    }

    @Override
    public void onResponseError(int requestType, int error, String message) {

    }
}