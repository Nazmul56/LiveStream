package io.nazmul.framework.modules.processors;

import io.nazmul.capture.video.camera.VideoCaptureFrame;
import io.nazmul.framework.modules.channels.VideoChannel;

public interface IPreprocessor {
    VideoCaptureFrame onPreProcessFrame(VideoCaptureFrame outFrame, VideoChannel.ChannelContext context);

    void initPreprocessor();

    void enablePreProcess(boolean enabled);

    void releasePreprocessor(VideoChannel.ChannelContext context);

    void setBlurValue(float blur);

    void setWhitenValue(float whiten);

    void setCheekValue(float cheek);

    void setEyeValue(float eye);
}
