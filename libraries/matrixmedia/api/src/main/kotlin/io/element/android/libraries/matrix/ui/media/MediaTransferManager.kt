/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.media

import kotlinx.coroutines.flow.StateFlow

interface MediaTransferManager {
    val transfers: StateFlow<Map<String, TransferState>>

    fun updateTransfer(
        transferId: String,
        state: TransferState
    )

    fun removeTransfer(
        transferId: String
    )

    fun fileSizFormat(fileSize: Long, useShortFormat: Boolean = true): String
}
