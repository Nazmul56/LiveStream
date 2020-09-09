package io.nazmul.vlive.proxy.interfaces;

import io.nazmul.vlive.proxy.struts.model.CreateRoomRequestBody;
import io.nazmul.vlive.proxy.struts.model.ModifySeatStateRequestBody;
import io.nazmul.vlive.proxy.struts.model.ModifyUserStateRequestBody;
import io.nazmul.vlive.proxy.struts.model.PkRoomBody;
import io.nazmul.vlive.proxy.struts.model.SendGiftBody;
import io.nazmul.vlive.proxy.struts.response.AudienceListResponse;
import io.nazmul.vlive.proxy.struts.response.CreateRoomResponse;
import io.nazmul.vlive.proxy.struts.response.EnterRoomResponse;
import io.nazmul.vlive.proxy.struts.response.GiftRankResponse;
import io.nazmul.vlive.proxy.struts.response.LeaveRoomResponse;
import io.nazmul.vlive.proxy.struts.response.ModifySeatStateResponse;
import io.nazmul.vlive.proxy.struts.response.ModifyUserStateResponse;
import io.nazmul.vlive.proxy.struts.response.SeatStateResponse;
import io.nazmul.vlive.proxy.struts.response.SendGiftResponse;
import io.nazmul.vlive.proxy.struts.response.StartStopPkResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LiveRoomService {
    @POST("ent/v1/room")
    Call<CreateRoomResponse> requestCreateLiveRoom(@Header("token") String token, @Header("reqId") long reqId,
                                                   @Header("reqType") int reqType, @Body CreateRoomRequestBody body);

    @POST("ent/v1/room/{roomId}/entry")
    Call<EnterRoomResponse> requestEnterLiveRoom(@Header("token") String token, @Header("reqId") long reqId,
                                                 @Header("reqType") int reqType, @Path("roomId") String roomId);

    @POST("ent/v1/room/{roomId}/exit")
    Call<LeaveRoomResponse> requestLeaveLiveRoom(@Header("token") String token, @Header("reqId") long reqId,
                                                 @Header("reqType") int reqType, @Path("roomId") String roomId);

    @GET("ent/v1/room/{roomId}/user/page")
    Call<AudienceListResponse> requestAudienceList(@Header("token") String token, @Header("reqId") long reqId,
                                                   @Header("reqType") int reqType, @Path("roomId") String roomId,
                                                   @Query("nextId") String nextId, @Query("count") int count,
                                                   @Query("type") int type);

    @GET("ent/v1/room/{roomId}/seats")
    Call<SeatStateResponse> requestSeatState(@Header("token") String token, @Header("reqId") long reqId,
                                             @Header("reqType") int reqType, @Path("roomId") String roomId);

    @POST("ent/v1/room/{roomId}/user/{userId}")
    Call<ModifyUserStateResponse> requestModifyUserState(@Header("token") String token, @Path("roomId") String roomId,
                                                         @Path("userId") String userId, @Body ModifyUserStateRequestBody body);

    @POST("ent/v1/room/{roomId}/seat")
    Call<ModifySeatStateResponse> requestModifySeatState(@Header("token") String token, @Header("reqId") long reqId,
                                                         @Header("reqType") int reqType, @Path("roomId") String roomId,
                                                         @Body ModifySeatStateRequestBody body);

    @POST("ent/v1/room/{roomId}/gift")
    Call<SendGiftResponse> requestSendGift(@Header("token") String token, @Header("reqId") long reqId,
                                           @Header("reqType") int reqType, @Path("roomId") String roomId,
                                           @Body SendGiftBody body);

    @GET("ent/v1/room/{roomId}/ranks")
    Call<GiftRankResponse> requestGiftRank(@Header("reqId") long reqId, @Header("reqType") int reqType,
                                           @Path("roomId") String roomId);

    @POST("ent/v1/room/{roomId}/pk")
    Call<StartStopPkResponse> requestStartStopPk(@Header("token") String token, @Header("reqId") long reqId, @Header("reqType") int reqType,
                                                 @Path("roomId") String myRoomId, @Body PkRoomBody body);
}
