// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
// Só para o teste no computador: veja asset_manager.h.
#pragma once
#include <jni.h>
#include "asset_manager.h"

inline AAssetManager * AAssetManager_fromJava(JNIEnv *, jobject) { static AAssetManager m{0}; return &m; }
