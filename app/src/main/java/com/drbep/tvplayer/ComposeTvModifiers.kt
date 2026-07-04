package com.drbep.tvplayer

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.android.awaitFrame

fun Modifier.tvButtonSemantics(enabled: Boolean = true): Modifier {
    val focusModifier = if (enabled) Modifier.focusable() else Modifier
    return this
        .then(focusModifier)
        .semantics(mergeDescendants = true) {
            role = Role.Button
            if (!enabled) {
                disabled()
            }
        }
}

@Composable
fun rememberTvInitialFocusRequester(
    enabled: Boolean,
    vararg keys: Any?
): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(enabled, *keys) {
        if (enabled) {
            awaitFrame()
            runCatching { requester.requestFocus() }
        }
    }
    return requester
}

fun Modifier.tvPanelBackHandler(onBack: Runnable?): Modifier {
    if (onBack == null) {
        return this
    }
    return onPreviewKeyEvent { event ->
        val keyCode = event.key.nativeKeyCode
        if (event.type == KeyEventType.KeyUp && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)) {
            onBack.run()
            true
        } else {
            false
        }
    }
}

@Composable
fun TvRequestFocus(
    requester: FocusRequester,
    enabled: Boolean,
    vararg keys: Any?
) {
    LaunchedEffect(enabled, *keys) {
        if (enabled) {
            awaitFrame()
            runCatching { requester.requestFocus() }
        }
    }
}
