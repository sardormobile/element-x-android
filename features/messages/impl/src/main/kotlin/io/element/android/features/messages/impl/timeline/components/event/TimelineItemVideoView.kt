/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import android.annotation.SuppressLint
import android.text.SpannedString
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.FakeMediaTransferManager
import io.element.android.features.messages.impl.MediaFileTransferAction
import io.element.android.features.messages.impl.MessagesEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.components.ATimelineItemEventRow
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayout
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayoutData
import io.element.android.features.messages.impl.timeline.model.TimelineItemGroupPosition
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemVideoContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemVideoContentProvider
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemVideoContent
import io.element.android.features.messages.impl.timeline.protection.ProtectedView
import io.element.android.features.messages.impl.timeline.protection.coerceRatioWhenHidingContent
import io.element.android.libraries.designsystem.components.blurhash.blurHashBackground
import io.element.android.libraries.designsystem.modifiers.onKeyboardContextMenuAction
import io.element.android.libraries.designsystem.modifiers.roundedBackground
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.matrix.ui.media.MAX_THUMBNAIL_HEIGHT
import io.element.android.libraries.matrix.ui.media.MAX_THUMBNAIL_WIDTH
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import io.element.android.libraries.matrix.ui.media.MediaTransferManager
import io.element.android.libraries.matrix.ui.media.TransferState
import io.element.android.libraries.textcomposer.ElementRichTextEditorStyle
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.ui.utils.a11y.isTalkbackActive
import io.element.android.wysiwyg.compose.EditorStyledText
import io.element.android.wysiwyg.link.Link

