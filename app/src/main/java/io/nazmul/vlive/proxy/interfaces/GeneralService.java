package io.nazmul.vlive.proxy.interfaces;

import io.nazmul.vlive.proxy.struts.response.AppVersionResponse;
import io.nazmul.vlive.proxy.struts.response.GiftListResponse;
import io.nazmul.vlive.proxy.struts.response.MusicListResponse;
import io.nazmul.vlive.proxy.struts.response.OssPolicyResponse;
import io.nazmul.vlive.proxy.struts.response.RefreshTokenResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface GeneralService {
    @GET("ent/v1/app/version")
    Call<AppVersionResponse> requestAppVersion(@Header("reqId") long reqId, @Header("reqType") int reqType,
                                               @Query("appCode") String appCode, @Query("osType") int osType,
                                               @Query("terminalType") int terminalType, @Query("appVersion") String appVersion);

    @GET("ent/v1/gifts")
    Call<GiftListResponse> requestGiftList(@Header("reqId") long reqId, @Header("reqType") int reqType);

    @GET("ent/v1/musics")
    Call<MusicListResponse> requestMusicList(@Header("reqId") long reqId, @Header("reqType") int reqType);

    @GET("ent/v1/room/{roomId}/token/refresh")
    Call<RefreshTokenResponse> requestRefreshToken(@Header("reqId") long reqId, @Header("reqType") int reqType,
                                                   @Header("token") String token, @Query("roomId") String roomId);

    @GET("ent/v1/file/policy")
    Call<OssPolicyResponse> requestOssPolicy(@Header("reqId") long reqId, @Header("reqType") int reqType,
                                             @Header("token") String token, @Query("type") int type);
}
