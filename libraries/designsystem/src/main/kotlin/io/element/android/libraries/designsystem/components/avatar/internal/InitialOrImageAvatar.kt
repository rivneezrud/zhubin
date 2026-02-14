/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.avatar.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import io.element.android.libraries.designsystem.components.avatar.AvatarData

@Composable
internal fun InitialOrImageAvatar(
    avatarData: AvatarData,
    hideAvatarImage: Boolean,
    forcedAvatarSize: Dp?,
    avatarShape: Shape,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val size = forcedAvatarSize ?: avatarData.size.dp

    when {
        // If no URL (or forced hide), but the name contains the Iran flag emoji, try to load the provided drawable
        (avatarData.url.isNullOrBlank() || hideAvatarImage) && (avatarData.name?.contains("🇮🇷") == true) -> {
            val context = LocalContext.current
            // The drawable is expected to be named "flag_iran" in the merged resources (app/src/main/res/drawable/flag_iran.png)
            val resId = context.resources.getIdentifier("flag_iran", "drawable", context.packageName)
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .size(size)
                        .clip(avatarShape)
                )
                return
            }

            // Fallback to initials if drawable not found
            InitialLetterAvatar(
                avatarData = avatarData,
                avatarShape = avatarShape,
                forcedAvatarSize = forcedAvatarSize,
                modifier = modifier,
                contentDescription = contentDescription,
            )
        }
        avatarData.url.isNullOrBlank() || hideAvatarImage -> InitialLetterAvatar(
            avatarData = avatarData,
            avatarShape = avatarShape,
            forcedAvatarSize = forcedAvatarSize,
            modifier = modifier,
            contentDescription = contentDescription,
        )
        else -> ImageAvatar(
            avatarData = avatarData,
            avatarShape = avatarShape,
            forcedAvatarSize = forcedAvatarSize,
            modifier = modifier,
            contentDescription = contentDescription,
        )
    }
}
