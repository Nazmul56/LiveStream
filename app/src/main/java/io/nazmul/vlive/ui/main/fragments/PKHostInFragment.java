package io.nazmul.vlive.ui.main.fragments;

import io.nazmul.vlive.proxy.ClientProxy;
import io.nazmul.vlive.ui.live.HostPKLiveActivity;

public class PKHostInFragment extends AbsPageFragment {
    @Override
    protected int onGetRoomListType() {
        return ClientProxy.ROOM_TYPE_PK;
    }

    @Override
    protected Class<?> getLiveActivityClass() {
        return HostPKLiveActivity.class;
    }
}
