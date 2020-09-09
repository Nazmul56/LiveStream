package io.nazmul.vlive.ui.main.fragments;

import io.nazmul.vlive.proxy.ClientProxy;
import io.nazmul.vlive.ui.live.VirtualHostLiveActivity;

public class VirtualHostFragment extends AbsPageFragment {
    @Override
    protected int onGetRoomListType() {
        return ClientProxy.ROOM_TYPE_VIRTUAL_HOST;
    }

    @Override
    protected Class<?> getLiveActivityClass() {
        return VirtualHostLiveActivity.class;
    }
}
