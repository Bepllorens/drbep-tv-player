package com.drbep.tvplayer

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

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
