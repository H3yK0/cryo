// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
//
// RF-VOZ-01: a ponte entre o Kotlin (voice/WhisperNative.kt) e o whisper.cpp. Só três trabalhos:
//  1. abrir o modelo direto de dentro do APK (assets), sem copiar para um arquivo;
//  2. transformar o áudio (que só existe na memória) em texto;
//  3. soltar tudo ao fechar o microfone (RF-VOZ-09).
// Nada aqui grava arquivo, abre rede ou escreve em log (RF-SEG-07): as mensagens do Whisper vão para uma função vazia.

#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <algorithm>
#include <atomic>
#include <mutex>
#include <string>
#include <vector>

#include "whisper.h"

namespace {

// As mensagens do Whisper e do ggml (nomes de arquivo, tempos) não vão para lugar nenhum.
void silent(enum ggml_log_level, const char *, void *) {}

struct Session {
    whisper_context * ctx = nullptr;
    std::atomic<bool> aborted{false};   // "Cancelar" ou microfone fechado: a conta em andamento para no próximo passo
    std::mutex busy;                     // fechar espera a transcrição em andamento terminar
    float noSpeech = 0.f;
};

// O modelo é lido em pedaços direto do APK (fica guardado sem compressão: noCompress "bin" no Gradle).
size_t assetRead(void * ctx, void * out, size_t n) {
    int r = AAsset_read(static_cast<AAsset *>(ctx), out, n);
    return r > 0 ? static_cast<size_t>(r) : 0;
}
bool assetEof(void * ctx) { return AAsset_getRemainingLength64(static_cast<AAsset *>(ctx)) <= 0; }
void assetClose(void * ctx) { AAsset_close(static_cast<AAsset *>(ctx)); }

bool shouldAbort(void * data) { return static_cast<Session *>(data)->aborted.load(); }

} // namespace

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *, void *) {
    whisper_log_set(silent, nullptr);   // também cala o ggml
    return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeAvailable(JNIEnv *, jobject) {
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeOpen(JNIEnv * env, jobject, jobject assets, jstring path, jboolean flash) {
    AAssetManager * mgr = AAssetManager_fromJava(env, assets);
    if (mgr == nullptr) return 0;
    const char * p = env->GetStringUTFChars(path, nullptr);
    AAsset * asset = AAssetManager_open(mgr, p, AASSET_MODE_STREAMING);
    env->ReleaseStringUTFChars(path, p);
    if (asset == nullptr) return 0;

    whisper_model_loader loader{asset, assetRead, assetEof, assetClose};
    whisper_context_params cp = whisper_context_default_params();
    cp.use_gpu = false;
    // RF-VOZ-13: a "atenção rápida" só com a chave experimental do modo desenvolvedor (ainda não conferida no celular).
    cp.flash_attn = flash;
    whisper_context * ctx = whisper_init_with_params(&loader, cp);   // fecha o asset sozinho, dando certo ou não
    if (ctx == nullptr) return 0;
    auto * s = new Session();
    s->ctx = ctx;
    return reinterpret_cast<jlong>(s);
}

/**
 * Áudio (16 kHz, mono, -1..1) → texto em UTF-8 (bytes: o "UTF-8 modificado" do JNI quebraria com emoji).
 * [greedy] = rápido, para as palavras ao vivo; senão, busca em feixe (beam search) para o resultado final.
 * [audioCtx] > 0 encurta a janela do Whisper (30 s) para o tamanho da fala: muito mais rápido em frases curtas.
 */
JNIEXPORT jbyteArray JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeTranscribe(
        JNIEnv * env, jobject, jlong handle, jfloatArray pcm, jint samples, jstring language, jstring prompt,
        jint threads, jboolean greedy, jint audioCtx, jint maxTokens) {
    auto * s = reinterpret_cast<Session *>(handle);
    if (s == nullptr || s->aborted.load()) return nullptr;
    std::lock_guard<std::mutex> lock(s->busy);
    if (s->ctx == nullptr || s->aborted.load()) return nullptr;

    // Menos de 1 s o Whisper não ouve direito: completa com silêncio.
    const int n = std::max(0, std::min<int>(samples, env->GetArrayLength(pcm)));
    std::vector<float> audio(std::max(n, 16000 + 1600), 0.f);
    env->GetFloatArrayRegion(pcm, 0, n, audio.data());

    const char * lang = env->GetStringUTFChars(language, nullptr);
    const char * hint = prompt != nullptr ? env->GetStringUTFChars(prompt, nullptr) : nullptr;

    whisper_full_params wp = whisper_full_default_params(greedy ? WHISPER_SAMPLING_GREEDY : WHISPER_SAMPLING_BEAM_SEARCH);
    wp.n_threads = std::max(1, static_cast<int>(threads));
    wp.language = lang;
    wp.detect_language = false;
    wp.translate = false;
    wp.no_context = true;
    wp.no_timestamps = true;
    wp.single_segment = greedy;
    wp.print_special = false;
    wp.print_progress = false;
    wp.print_realtime = false;
    wp.print_timestamps = false;
    wp.suppress_blank = true;
    wp.suppress_nst = true;          // nada de "[Música]" e afins
    wp.initial_prompt = hint;        // RF-VOZ-04: os nomes da pessoa e o jeito de escrever valores
    wp.temperature = 0.0f;
    wp.temperature_inc = greedy ? 0.0f : 0.2f;
    wp.max_tokens = std::max(0, static_cast<int>(maxTokens));
    wp.audio_ctx = std::max(0, static_cast<int>(audioCtx));
    wp.greedy.best_of = 1;
    wp.beam_search.beam_size = 3;
    wp.abort_callback = shouldAbort;
    wp.abort_callback_user_data = s;

    const int rc = whisper_full(s->ctx, wp, audio.data(), static_cast<int>(audio.size()));
    std::fill(audio.begin(), audio.end(), 0.f);   // o áudio não fica na memória além do necessário

    env->ReleaseStringUTFChars(language, lang);
    if (hint != nullptr) env->ReleaseStringUTFChars(prompt, hint);
    if (rc != 0 || s->aborted.load()) return nullptr;

    std::string text;
    float noSpeech = 0.f;
    const int segs = whisper_full_n_segments(s->ctx);
    for (int i = 0; i < segs; i++) {
        text += whisper_full_get_segment_text(s->ctx, i);
        noSpeech = std::max(noSpeech, whisper_full_get_segment_no_speech_prob(s->ctx, i));
    }
    s->noSpeech = segs > 0 ? noSpeech : 1.f;
    jbyteArray out = env->NewByteArray(static_cast<jsize>(text.size()));
    if (out != nullptr) env->SetByteArrayRegion(out, 0, static_cast<jsize>(text.size()), reinterpret_cast<const jbyte *>(text.data()));
    return out;
}

/** Chance (0..1), segundo o Whisper, de o último trecho não ter fala: acima de ~0,6 é ruído e o texto é descartado. */
JNIEXPORT jfloat JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeNoSpeech(JNIEnv *, jobject, jlong handle) {
    auto * s = reinterpret_cast<Session *>(handle);
    return s != nullptr ? s->noSpeech : 1.f;
}

/** Pede para a transcrição em andamento parar (pode vir de outra thread). Vale até fechar. */
JNIEXPORT void JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeAbort(JNIEnv *, jobject, jlong handle) {
    auto * s = reinterpret_cast<Session *>(handle);
    if (s != nullptr) s->aborted.store(true);
}

JNIEXPORT void JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeClose(JNIEnv *, jobject, jlong handle) {
    auto * s = reinterpret_cast<Session *>(handle);
    if (s == nullptr) return;
    s->aborted.store(true);
    {
        std::lock_guard<std::mutex> lock(s->busy);   // espera a conta em andamento parar
        if (s->ctx != nullptr) whisper_free(s->ctx);
        s->ctx = nullptr;
    }
    delete s;
}

JNIEXPORT jstring JNICALL
Java_io_github_h3yk0_cryo_voice_WhisperNative_nativeInfo(JNIEnv * env, jobject) {
    return env->NewStringUTF(whisper_print_system_info());
}

} // extern "C"
