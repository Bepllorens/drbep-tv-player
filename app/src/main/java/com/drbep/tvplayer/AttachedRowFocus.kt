package com.drbep.tvplayer

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Wait for the target's layout, not a guessed number of frames; cancellation follows the caller. */
internal suspend fun awaitAttachedRowFocus(
    list: LazyListState, id: String, requester: () -> FocusRequester?, focused: () -> Boolean,
): Boolean = withTimeoutOrNull(2_000L) {
    snapshotFlow { list.layoutInfo.visibleItemsInfo.any { it.key == id } }.first { it }
    while (true) {
        withFrameNanos { }
        requester()?.let { runCatching { it.requestFocus() } }
        withFrameNanos { }
        if (focused()) return@withTimeoutOrNull true
    }
    @Suppress("UNREACHABLE_CODE") false
} ?: false
