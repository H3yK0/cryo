// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
//
// Celulares sem o Whisper (32 bits, x86): as mesmas funções da ponte, mas "não disponível". O Kotlin vê isso e usa o
// reconhecedor do próprio celular (plano B, RF-VOZ-01). Existe só para o APK instalar em todo celular.

#include <jni.h>

extern "C" {

JNIEXPORT jboolean JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeAvailable(JNIEnv *, jobject) { return JNI_FALSE; }

JNIEXPORT jlong JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeOpen(JNIEnv *, jobject, jobject, jstring) { return 0; }

JNIEXPORT jbyteArray JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeTranscribe(
        JNIEnv *, jobject, jlong, jfloatArray, jint, jstring, jstring, jint, jboolean, jint, jint) { return nullptr; }

JNIEXPORT jfloat JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeNoSpeech(JNIEnv *, jobject, jlong) { return 1.f; }

JNIEXPORT void JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeAbort(JNIEnv *, jobject, jlong) {}

JNIEXPORT void JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeClose(JNIEnv *, jobject, jlong) {}

JNIEXPORT jstring JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeInfo(JNIEnv * env, jobject) { return env->NewStringUTF(""); }

} // extern "C"
