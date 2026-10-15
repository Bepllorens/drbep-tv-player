package com.drbep.tvplayer

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Only composed rows tick, and only while this window is visible and resumed. No I/O. */
@Composable
internal fun <T> rememberVisibleProgramme(key: Any, initial: T, read: (() -> T)?, same: (T,T)->Boolean): T {
    var value by remember(key, initial) { mutableStateOf(initial) }
    val latestRead by rememberUpdatedState(read)
    val latestSame by rememberUpdatedState(same)
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(key, initial, lifecycle, view) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                if (view.isShown && view.hasWindowFocus()) {
                    latestRead?.invoke()?.let { fresh -> if (!latestSame(value,fresh)) value=fresh }
                }
                delay(15_000)
            }
        }
    }
    return value
}

@Composable
internal fun liveProgrammeCard(card: StartupHomeHubUiModel.ContinueCard): StartupHomeHubUiModel.ContinueCard =
    rememberVisibleProgramme(card.channelId, card, card.refreshProgramme?.let { supplier -> { supplier.get() } }) { a,b ->
        a.channelId==b.channelId && a.subtitle==b.subtitle && a.schedule==b.schedule &&
            a.description==b.description && a.nextProgram==b.nextProgram && a.progress==b.progress &&
            a.programmeMetadata?.key==b.programmeMetadata?.key && a.programmeMetadata?.initial==b.programmeMetadata?.initial
    }
