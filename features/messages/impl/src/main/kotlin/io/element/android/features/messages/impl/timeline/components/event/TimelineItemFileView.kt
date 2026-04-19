/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayoutData
import io.element.android.features.messages.impl.timeline.media.TransferProgress
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemFileContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemFileContentProvider
import io.element.android.libraries.designsystem.icons.CompoundDrawables
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.LinearProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TimelineItemFileView(
    content: TimelineItemFileContent,
    sendState: LocalEventSendState?,
    downloadProgress: StateFlow<TransferProgress?>?,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit,
    modifier: Modifier = Modifier,
) {
    android.util.Log.e("ZHUDEBUG", "sendState=$sendState, progress=${(sendState as? LocalEventSendState.Sending.MediaWithProgress)?.progress}, total=${(sendState as? LocalEventSendState.Sending.MediaWithProgress)?.total}")
    android.util.Log.e("ZHUDEBUG", "content type: ${content::class.simpleName}")
    val uploadProgress = remember(sendState) {
        (sendState as? LocalEventSendState.Sending.MediaWithProgress)
            ?.takeIf { it.total > 0L && it.progress in 0 until it.total }
            ?.let {
                TransferProgress(
                    bytesTransferred = it.progress,
                    contentLength = it.total,
                )
            }
    }

    val downloadProgressState by (downloadProgress ?: EmptyTransferProgressStateFlow).collectAsState()

    val progress = uploadProgress ?: downloadProgressState
    val showProgress = progress != null && progress.contentLength > 0L && progress.fraction < 1f

    Column(modifier = modifier) {
        TimelineItemAttachmentView(
            filename = content.filename,
            fileExtensionAndSize = content.fileExtensionAndSize,
            caption = content.caption,
            onContentLayoutChange = onContentLayoutChange,
            icon = {
                Icon(
                    resourceId = CompoundDrawables.ic_compound_attachment,
                    contentDescription = stringResource(CommonStrings.common_file),
                    tint = ElementTheme.colors.iconPrimary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(-45f),
                )
            }
        )

        if (showProgress) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LinearProgressIndicator(
                    progress = { progress!!.fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${progress!!.percentage}%",
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

private object EmptyTransferProgressStateFlow : StateFlow<TransferProgress?> {
    override val replayCache: List<TransferProgress?> = listOf(null)
    override val value: TransferProgress? = null

    override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<TransferProgress?>): Nothing {
        awaitCancellation()
    }
}

@PreviewsDayNight
@Composable
internal fun TimelineItemFileViewPreview(@PreviewParameter(TimelineItemFileContentProvider::class) content: TimelineItemFileContent) = ElementPreview {
    TimelineItemFileView(
        content = content,
        sendState = LocalEventSendState.Sending.MediaWithProgress(
            index = 0L,
            progress = 42L,
            total = 100L,
        ),
        downloadProgress = null,
        onContentLayoutChange = {},
    )
}
