# SPDX-License-Identifier: AGPL-3.0-or-later
# Copyright (C) 2026 Hanry Franco
#
# A ponte do Whisper compilada para o computador, só para o WhisperHostTest (não vai para o APK):
#   app/src/test/cpp/build-host.sh
cmake_minimum_required(VERSION 3.22)
project(cryo_whisper_host C CXX)
find_package(JNI REQUIRED)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CRYO_GGML_COMMIT "v1.9.5" CACHE STRING "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_CCACHE OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(MAIN ${CMAKE_CURRENT_SOURCE_DIR}/../../main/cpp)
add_subdirectory(${MAIN}/whisper/ggml ggml)
add_library(cryo_whisper_host SHARED ${MAIN}/cryo_whisper.cpp ${MAIN}/whisper/src/whisper.cpp ${MAIN}/whisper/src/aneforge/whisper-aneforge.cpp)
target_include_directories(cryo_whisper_host PRIVATE shim ${JNI_INCLUDE_DIRS} ${MAIN}/whisper/include ${MAIN}/whisper/src)
target_compile_definitions(cryo_whisper_host PRIVATE WHISPER_VERSION="1.9.5")
target_compile_features(cryo_whisper_host PRIVATE cxx_std_17)
target_compile_options(cryo_whisper_host PRIVATE -O2)
target_link_libraries(cryo_whisper_host PRIVATE ggml)
