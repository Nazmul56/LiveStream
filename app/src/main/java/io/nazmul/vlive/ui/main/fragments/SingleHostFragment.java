package io.nazmul.vlive.ui.main.fragments;

import io.nazmul.vlive.proxy.ClientProxy;
import io.nazmul.vlive.ui.live.SingleHostLiveActivity;

public class SingleHostFragment extends AbsPageFragment {
    @Override
    protected int onGetRoomListType() {
        return ClientProxy.ROOM_TYPE_SINGLE;
    }

    @Override
    protected Class<?> getLiveActivityClass() {
        return SingleHostLiveActivity.class;
    }
}
