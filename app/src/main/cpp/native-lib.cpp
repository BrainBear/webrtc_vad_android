#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_brainbear_webrtc_1vad_1android_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from vad";
    return env->NewStringUTF(hello.c_str());
}
