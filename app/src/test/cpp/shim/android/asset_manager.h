// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
//
// Só para o teste no computador (WhisperHostTest): imita o leitor de assets do Android lendo arquivos da pasta
// CRYO_ASSETS. Assim a mesma ponte (src/main/cpp/cryo_whisper.cpp) roda na JVM, sem celular.
#pragma once
#include <cstdio>
#include <cstdlib>
#include <cstdint>
#include <string>

struct AAssetManager { int unused; };
struct AAsset { FILE * f; int64_t size; };
enum { AASSET_MODE_STREAMING = 2 };

inline AAsset * AAssetManager_open(AAssetManager *, const char * name, int) {
    const char * base = std::getenv("CRYO_ASSETS");
    if (base == nullptr) return nullptr;
    FILE * f = std::fopen((std::string(base) + "/" + name).c_str(), "rb");
    if (f == nullptr) return nullptr;
    std::fseek(f, 0, SEEK_END);
    int64_t size = std::ftell(f);
    std::fseek(f, 0, SEEK_SET);
    return new AAsset{f, size};
}
inline int AAsset_read(AAsset * a, void * buf, size_t n) { return static_cast<int>(std::fread(buf, 1, n, a->f)); }
inline int64_t AAsset_getRemainingLength64(AAsset * a) { return a->size - std::ftell(a->f); }
inline void AAsset_close(AAsset * a) { std::fclose(a->f); delete a; }
