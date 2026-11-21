#!/bin/sh
# A ponte do Whisper para o computador (WhisperHostTest). Depois:
#   ./gradlew testDebugUnitTest --tests '*WhisperHostTest' -PwhisperHost=true
#
# RF-VOZ-15 (1.6): o modelo não fica mais no repositório. Este script baixa o "base" de 5 bits da origem oficial (Hugging
# Face) para app/build/whisper-host/ e confere o SHA-256, o mesmo que o app confere (voice/WhisperModels.kt). Quem já tem
# o arquivo pode apontar para ele: CRYO_WHISPER_MODEL=/caminho/ggml-base-q5_1.bin app/src/test/cpp/build-host.sh
set -e
cd "$(dirname "$0")"
OUT=../../../build/whisper-host
MODEL="$OUT/ggml-base-q5_1.bin"
SHA=422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898
mkdir -p "$OUT"
if [ -n "$CRYO_WHISPER_MODEL" ]; then
    cp "$CRYO_WHISPER_MODEL" "$MODEL"
elif [ ! -f "$MODEL" ]; then
    curl -fL --retry 3 -o "$MODEL.part" https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin
    mv "$MODEL.part" "$MODEL"
fi
echo "$SHA  $MODEL" | sha256sum -c -
cmake -S . -B "$OUT" -DCMAKE_BUILD_TYPE=Release >/dev/null
cmake --build "$OUT" -j"$(nproc)" --target cryo_whisper_host
