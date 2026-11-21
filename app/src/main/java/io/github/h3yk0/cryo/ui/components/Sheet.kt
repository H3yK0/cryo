// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package io.github.h3yk0.cryo.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * DS-PAD-03: a folha que sobe de baixo, em todo o app. É o `ModalBottomSheet` do Material com uma diferença: a folha
 * **nunca entra embaixo da barra de status** (ERR-059).
 *
 * Por quê: o `ModalBottomSheet` dá ao conteúdo uma margem de cima do tamanho da parte da barra de status que a folha cobre,
 * e essa parte depende de onde a folha está. Quando o conteúdo quase enchia a tela (a lista de idiomas com 14 línguas, num
 * celular comum), a margem mudava a altura da folha, a altura mudava a posição, a posição mudava a margem... e a folha
 * ficava tremendo para cima e para baixo sem parar. Com a margem da barra de status aplicada **por fora** da folha, ela
 * deixa de depender da posição e a conta para de girar, em qualquer altura de conteúdo e em qualquer celular.
 */
@Composable
fun CryoSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        content = content,
    )
}
