#!/bin/sh
# A ponte do Whisper para o computador (WhisperHostTest). Depois:
#   ./gradlew testDebugUnitTest --tests '*WhisperHostTest' -PwhisperHost=true
set -e
cd "$(dirname "$0")"
cmake -S . -B ../../../build/whisper-host -DCMAKE_BUILD_TYPE=Release >/dev/null
cmake --build ../../../build/whisper-host -j"$(nproc)" --target cryo_whisper_host
