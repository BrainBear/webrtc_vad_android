#include <jni.h>
#include <string>

#include <android/log.h>
#include "vad/webrtc_vad.h"

#define  LOG_TAG    "jnitest"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)



extern "C"
JNIEXPORT jint JNICALL
Java_com_brainbear_webrtc_1vad_1android_VADHandler_native_1create(JNIEnv *env, jclass type,
                                                                  jint mode) {

    int ret = 0;
    VadInst *vad;
    vad = WebRtcVad_Create();
    LOGD("create=%d", vad == NULL);

    ret = WebRtcVad_Init(vad);
    LOGD("init=%d", ret);

    ret = WebRtcVad_set_mode(vad, mode);
    LOGD("set mode=%d", ret);

    return (jint) (vad);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_brainbear_webrtc_1vad_1android_VADHandler_native_1process(JNIEnv *env, jclass type,
                                                                   jint pointer, jint fs,
                                                                   jshortArray data_, jint len) {
    jshort *data = env->GetShortArrayElements(data_, NULL);

    int ret = 0;
    ret = WebRtcVad_ValidRateAndFrameLength(fs, len);
    if (ret < 0) {
        LOGD("invalid rate and frame length");
    }
    ret = WebRtcVad_Process((VadInst *) pointer, fs, data, len);

    env->ReleaseShortArrayElements(data_, data, 0);

    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_brainbear_webrtc_1vad_1android_VADHandler_native_1release(JNIEnv *env, jclass type,
                                                                   jint pointer) {

    WebRtcVad_Free((VadInst *) pointer);
    return 0;

}