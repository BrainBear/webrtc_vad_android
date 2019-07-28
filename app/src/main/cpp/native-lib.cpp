#include <jni.h>
#include <string>

#include <android/log.h>
#include "vad/webrtc_vad.h"

#define  LOG_TAG    "jnitest"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)


VadInst *vad;

extern "C"
JNIEXPORT jint JNICALL
Java_com_brainbear_webrtc_1vad_1android_VADHandler_create(JNIEnv *env, jclass type, jint mode) {

    int ret = 0;
    vad = WebRtcVad_Create();
    LOGD("create=%d", vad == NULL);

    ret = WebRtcVad_Init(vad);
    LOGD("init=%d", ret);

    ret = WebRtcVad_set_mode(vad, mode);
    LOGD("set mode=%d", ret);

    return ret;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_brainbear_webrtc_1vad_1android_VADHandler_process(JNIEnv *env, jclass type, jint fs,
                                                           jshortArray data_, jint len) {
    jshort *data = env->GetShortArrayElements(data_, NULL);

    int ret = WebRtcVad_Process(vad, fs, data, len);

    env->ReleaseShortArrayElements(data_, data, 0);

    return ret;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_brainbear_webrtc_1vad_1android_VADHandler_release(JNIEnv *env, jclass type) {

    WebRtcVad_Free(vad);
    return 0;

}