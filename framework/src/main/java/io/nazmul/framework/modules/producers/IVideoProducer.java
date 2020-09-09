package io.nazmul.framework.modules.producers;

import io.nazmul.capture.video.camera.VideoCaptureFrame;

public interface IVideoProducer {
    void connectChannel(int channelId);
    void pushVideoFrame(VideoCaptureFrame frame);
    void disconnect();
}