@SuppressLint("TimberArgCount")
@Composable
fun TimelineItemVideoView(
    content: TimelineItemVideoContent,
    mediaTransferManager: MediaTransferManager,
    hideMediaContent: Boolean,
    onContentClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    onShowContentClick: () -> Unit,
    onLinkClick: (Link) -> Unit,
    onLinkLongClick: (Link) -> Unit,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit,
    onMediaFileTransfer: ((MessagesEvent.MediaFileTransfer) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isTalkbackActive = isTalkbackActive()
    val a11yLabel = stringResource(CommonStrings.common_video)
    val description = content.caption?.let { "$a11yLabel: $it" } ?: a11yLabel
    val transferManagerState by mediaTransferManager.transfers.collectAsState()

    val containerModifier = if (content.showCaption) {
        Modifier
//                .padding(top = 6.dp)
            .clip(RoundedCornerShape(6.dp))
    } else {
        Modifier
    }

    val mediaDuration = buildString {
        val h = content.duration.inWholeHours
        val m = content.duration.inWholeMinutes % 60
        val s = content.duration.inWholeSeconds % 60

        if (h > 0) {
            append("$h:")
            append("%02d:".format(m))
        } else {
            append("$m:")
        }

        append("%02d".format(s))
    }
    val transferState = transferManagerState.getOrElse(content.mediaSource.safeUrl) { TransferState.Idly }

    val fileSize = when (transferState) {
        is TransferState.InProgress ->
            "${mediaTransferManager.fileSizFormat(transferState.current)} / ${
                mediaTransferManager.fileSizFormat(
                    transferState.total
                )
            }"

        else -> content.formattedFileSize
    }

    Column(modifier = modifier) {
        TimelineItemAspectRatioBox(
            modifier = containerModifier.blurHashBackground(content.blurHash, alpha = 0.9f),
            aspectRatio = coerceRatioWhenHidingContent(content.aspectRatio, hideMediaContent),
            contentAlignment = Alignment.TopStart,
        ) {
            ProtectedView(
                hideContent = hideMediaContent,
                onShowClick = onShowContentClick,
            ) {
                var isLoaded by remember { mutableStateOf(false) }
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLoaded) Modifier.background(Color.White) else Modifier)
                        .then(
                            if (!isTalkbackActive && onContentClick != null) {
                                Modifier
                                    .combinedClickable(
                                        onClick = onContentClick,
                                        onLongClick = onLongClick,
                                    )
                                    .onKeyboardContextMenuAction(onLongClick)
                            } else {
                                Modifier
                            }
                        ),
                    model = MediaRequestData(
                        source = content.thumbnailSource,
                        kind = MediaRequestData.Kind.Thumbnail(
                            width = content.thumbnailWidth?.toLong() ?: MAX_THUMBNAIL_WIDTH,
                            height = content.thumbnailHeight?.toLong() ?: MAX_THUMBNAIL_HEIGHT,
                        )
                    ),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    contentDescription = description,
                    onState = { isLoaded = it is AsyncImagePainter.State.Success },
                )

                MediaDownloadChip(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    content = content,
                    transferState = transferState,
                    fileSize = fileSize,
                    duration = mediaDuration,
                    onDownloadClick = {
                        onMediaFileTransfer?.invoke(
                            MessagesEvent.MediaFileTransfer(
                                mediaSource = content.mediaSource,
                                kind = MediaRequestData.Kind.File(
                                    fileName = content.filename,
                                    mimeType = content.mimeType
                                ),
                                action = MediaFileTransferAction.Download
                            )
                        )
                    },
                    onCancelClick = {
                        onMediaFileTransfer?.invoke(
                            MessagesEvent.MediaFileTransfer(
                                mediaSource = content.mediaSource,
                                kind = MediaRequestData.Kind.File(
                                    fileName = content.filename,
                                    mimeType = content.mimeType
                                ),
                                action = MediaFileTransferAction.Cancel
                            )
                        )
                    }
                )
                Box(
                    modifier = Modifier.roundedBackground().align(Alignment.Center),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        imageVector = CompoundIcons.PlaySolid(),
                        contentDescription = stringResource(id = CommonStrings.a11y_play),
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.semantics { hideFromAccessibility() }
                    )
                }
            }
        }

        if (content.showCaption) {
            Spacer(modifier = Modifier.height(8.dp))
            val caption = if (LocalInspectionMode.current) {
                SpannedString(content.caption)
            } else {
                content.formattedCaption ?: SpannedString(content.caption)
            }
            CompositionLocalProvider(
                LocalContentColor provides ElementTheme.colors.textPrimary,
                LocalTextStyle provides ElementTheme.typography.fontBodyLgRegular,
            ) {
                val aspectRatio = content.aspectRatio ?: DEFAULT_ASPECT_RATIO
                EditorStyledText(
                    modifier = Modifier
                        .padding(horizontal = 8.dp) // This is (12.dp - 8.dp) contentPadding from CommonLayout
                        .widthIn(min = MIN_HEIGHT_IN_DP.dp * aspectRatio, max = MAX_HEIGHT_IN_DP.dp * aspectRatio),
                    text = caption,
                    onLinkClickedListener = onLinkClick,
                    onLinkLongClickedListener = onLinkLongClick,
                    style = ElementRichTextEditorStyle.textStyle(),
                    releaseOnDetach = false,
                    onTextLayout = ContentAvoidingLayout.measureLegacyLastTextLine(onContentLayoutChange = onContentLayoutChange),
                )
            }
        }
    }
}
@SuppressLint("ConfigurationScreenWidthHeight", "TimberArgCount")
@Composable
fun MediaDownloadChip(
    transferState: TransferState,
    content: TimelineItemVideoContent,
    fileSize: String,
    duration: String,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
//    LaunchedEffect(transferState.hashCode()) {
//        when (transferState) {
//            TransferState.Cancelled -> {
//                Timber.tag("timelineState123").d("Cancelled", transferState)
//            }
//            is TransferState.Failed -> {
//                Timber.tag("timelineState123").d("Failed", transferState.error)
//            }
//            TransferState.Idly -> {
//                Timber.tag("timelineState123").d("Idly", transferState)
//            }
//            is TransferState.InProgress -> {
//                Timber.tag("timelineState123").d("InProgress: current: ${transferState.current}, total: ${transferState.total}")
//            }
//            is TransferState.NotDownloaded -> {
//                Timber.tag("timelineState123").d("NotDownloaded", transferState)
//            }
//            TransferState.Success -> {
//                Timber.tag("timelineState123").d("Success", transferState)
//            }
//        }
//    }
    val interactionSource = remember { MutableInteractionSource() }
    val isCached = content.isCached || transferState is TransferState.Success
    val isDownloading = transferState is TransferState.InProgress
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Color.Black.copy(alpha = 0.45f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
            ) {
                if (transferState is TransferState.InProgress) {
                    onCancelClick()
                } else {
                    onDownloadClick()
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 4.dp,
                vertical = 3.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (isDownloading) {
                val progress =
                    if (transferState.total > 0)
                        transferState.current.toFloat() / transferState.total.toFloat()
                    else 0f

                val animatedProgress by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = tween(250),
                    label = "progress"
                )

                Box(
                    modifier = Modifier.size(18.dp),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 1.5.dp,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                    )

                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else if (!isCached) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = duration,
                    color = Color.White,
                    fontSize = 9.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                if (!isCached) {
                    Text(
                        text = fileSize,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        lineHeight = 9.sp,
                        maxLines = 1,
                    )
                }

            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun TimelineItemVideoViewPreview(@PreviewParameter(TimelineItemVideoContentProvider::class) content: TimelineItemVideoContent) = ElementPreview {
    TimelineItemVideoView(
        content = content,
        mediaTransferManager = FakeMediaTransferManager,
        hideMediaContent = false,
        onShowContentClick = {},
        onContentClick = {},
        onLongClick = {},
        onLinkClick = {},
        onLinkLongClick = {},
        onContentLayoutChange = {},
        onMediaFileTransfer = {}
    )
}

@PreviewsDayNight
@Composable
internal fun TimelineItemVideoViewHideMediaContentPreview() = ElementPreview {
    TimelineItemVideoView(
        content = aTimelineItemVideoContent(),
        mediaTransferManager = FakeMediaTransferManager,
        hideMediaContent = true,
        onShowContentClick = {},
        onContentClick = {},
        onLongClick = {},
        onLinkClick = {},
        onLinkLongClick = {},
        onContentLayoutChange = {},
        onMediaFileTransfer = {}
    )
}

@PreviewsDayNight
@Composable
internal fun TimelineVideoWithCaptionRowPreview() = ElementPreview {
    Column {
        sequenceOf(false, true).forEach { isMine ->
            ATimelineItemEventRow(
                event = aTimelineItemEvent(
                    isMine = isMine,
                    content = aTimelineItemVideoContent().copy(
                        filename = "video.mp4",
                        caption = "A long caption that may wrap into several lines",
                        aspectRatio = 2.5f,
                    ),
                    groupPosition = TimelineItemGroupPosition.Last,
                ),
            )
        }
        ATimelineItemEventRow(
            event = aTimelineItemEvent(
                isMine = false,
                content = aTimelineItemVideoContent().copy(
                    filename = "video.mp4",
                    caption = "Video with null aspect ratio",
                    aspectRatio = null,
                ),
                groupPosition = TimelineItemGroupPosition.Last,
            ),
        )
    }
}
