/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.media.transfer

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.androidutils.filesize.FileSizeFormatter
import io.element.android.libraries.matrix.ui.media.MediaTransferManager
import io.element.android.libraries.matrix.ui.media.TransferState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class MediaTransferManagerImpl @Inject constructor(
    private val fileSizeFormatter: FileSizeFormatter,
): MediaTransferManager {

    private val _transfers = MutableStateFlow<Map<String, TransferState>>(emptyMap())

    override val transfers =
        _transfers.asStateFlow()

    override fun updateTransfer(
        transferId: String,
        state: TransferState
    ) {
        _transfers.update {
            it + (transferId to state)
        }
    }

    override fun removeTransfer(
        transferId: String
    ) {
        _transfers.update {
            it - transferId
        }
    }

    override fun fileSizFormat(fileSize: Long, useShortFormat: Boolean): String = fileSizeFormatter.format(fileSize = fileSize, useShortFormat = useShortFormat)

}
