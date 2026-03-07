/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

// Local no-op replacement for `com.google.gms.google-services`.
// This keeps builds working in environments where the upstream plugin
// cannot be resolved. Firebase is initialized manually in app code.
