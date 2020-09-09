package io.nazmul.vlive.ui.main.fragments;

import io.nazmul.vlive.proxy.ClientProxy;
import io.nazmul.vlive.ui.live.MultiHostLiveActivity;

public class HostInFragment extends AbsPageFragment {
    @Override
    protected int onGetRoomListType() {
        return ClientProxy.ROOM_TYPE_HOST_IN;
    }

    @Override
    protected Class<?> getLiveActivityClass() {
        return MultiHostLiveActivity.class;
    }
}
