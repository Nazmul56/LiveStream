package io.nazmul.vlive.proxy;

import com.elvishew.xlog.XLog;
import com.huawei.multimedia.audiokit.BuildConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.nazmul.vlive.proxy.interfaces.GeneralService;
import io.nazmul.vlive.proxy.interfaces.LiveRoomService;
import io.nazmul.vlive.proxy.interfaces.RoomListService;
import io.nazmul.vlive.proxy.interfaces.UserService;
import io.nazmul.vlive.proxy.struts.model.CreateRoomRequestBody;
import io.nazmul.vlive.proxy.struts.model.CreateUserBody;
import io.nazmul.vlive.proxy.struts.model.LoginBody;
import io.nazmul.vlive.proxy.struts.model.ModifySeatStateRequestBody;
import io.nazmul.vlive.proxy.struts.model.ModifyUserStateRequestBody;
import io.nazmul.vlive.proxy.struts.model.PkRoomBody;
import io.nazmul.vlive.proxy.struts.model.SendGiftBody;
import io.nazmul.vlive.proxy.struts.model.UserRequestBody;
import io.nazmul.vlive.proxy.struts.request.Request;
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
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.internal.EverythingIsNonNull;

class Client {
    private static final String PRODUCT_URL = "https://api-solutions.sh.agoralab.co";
    private static final String MSG_NULL_RESPONSE = "Response content is null";
    private static final int MAX_RESPONSE_THREAD = 10;
    private static final int DEFAULT_TIMEOUT_IN_SECONDS = 30;

    private static final int ERROR_OK = 0;
    private static final int ERROR_CONNECTION = -1;
    private static final int ERROR_NULL = -2;

    private GeneralService mGeneralService;
    private RoomListService mRoomListService;
    private LiveRoomService mLiveRoomService;
    private UserService mUserService;

    private List<ClientProxyListener> mProxyListeners = new ArrayList<>();

    Client() {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .build();

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(PRODUCT_URL)
                .client(okHttpClient)
                .callbackExecutor(Executors.newFixedThreadPool(MAX_RESPONSE_THREAD))
                .addConverterFactory(GsonConverterFactory.create());

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(XLog::d);
            interceptor.level(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
            builder.client(client);
        }

        Retrofit retrofit = builder.build();
        mGeneralService = retrofit.create(GeneralService.class);
        mRoomListService = retrofit.create(RoomListService.class);
        mLiveRoomService = retrofit.create(LiveRoomService.class);
        mUserService = retrofit.create(UserService.class);
    }

    void registerProxyListener(ClientProxyListener listener) {
        if (!mProxyListeners.contains(listener)) {
            mProxyListeners.add(listener);
        }
    }

    void removeProxyListener(ClientProxyListener listener) {
        mProxyListeners.remove(listener);
    }

