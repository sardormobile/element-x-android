/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl

import io.element.android.libraries.matrix.ui.media.MediaTransferManager
import io.element.android.libraries.matrix.ui.media.TransferState
import kotlinx.coroutines.flow.MutableStateFlow

object FakeMediaTransferManager : MediaTransferManager {

    override val transfers =
        MutableStateFlow(
            mapOf(
                "event1" to TransferState.InProgress(
                    current = 50,
                    total = 100,
                )
            )
        )

    override fun updateTransfer(transferId: String, state: TransferState) {

    }

    override fun removeTransfer(transferId: String) {
    }

    override fun fileSizFormat(fileSize: Long, useShortFormat: Boolean): String {
        return "***"
    }

}
