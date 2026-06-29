/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.media


sealed interface TransferState {
    data object Idly: TransferState

    data class InProgress(
        val current: Long,
        val total: Long
    ): TransferState

    data object Cancelled: TransferState
    data object Success: TransferState
    data class Failed(val error: String): TransferState
}
