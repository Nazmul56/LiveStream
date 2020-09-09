package io.nazmul.vlive.proxy;

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

public interface ClientProxyListener {
    void onAppVersionResponse(AppVersionResponse response);

    void onRefreshTokenResponse(RefreshTokenResponse refreshTokenResponse);

    void onOssPolicyResponse(OssPolicyResponse response);

    void onMusicLisResponse(MusicListResponse response);

    void onGiftListResponse(GiftListResponse response);

    void onRoomListResponse(RoomListResponse response);

    void onCreateUserResponse(CreateUserResponse response);

    void onEditUserResponse(EditUserResponse response);

    void onLoginResponse(LoginResponse response);

    void onCreateRoomResponse(CreateRoomResponse response);

    void onEnterRoomResponse(EnterRoomResponse response);

    void onLeaveRoomResponse(LeaveRoomResponse response);

    void onAudienceListResponse(AudienceListResponse response);

    void onRequestSeatStateResponse(SeatStateResponse response);

    void onModifyUserStateResponse(ModifyUserStateResponse response);

    void onModifySeatStateResponse(ModifySeatStateResponse response);

    void onSendGiftResponse(SendGiftResponse response);

    void onGiftRankResponse(GiftRankResponse response);

    void onStartStopPkResponse(StartStopPkResponse response);

    void onResponseError(int requestType, int error, String message);
}