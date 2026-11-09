#!/bin/sh
# SPDX-License-Identifier: AGPL-3.0-or-later
# Copyright (C) 2026 Hanry Franco
#
# RF-ILU-04: junta os quadros de cada ilustração (gerados por StickerFramesTest) num WebP animado para as notificações.
#   ./gradlew testDebugUnitTest --tests '*StickerFramesTest' -Pstickers=true && tools/stickers/make_stickers.sh
# 24 quadros a 60 ms (1,44 s por volta), em laço: o Android só anima com a cortina aberta e para quando ela fecha.
set -e
cd "$(dirname "$0")/../.."
mkdir -p app/src/main/res/raw
for dir in app/screenshots/stickers/*/; do
  name=$(basename "$dir")
  ffmpeg -hide_banner -loglevel error -y -framerate 16.667 -i "$dir/f_%02d.png" \
    -c:v libwebp_anim -lossless 0 -quality 82 -compression_level 6 -loop 0 -pix_fmt yuva420p \
    "app/src/main/res/raw/sticker_$name.webp"
  printf '%s %s bytes\n' "$name" "$(wc -c < "app/src/main/res/raw/sticker_$name.webp")"
done
