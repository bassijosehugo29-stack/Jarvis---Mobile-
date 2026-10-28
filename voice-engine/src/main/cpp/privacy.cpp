// SPDX-License-Identifier: GPL-3.0-or-later
#include <android/log.h>
#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <cstring>

// A dependency may include input text in an error. Filter logging in this process
// before loading any inference runtime, and drop stderr. Only our static,
// text-free diagnostic tag is forwarded. This does not change other apps' logs.
static void private_logger(const __android_log_message* message) {
    if (message && message->tag && std::strcmp(message->tag, "JarvisVoiceA") == 0) {
        __android_log_logd_logger(message);
    }
}
extern "C" JNIEXPORT jboolean JNICALL
Java_br_com_jarvis_voicea_NativePrivacy_installImpl(JNIEnv*, jobject) {
    const int sink = open("/dev/null", O_WRONLY | O_CLOEXEC);
    if (sink < 0) return JNI_FALSE;
    const bool redirected = dup2(sink, STDERR_FILENO) >= 0;
    close(sink);
    if (!redirected) return JNI_FALSE;
    __android_log_set_logger(private_logger);
    return JNI_TRUE;
}
