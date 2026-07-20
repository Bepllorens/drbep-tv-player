package com.drbep.tvplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import java.util.WeakHashMap

/**
 * Keeps long-lived overlay compositions alive while their immutable UI model
 * changes. This preserves focus/scroll state and avoids rebuilding a complete
 * composition for every playback heartbeat or EPG update.
 */
private object StableComposeContentStore {
    data class Entry(
        val rendererKey: String,
        val model: MutableState<Any>
    )

    val entries = WeakHashMap<ComposeView, Entry>()
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> ComposeView.setStableContent(
    rendererKey: String,
    model: T,
    renderer: @Composable (T) -> Unit
) {
    val existing = StableComposeContentStore.entries[this]
    if (existing != null && existing.rendererKey == rendererKey) {
        existing.model.value = model
        return
    }
    val state = mutableStateOf<Any>(model)
    StableComposeContentStore.entries[this] = StableComposeContentStore.Entry(rendererKey, state)
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    setContent {
        renderer(state.value as T)
    }
}
