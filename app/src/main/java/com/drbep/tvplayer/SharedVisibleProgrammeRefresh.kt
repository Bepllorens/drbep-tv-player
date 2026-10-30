package com.drbep.tvplayer

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

internal class VisibleProgrammeRefreshScope {
    val wake = Channel<Unit>(Channel.CONFLATED)
    val visible = MutableStateFlow(false)
    val queue = VisibleProgrammeRefreshQueue { failure ->
        android.util.Log.w("ProgrammeRefresh", "Cached refresh failed: " + failure.javaClass.simpleName)
    }
    fun changed() { wake.trySend(Unit) }
}

internal val LocalProgrammeRefreshScope = staticCompositionLocalOf<VisibleProgrammeRefreshScope?> { null }

/** One lifecycle observer/clock per TV screen; rows only register while mounted. */
@Composable
internal fun SharedVisibleProgrammeRefresh(content: @Composable () -> Unit) {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = remember(view, lifecycle) { VisibleProgrammeRefreshScope() }
    DisposableEffect(scope, view) {
        fun updateVisibility() { scope.visible.value = view.isShown && view.hasWindowFocus() }
        val focus = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            scope.visible.value = focused && view.isShown
        }
        val layout = ViewTreeObserver.OnGlobalLayoutListener { updateVisibility() }
        var observedTree: ViewTreeObserver? = null
        fun removeTreeListeners() {
            observedTree?.takeIf { it.isAlive }?.let {
                it.removeOnWindowFocusChangeListener(focus)
                it.removeOnGlobalLayoutListener(layout)
            }
            observedTree = null
        }
        fun observeTree() {
            val tree = view.viewTreeObserver
            if (observedTree !== tree && tree.isAlive) {
                removeTreeListeners()
                tree.addOnWindowFocusChangeListener(focus)
                tree.addOnGlobalLayoutListener(layout)
                observedTree = tree
            }
        }
        val attach = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { observeTree(); updateVisibility() }
            override fun onViewDetachedFromWindow(v: View) {
                scope.visible.value = false
                removeTreeListeners()
            }
        }
        view.addOnAttachStateChangeListener(attach)
        if (view.isAttachedToWindow) observeTree()
        updateVisibility()
        onDispose {
            view.removeOnAttachStateChangeListener(attach)
            removeTreeListeners()
            scope.visible.value = false
            scope.queue.clear()
        }
    }
    LaunchedEffect(scope, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            scope.visible.collectLatest { visible ->
                if (!visible) return@collectLatest
                scope.queue.resume(android.os.SystemClock.uptimeMillis())
                try {
                    while (isActive) {
                        val wait = scope.queue.delayUntilNext(android.os.SystemClock.uptimeMillis())
                        when {
                            wait == Long.MAX_VALUE -> scope.wake.receive()
                            wait > 0L -> withTimeoutOrNull(wait) { scope.wake.receive() }
                            else -> {
                                android.os.Trace.beginSection("DRBEP.sharedProgrammeBatch")
                                try { scope.queue.runDue(android.os.SystemClock.uptimeMillis()) }
                                finally { android.os.Trace.endSection() }
                            }
                        }
                    }
                } finally { scope.queue.pause() }
            }
        }
    }
    CompositionLocalProvider(LocalProgrammeRefreshScope provides scope, content = content)
}
