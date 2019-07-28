package com.brainbear.webrtc_vad_android;

/**
 * created by canxionglian on 2019-07-28
 */
public class VADHandler {



    public native static int create(int mode);
    public native static int process(int fs,short data[],int len);
    public native static int release();



}
