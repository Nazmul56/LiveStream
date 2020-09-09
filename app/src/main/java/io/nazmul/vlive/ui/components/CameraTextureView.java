package io.nazmul.vlive.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

import io.nazmul.framework.modules.consumers.TextureViewConsumer;

public class CameraTextureView extends TextureView {
    public CameraTextureView(Context context) {
        super(context);
        setTextureViewConsumer();
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTextureViewConsumer();
    }

    private void setTextureViewConsumer() {
        setSurfaceTextureListener(new TextureViewConsumer());
    }
}
