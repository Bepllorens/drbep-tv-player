package com.drbep.tvplayer

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Only composed rows tick, and only while this window is visible and resumed. No I/O. */
@Composable
internal fun <T> rememberVisibleProgramme(key: Any, initial: T, read: (() -> T)?, initialDelayMs: Long = 0, same: (T,T)->Boolean): T {
    var value by remember(key, initial) { mutableStateOf(initial) }
    val latestRead by rememberUpdatedState(read)
    val latestSame by rememberUpdatedState(same)
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = LocalProgrammeRefreshScope.current
    if (scope != null) {
        DisposableEffect(key, initial, scope) {
            val registration = scope.queue.register(android.os.SystemClock.uptimeMillis(), initialDelayMs) {
                // Keep the per-read guard too: visibility can change before the
                // shared lifecycle collector has processed its cancellation.
                if (!view.isShown || !view.hasWindowFocus()) return@register
                android.os.Trace.beginSection("DRBEP.visibleProgramme")
                try { latestRead?.invoke()?.let { fresh -> if (!latestSame(value, fresh)) value = fresh } }
                finally { android.os.Trace.endSection() }
            }
            scope.changed()
            onDispose { scope.queue.unregister(registration); scope.changed() }
        }
    } else LaunchedEffect(key, initial, lifecycle, view) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (initialDelayMs > 0) delay(initialDelayMs)
            while (true) {
                if (view.isShown && view.hasWindowFocus()) {
                    android.os.Trace.beginSection("DRBEP.visibleProgramme")
                    try { latestRead?.invoke()?.let { fresh -> if (!latestSame(value,fresh)) value=fresh } }
                    finally { android.os.Trace.endSection() }
                }
                delay(15_000)
            }
        }
    }
    return value
}

@Composable
internal fun liveProgrammeCard(card: StartupHomeHubUiModel.ContinueCard, reuseRecentSnapshot: Boolean = false): StartupHomeHubUiModel.ContinueCard {
    val initial = remember(card) {
        if (card.initialProgramme == null) card else {
            android.os.Trace.beginSection("DRBEP.tvVisibleSnapshot")
            try { card.resolveProgrammeSnapshot() } finally { android.os.Trace.endSection() }
        }
    }
    return rememberVisibleProgramme(initial.channelId, initial, initial.refreshProgramme?.let { supplier -> { supplier.get() } },
        initialDelayMs = if (reuseRecentSnapshot && initial.programmeSnapshotUptimeMs > 0)
            (15_000L - (android.os.SystemClock.uptimeMillis() - initial.programmeSnapshotUptimeMs)).coerceIn(0, 15_000L) else 0) { a,b ->
        a.channelId==b.channelId && a.subtitle==b.subtitle && a.schedule==b.schedule &&
            a.description==b.description && a.nextProgram==b.nextProgram && a.progress==b.progress &&
            a.programmeMetadata?.key==b.programmeMetadata?.key && a.programmeMetadata?.initial==b.programmeMetadata?.initial
    }
}
