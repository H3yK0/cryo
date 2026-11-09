# SPDX-License-Identifier: AGPL-3.0-or-later
# Copyright (C) 2026 Hanry Franco
#
# RF-VOZ-01/10: o reconhecimento de fala no aparelho (whisper.cpp, MIT, cópia em whisper/) numa biblioteca só,
# libcryo_whisper.so. Tudo aqui existe para duas coisas: rodar rápido no celular e sair IGUAL em qualquer máquina
# (o F-Droid compila de novo e compara com o APK da Release).

cmake_minimum_required(VERSION 3.22.1)
project(cryo_whisper C CXX)

# --- Compilação reproduzível -------------------------------------------------------------------------------------
# Nenhum caminho da máquina dentro do binário: as pastas do projeto, da compilação e do NDK viram nomes fixos
# (vale para __FILE__ das mensagens de erro do ggml). Sem informação de depuração (-g0): ela carregaria caminhos e
# mudaria o "build-id". O build-id vem do conteúdo (sha1), não da hora.
set(CRYO_FLAGS
    -O3 -DNDEBUG -g0
    -ffunction-sections -fdata-sections
    "-ffile-prefix-map=${CMAKE_SOURCE_DIR}=cryo"
    "-ffile-prefix-map=${CMAKE_BINARY_DIR}=build"
    "-ffile-prefix-map=${ANDROID_NDK}=ndk")
# As opções de compilação vêm depois das do NDK e do tipo de compilação na linha de comando: o -O3 e o -g0 daqui valem
# também na versão de teste (Whisper sem otimização ficaria lento demais para testar).
add_compile_options(${CRYO_FLAGS})
# Páginas de 16 KB (Android 15+ em alguns aparelhos): sem isso a biblioteca não carregaria neles.
add_link_options(-Wl,--build-id=sha1 -Wl,--gc-sections -Wl,-z,max-page-size=16384 -Wl,--exclude-libs,ALL)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# --- Celulares sem o Whisper --------------------------------------------------------------------------------------
# O modelo foi feito para processadores ARM de 64 bits com as instruções de 2017 para cá (fp16 e dotprod: o Galaxy
# A55 tem). Nos outros (32 bits, x86 dos emuladores), uma biblioteca "vazia" com as mesmas funções: o app instala do
# mesmo jeito e usa o reconhecedor do próprio celular (plano B, RF-VOZ-01).
if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
    add_library(cryo_whisper SHARED cryo_whisper_stub.cpp)
    target_compile_options(cryo_whisper PRIVATE -fvisibility=hidden)
    return()
endif()

# --- ggml (o motor de contas do Whisper) --------------------------------------------------------------------------
set(CRYO_GGML_COMMIT "v1.9.5" CACHE STRING "" FORCE)      # whisper.cpp v1.9.5; nunca do git (veja whisper/ggml/CMakeLists.txt)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)                  # nunca "otimizar para esta máquina": sairia diferente
set(GGML_CPU_ARM_ARCH "armv8.2-a+fp16+dotprod" CACHE STRING "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)                  # threads próprias do ggml (o OpenMP do NDK pesa e varia)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)            # baixaria código da internet durante a compilação
set(GGML_CCACHE OFF CACHE BOOL "" FORCE)
set(GGML_LTO OFF CACHE BOOL "" FORCE)
set(GGML_ALL_WARNINGS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
add_subdirectory(whisper/ggml ggml)

# --- A biblioteca do Cryo: Whisper + ponte com o Kotlin ------------------------------------------------------------
add_library(cryo_whisper SHARED
    cryo_whisper.cpp
    whisper/src/whisper.cpp
    whisper/src/aneforge/whisper-aneforge.cpp)
target_include_directories(cryo_whisper PRIVATE whisper/include whisper/src)
target_compile_definitions(cryo_whisper PRIVATE WHISPER_VERSION="1.9.5")
target_compile_features(cryo_whisper PRIVATE cxx_std_17)
target_compile_options(cryo_whisper PRIVATE -fvisibility=hidden)
target_link_libraries(cryo_whisper PRIVATE ggml android)
