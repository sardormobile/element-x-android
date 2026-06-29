/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.media

import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.matrix.api.media.MatrixMediaLoader
import io.element.android.libraries.matrix.api.media.MediaFile
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.MediaTransferManager
import io.element.android.libraries.matrix.ui.media.TransferState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.MediaFileProgressWatcher
import org.matrix.rustcomponents.sdk.MediaFileTransferProgress
import org.matrix.rustcomponents.sdk.use
import timber.log.Timber
import java.io.File
import org.matrix.rustcomponents.sdk.MediaSource as RustMediaSource

class RustMediaLoader(
    private val baseCacheDirectory: File,
    dispatchers: CoroutineDispatchers,
    private val innerClient: Client,
    private val mediaTransferManager: MediaTransferManager
) : MatrixMediaLoader {
    private val mediaDispatcher = dispatchers.io.limitedParallelism(32)
    private val cacheDirectory
        get() = File(baseCacheDirectory, "temp/media").apply {
            if (!exists()) mkdirs() // Must always ensure that this directory exists because "Clear cache" does not restart an app's process.
        }

    override suspend fun loadMediaContent(source: MediaSource): Result<ByteArray> =
        withContext(mediaDispatcher) {
            runCatchingExceptions {
                source.toRustMediaSource().use { source ->
                    innerClient.getMediaContent(source)
                }
            }
        }

    override suspend fun loadMediaThumbnail(
        source: MediaSource,
        width: Long,
        height: Long
    ): Result<ByteArray> =
        withContext(mediaDispatcher) {
            runCatchingExceptions {
                source.toRustMediaSource().use { mediaSource ->
                    innerClient.getMediaThumbnail(
                        mediaSource = mediaSource,
                        width = width.toULong(),
                        height = height.toULong()
                    )
                }
            }
        }

    override suspend fun downloadMediaFile(
        source: MediaSource,
        mimeType: String?,
        filename: String?,
        useCache: Boolean,
    ): Result<MediaFile> =
        withContext(mediaDispatcher) {
            try {
                runCatchingExceptions {
                    source.toRustMediaSource().use { mediaSource ->
                        Timber.tag("transmissionProgress123").d("mediaSource: ${mediaSource.url()}")
                        val mediaFile = innerClient.getMediaFileWithProgress(
                            mediaSource = mediaSource,
                            filename = filename,
                            mimeType = when {
                                mimeType == null -> MimeTypes.OctetStream
                                MimeTypes.hasSubtype(mimeType) -> mimeType
                                // Fallback to a default mime type based on the main type, so that the SDK can create a file with the correct extension.
                                mimeType == MimeTypes.Images -> MimeTypes.Jpeg
                                mimeType == MimeTypes.Videos -> MimeTypes.Mp4
                                mimeType == MimeTypes.Audio -> MimeTypes.Mp3
                                else -> MimeTypes.OctetStream
                            },
                            useCache = useCache,
                            tempDir = cacheDirectory.path,
                            progressWatcher = object : MediaFileProgressWatcher {
                                override fun transmissionProgress(progress: MediaFileTransferProgress) {
                                    when(progress) {
                                        MediaFileTransferProgress.Cancelled -> {
                                            Timber.tag("transmissionProgress123").d("state: Cancelled")
                                            mediaTransferManager.updateTransfer(
                                                transferId = source.safeUrl,
                                                state = TransferState.Cancelled
                                            )
                                        }
                                        is MediaFileTransferProgress.Failed -> {
                                            Timber.tag("transmissionProgress123").d("state: Failed")
                                            mediaTransferManager.updateTransfer(
                                                transferId = source.safeUrl,
                                                state = TransferState.Failed(progress.error)
                                            )
                                        }
                                        MediaFileTransferProgress.Success -> {
                                            Timber.tag("transmissionProgress123").d("state: Success")
                                            mediaTransferManager.updateTransfer(
                                                transferId = source.safeUrl,
                                                state = TransferState.Success
                                            )
                                        }
                                        is MediaFileTransferProgress.Progress -> {
                                            Timber.tag("transmissionProgress123").d("state: Progress")
                                            mediaTransferManager.updateTransfer(
                                                transferId = source.safeUrl,
                                                state = TransferState.InProgress(current = progress.current.toLong(), total = progress.total.toLong())
                                            )
                                        }
                                    }
                                }
                            },
                        )
                        RustMediaFile(mediaFile)
                    }
                }
            } catch (e: CancellationException) {
                Timber.tag("transmissionProgress123").d("CancellationException")
                cancelMediaDownload(source)
                throw e
            }
        }

    override fun cancelMediaDownload(source: MediaSource) {
        source.toRustMediaSource().use { source ->
            innerClient.cancelMediaFileProgress(source)
        }
    }

    override suspend fun hasMediaInCache(source: MediaSource): Boolean {
        return innerClient.hasMediaContent(source.toRustMediaSource())
    }

    private fun MediaSource.toRustMediaSource(): RustMediaSource {
        val json = this.json
        return if (json != null) {
            RustMediaSource.fromJson(json)
        } else {
            RustMediaSource.fromUrl(safeUrl)
        }
    }
}
