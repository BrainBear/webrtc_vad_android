package com.brainbear.webrtc_vad_android;

import android.support.annotation.IntDef;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * created by canxionglian on 2019-07-28
 */
public class VADHandler {

    private static final String TAG = "VADHandler";

    public native static int native_create(int mode);

    public native static int native_process(int pointer, int fs, short data[], int len);

    public native static int native_release(int pointer);


    private final Builder mBuilder;

    public static class Builder {
        @SampleRate
        private int sampleRate;
        @FrameSize
        private int frameSize;
        private long eos;
        private long bos;
        @AggressivenessMode
        private int aggressivenessMode;
        @ChannelConfig
        private int channel = CHANNEL_MONO;
        @BitWidth
        private int pcmBitWidth = PCM_16BIT;


        public VADHandler build() throws Exception {
            return new VADHandler(this);
        }

        @SampleRate
        public int getSampleRate() {
            return sampleRate;
        }

        public Builder setSampleRate(@SampleRate int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        @FrameSize
        public int getFrameSize() {
            return frameSize;
        }

        public Builder setFrameSize(@FrameSize int frameSize) {
            this.frameSize = frameSize;
            return this;
        }

        public long getEos() {
            return eos;
        }

        public Builder setEos(long eos) {
            this.eos = eos;
            return this;
        }

        public long getBos() {
            return bos;
        }

        public Builder setBos(long bos) {
            this.bos = bos;
            return this;
        }

        @AggressivenessMode
        public int getAggressivenessMode() {
            return aggressivenessMode;
        }

        public Builder setAggressivenessMode(@AggressivenessMode int aggressivenessMode) {
            this.aggressivenessMode = aggressivenessMode;
            return this;
        }

        @ChannelConfig
        public int getChannel() {
            return channel;
        }

        public Builder setChannel(@ChannelConfig int channel) {
            this.channel = channel;
            return this;
        }

        @BitWidth
        public int getPcmBitWidth() {
            return pcmBitWidth;
        }

        public Builder setPcmBitWidth(@BitWidth int pcmBitWidth) {
            this.pcmBitWidth = pcmBitWidth;
            return this;
        }
    }


    private int mNativePointer;

    private VADHandler(Builder builder) throws Exception {
        this.mBuilder = builder;

        mNativePointer = native_create(builder.aggressivenessMode);
        if (0 == mNativePointer) {
            throw new Exception("vad native exception");
        }
    }

    private ByteArrayOutputStream mDataBuf = new ByteArrayOutputStream();


    public void process(byte data[], int len) {

        int frameLenIn8Bit = getFrameLength(mBuilder.getFrameSize(), mBuilder.getSampleRate());

        int count = (mDataBuf.size() + len) / frameLenIn8Bit;
        int s = (mDataBuf.size() + len) % frameLenIn8Bit;


        if (count > 0) {
            mDataBuf.write(data, 0, len - s);


            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mDataBuf.toByteArray());

            byte[] temp = new byte[frameLenIn8Bit];

            int read;
            while ((read = byteArrayInputStream.read(temp, 0, frameLenIn8Bit)) >= 0) {
                short[] shortArray = new short[frameLenIn8Bit / 2];
                ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().get(shortArray);


                int active = native_process(mNativePointer, mBuilder.sampleRate, shortArray, shortArray.length);

                handleVAD(active == 1);
            }

            mDataBuf.reset();
        }


        mDataBuf.write(data, len - s, s);

    }


    private int flag;
    private int mCurrentStatus = STATUS_IDLE;

    private static final int STATUS_IDLE = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_INACTIVE = 2;


    private long beginTime = 0;

    private void handleVAD(boolean active) {
        if (active) {
            if (flag < 0) {
                flag = 0;
                beginTime = System.currentTimeMillis();
            }
            if (mCurrentStatus != STATUS_ACTIVE) {
                flag++;
            }

        } else {
            if (flag > 0) {
                flag = 0;
                beginTime = System.currentTimeMillis();
            }
            if (mCurrentStatus != STATUS_INACTIVE) {
                flag--;
            }
        }


        if (flag > 0
                && mCurrentStatus != STATUS_ACTIVE
                && flag * mBuilder.frameSize > mBuilder.bos) {
            mCurrentStatus = STATUS_ACTIVE;

            Log.d(TAG, "bos: " + (System.currentTimeMillis() - beginTime));
            if (null != mVadListener) {
                mVadListener.onVad(true);
            }

        } else if (flag < 0
                && mCurrentStatus != STATUS_INACTIVE
                && -flag * mBuilder.frameSize > mBuilder.eos) {
            mCurrentStatus = STATUS_INACTIVE;

            Log.d(TAG, "eos: " + (System.currentTimeMillis() - beginTime));
            if (null != mVadListener) {
                mVadListener.onVad(false);
            }
        }
    }


    private int getFrameLength(int frameSize, int sampleRate) {
        return sampleRate / 1000 * frameSize * 2;
    }

    public void release() {
        native_release(mNativePointer);
        mNativePointer = 0;
    }


    public static final int SAMPLE_RATE_8K = 8000;
    public static final int SAMPLE_RATE_16K = 16000;
    public static final int SAMPLE_RATE_32K = 32000;
    public static final int SAMPLE_RATE_48K = 48000;


    @IntDef({SAMPLE_RATE_8K, SAMPLE_RATE_16K, SAMPLE_RATE_32K, SAMPLE_RATE_48K})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SampleRate {
    }

    public static final int FRAME_10MS = 10;
    public static final int FRAME_20MS = 20;
    public static final int FRAME_30MS = 30;

    @IntDef({FRAME_10MS, FRAME_20MS, FRAME_30MS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FrameSize {
    }


    public static final int CHANNEL_MONO = 0;

    @IntDef({CHANNEL_MONO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ChannelConfig {
    }

    public static final int PCM_16BIT = 0;

    @IntDef({PCM_16BIT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BitWidth {
    }


    public static final int MODE_NORMAL = 0;
    public static final int MODE_LOW_BITRATE = 1;
    public static final int MODE_AGGRESSIVE = 2;
    public static final int MODE_VERY_AGGRESSIVE = 3;


    @IntDef({MODE_NORMAL, MODE_LOW_BITRATE, MODE_AGGRESSIVE, MODE_VERY_AGGRESSIVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AggressivenessMode {
    }


    private VADListener mVadListener;

    public void setVadListener(VADListener vadListener) {
        this.mVadListener = vadListener;
    }

    public interface VADListener {

        void onVad(boolean active);
    }


}