    void requestVersion(long reqId, String appCode, int osType, int terminalType, String appVersion) {
        mGeneralService.requestAppVersion(reqId, Request.APP_VERSION,
                appCode, osType, terminalType, appVersion).enqueue(new Callback<AppVersionResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<AppVersionResponse> call, Response<AppVersionResponse> response) {
                AppVersionResponse appVersionResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (appVersionResponse == null) {
                        try {
                            listener.onResponseError(Request.APP_VERSION, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (appVersionResponse.code == ERROR_OK) {
                        listener.onAppVersionResponse(appVersionResponse);
                    } else {
                        listener.onResponseError(Request.APP_VERSION, appVersionResponse.code, appVersionResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<AppVersionResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.APP_VERSION, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void requestGiftList(long reqId) {
        mGeneralService.requestGiftList(reqId, Request.GIFT_LIST).enqueue(new Callback<GiftListResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<GiftListResponse> call, Response<GiftListResponse> response) {
                GiftListResponse giftListResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (giftListResponse == null) {
                        try {
                            listener.onResponseError(Request.GIFT_LIST, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (giftListResponse.code == ERROR_OK) {
                        listener.onGiftListResponse(giftListResponse);
                    } else {
                        listener.onResponseError(Request.GIFT_LIST, giftListResponse.code, giftListResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<GiftListResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.GIFT_LIST, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void requestMusicList(long reqId) {
        mGeneralService.requestMusicList(reqId, Request.MUSIC_LIST).enqueue(new Callback<MusicListResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<MusicListResponse> call, Response<MusicListResponse> response) {
                MusicListResponse musicListResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (musicListResponse == null) {
                        try {
                            listener.onResponseError(Request.MUSIC_LIST, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (musicListResponse.code == ERROR_OK) {
                        listener.onMusicLisResponse(musicListResponse);
                    } else {
                        listener.onResponseError(Request.MUSIC_LIST, musicListResponse.code, musicListResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<MusicListResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.MUSIC_LIST, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void requestOssPolicy(long reqId, String token, int type) {
        mGeneralService.requestOssPolicy(reqId, Request.OSS, token, type).enqueue(new Callback<OssPolicyResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<OssPolicyResponse> call, Response<OssPolicyResponse> response) {
                OssPolicyResponse ossPolicyResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (ossPolicyResponse == null) {
                        try {
                            listener.onResponseError(Request.OSS, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (ossPolicyResponse.code == ERROR_OK) {
                        listener.onOssPolicyResponse(ossPolicyResponse);
                    } else {
                        listener.onResponseError(Request.OSS, ossPolicyResponse.code, ossPolicyResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<OssPolicyResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.OSS, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void createUser(long reqId, String userName) {
        mUserService.requestCreateUser(reqId, Request.CREATE_USER,
                new CreateUserBody(userName)).enqueue(new Callback<CreateUserResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<CreateUserResponse> call, Response<CreateUserResponse> response) {
                CreateUserResponse createUserResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (createUserResponse == null) {
                        try {
                            listener.onResponseError(Request.CREATE_USER, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (createUserResponse.code == ERROR_OK) {
                        listener.onCreateUserResponse(createUserResponse);
                    } else {
                        listener.onResponseError(Request.CREATE_USER, createUserResponse.code, createUserResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<CreateUserResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.CREATE_USER, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void editUser(long reqId, String token, String userId, String userName, String avatar) {
        mUserService.requestEditUser(token, reqId, Request.EDIT_USER, userId,
                new UserRequestBody(userName, avatar)).enqueue(new Callback<EditUserResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<EditUserResponse> call, Response<EditUserResponse> response) {
                EditUserResponse editUserResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (editUserResponse == null) {
                        try {
                            listener.onResponseError(Request.EDIT_USER, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (editUserResponse.code == ERROR_OK) {
                        listener.onEditUserResponse(editUserResponse);
                    } else {
                        listener.onResponseError(Request.EDIT_USER, editUserResponse.code, editUserResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<EditUserResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.EDIT_USER, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void login(long reqId, String userId) {
        mUserService.requestLogin(reqId, Request.USER_LOGIN, new LoginBody(userId)).enqueue(new Callback<LoginResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                LoginResponse loginResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (loginResponse == null) {
                        try {
                            listener.onResponseError(Request.USER_LOGIN, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (loginResponse.code == ERROR_OK) {
                        listener.onLoginResponse(loginResponse);
                    } else {
                        listener.onResponseError(Request.USER_LOGIN, loginResponse.code, loginResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.USER_LOGIN, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void requestRoomList(long reqId, String token, String nextId, int count, int type, Integer pkState) {
        mRoomListService.requestRoomList(reqId, token, Request.ROOM_LIST, nextId, count, type, pkState).enqueue(new Callback<RoomListResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<RoomListResponse> call, Response<RoomListResponse> response) {
                RoomListResponse roomListResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (roomListResponse == null) {
                        try {
                            listener.onResponseError(Request.ROOM_LIST, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (roomListResponse.code == ERROR_OK) {
                        listener.onRoomListResponse(roomListResponse);
                    } else {
                        listener.onResponseError(Request.ROOM_LIST, roomListResponse.code, roomListResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<RoomListResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.ROOM_LIST, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void createRoom(long reqId, String token, String roomName, int type, String avatar) {
        mLiveRoomService.requestCreateLiveRoom(token, reqId, Request.CREATE_ROOM,
                new CreateRoomRequestBody(roomName, type, avatar)).enqueue(new Callback<CreateRoomResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<CreateRoomResponse> call, Response<CreateRoomResponse> response) {
                CreateRoomResponse createRoomResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (createRoomResponse == null) {
                        try {
                            listener.onResponseError(Request.CREATE_ROOM, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (createRoomResponse.code == ERROR_OK) {
                        listener.onCreateRoomResponse(createRoomResponse);
                    } else {
                        listener.onResponseError(Request.CREATE_ROOM, createRoomResponse.code, createRoomResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<CreateRoomResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.CREATE_ROOM, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void enterRoom(long reqId, String token, String roomId) {
        mLiveRoomService.requestEnterLiveRoom(token, reqId, Request.ENTER_ROOM, roomId).enqueue(new Callback<EnterRoomResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<EnterRoomResponse> call, Response<EnterRoomResponse> response) {
                EnterRoomResponse enterRoomResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (enterRoomResponse == null) {
                        try {
                            listener.onResponseError(Request.ENTER_ROOM, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (enterRoomResponse.code == ERROR_OK) {
                        listener.onEnterRoomResponse(enterRoomResponse);
                    } else {
                        listener.onResponseError(Request.ENTER_ROOM, enterRoomResponse.code, enterRoomResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<EnterRoomResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.ENTER_ROOM, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void leaveRoom(long reqId, String token, String roomId) {
        mLiveRoomService.requestLeaveLiveRoom(token, reqId, Request.LEAVE_ROOM, roomId).enqueue(new Callback<LeaveRoomResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<LeaveRoomResponse> call, Response<LeaveRoomResponse> response) {
                LeaveRoomResponse leaveRoomResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (leaveRoomResponse == null) {
                        try {
                            listener.onResponseError(Request.LEAVE_ROOM, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (leaveRoomResponse.code == ERROR_OK) {
                        listener.onLeaveRoomResponse(leaveRoomResponse);
                    } else {
                        listener.onResponseError(Request.LEAVE_ROOM, leaveRoomResponse.code, leaveRoomResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<LeaveRoomResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.LEAVE_ROOM, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void requestAudienceList(long reqId, String token, String roomId, String nextId, int count ,int type) {
        mLiveRoomService.requestAudienceList(token, reqId, Request.AUDIENCE_LIST,
                roomId, nextId, count, type).enqueue(new Callback<AudienceListResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<AudienceListResponse> call, Response<AudienceListResponse> response) {
                AudienceListResponse audienceListResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (audienceListResponse == null) {
                        try {
                            listener.onResponseError(Request.AUDIENCE_LIST, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (audienceListResponse.code == ERROR_OK) {
                        listener.onAudienceListResponse(audienceListResponse);
                    } else {
                        listener.onResponseError(Request.AUDIENCE_LIST, audienceListResponse.code, audienceListResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<AudienceListResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.AUDIENCE_LIST, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void requestSeatState(long reqId, String token, String roomId) {
        mLiveRoomService.requestSeatState(token, reqId,
                Request.SEAT_STATE, roomId).enqueue(new Callback<SeatStateResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<SeatStateResponse> call, Response<SeatStateResponse> response) {
                SeatStateResponse seatStateResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (seatStateResponse == null) {
                        try {
                            listener.onResponseError(Request.SEAT_STATE, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (seatStateResponse.code == ERROR_OK) {
                        listener.onRequestSeatStateResponse(seatStateResponse);
                    } else {
                        listener.onResponseError(Request.SEAT_STATE, seatStateResponse.code, seatStateResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<SeatStateResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.SEAT_STATE, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void modifyUserState(String token, String roomId, String userId, int enableAudio, int enableVideo, int enableChat) {
        mLiveRoomService.requestModifyUserState(token, roomId, userId,
                new ModifyUserStateRequestBody(enableAudio, enableVideo, enableChat)).enqueue(new Callback<ModifyUserStateResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<ModifyUserStateResponse> call, Response<ModifyUserStateResponse> response) {
                ModifyUserStateResponse modifyUserStateResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (modifyUserStateResponse == null) {
                        listener.onResponseError(Request.MODIFY_SEAT_STATE, ERROR_NULL,
                                response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().toString());
                    } else if (modifyUserStateResponse.code == ERROR_OK) {
                        listener.onModifyUserStateResponse(modifyUserStateResponse);
                    } else {
                        listener.onResponseError(Request.MODIFY_SEAT_STATE, modifyUserStateResponse.code, modifyUserStateResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<ModifyUserStateResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.MODIFY_SEAT_STATE, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void modifySeatState(long reqId, String token, String roomId, int no, String userId, int state, String avatar) {
        ModifySeatStateRequestBody body = new ModifySeatStateRequestBody(no, userId, state, avatar);
        mLiveRoomService.requestModifySeatState(token, reqId,
                Request.MODIFY_SEAT_STATE, roomId, body).enqueue(new Callback<ModifySeatStateResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<ModifySeatStateResponse> call, Response<ModifySeatStateResponse> response) {
                ModifySeatStateResponse modifySeatStateResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (modifySeatStateResponse == null) {
                        listener.onResponseError(Request.MODIFY_SEAT_STATE, ERROR_NULL,
                                response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().toString());
                    } else if (modifySeatStateResponse.code == ERROR_OK) {
                        listener.onModifySeatStateResponse(modifySeatStateResponse);
                    } else {
                        listener.onResponseError(Request.MODIFY_SEAT_STATE, modifySeatStateResponse.code, modifySeatStateResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<ModifySeatStateResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.MODIFY_SEAT_STATE, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void sendGift(long reqId, String token, String roomId, int giftId, int count) {
        mLiveRoomService.requestSendGift(token, reqId, Request.SEND_GIFT,
                roomId, new SendGiftBody(giftId, count)).enqueue(new Callback<SendGiftResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<SendGiftResponse> call, Response<SendGiftResponse> response) {
                SendGiftResponse sendGiftResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (sendGiftResponse == null) {
                        listener.onResponseError(Request.SEND_GIFT, ERROR_NULL,
                                response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().toString());
                    } else if (sendGiftResponse.code == ERROR_OK) {
                        listener.onSendGiftResponse(sendGiftResponse);
                    } else {
                        listener.onResponseError(Request.SEND_GIFT, sendGiftResponse.code, sendGiftResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<SendGiftResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.SEND_GIFT, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void giftRank(long reqId, String roomId) {
        mLiveRoomService.requestGiftRank(reqId, Request.GIFT_RANK,
                roomId).enqueue(new Callback<GiftRankResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<GiftRankResponse> call, Response<GiftRankResponse> response) {
                GiftRankResponse giftRankResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (giftRankResponse == null) {
                        listener.onResponseError(Request.GIFT_RANK, ERROR_NULL,
                                response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().toString());
                    } else if (giftRankResponse.code == ERROR_OK) {
                        listener.onGiftRankResponse(giftRankResponse);
                    } else {
                        listener.onResponseError(Request.GIFT_RANK, giftRankResponse.code, giftRankResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<GiftRankResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.GIFT_RANK, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void refreshToken(long reqId, String token, String roomId) {
        mGeneralService.requestRefreshToken(reqId, Request.REFRESH_TOKEN,
                token, roomId).enqueue(new Callback<RefreshTokenResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<RefreshTokenResponse> call, Response<RefreshTokenResponse> response) {
                RefreshTokenResponse refreshTokenResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (refreshTokenResponse == null) {
                        listener.onResponseError(Request.REFRESH_TOKEN, ERROR_NULL,
                                response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().toString());
                    } else if (refreshTokenResponse.code == ERROR_OK) {
                        listener.onRefreshTokenResponse(refreshTokenResponse);
                    } else {
                        listener.onResponseError(Request.REFRESH_TOKEN, refreshTokenResponse.code, refreshTokenResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<RefreshTokenResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.REFRESH_TOKEN, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }

    void startStopPk(long reqId, String token, String myRoomId, String targetRoomId) {
        mLiveRoomService.requestStartStopPk(token, reqId, Request.PK_START_STOP,
                myRoomId, new PkRoomBody(targetRoomId)).enqueue(new Callback<StartStopPkResponse>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<StartStopPkResponse> call, Response<StartStopPkResponse> response) {
                StartStopPkResponse startStopPkResponse = response.body();
                for (ClientProxyListener listener : mProxyListeners) {
                    if (startStopPkResponse == null) {
                        try {
                            listener.onResponseError(Request.PK_START_STOP, ERROR_NULL,
                                    response.errorBody() == null ? MSG_NULL_RESPONSE : response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (startStopPkResponse.code == ERROR_OK) {
                        listener.onStartStopPkResponse(startStopPkResponse);
                    } else {
                        listener.onResponseError(Request.PK_START_STOP, startStopPkResponse.code, startStopPkResponse.msg);
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<StartStopPkResponse> call, Throwable t) {
                for (ClientProxyListener listener : mProxyListeners) {
                    listener.onResponseError(Request.PK_START_STOP, ERROR_CONNECTION, t.getMessage());
                }
            }
        });
    }
}
