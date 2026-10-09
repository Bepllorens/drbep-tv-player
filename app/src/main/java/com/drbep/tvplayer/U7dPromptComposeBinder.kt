package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

object U7dPromptComposeBinder {
    @JvmStatic fun bind(view: ComposeView, title: String, message: String, label: String, countdown: Int,
                        primary: Runnable?, live: Runnable?, cancel: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            var seconds by remember { mutableStateOf(countdown) }
            var automatic by remember { mutableStateOf(countdown > 0) }
            val focus = rememberTvInitialFocusRequester(true, title)
            LaunchedEffect(automatic) {
                if (automatic) {
                    while (seconds > 0) { delay(1000); seconds-- }
                    primary?.run()
                }
            }
            Box(Modifier.fillMaxSize().tvPanelBackHandler(cancel), contentAlignment = Alignment.BottomEnd) {
                Column(Modifier.padding(24.dp).widthIn(max = 510.dp).fillMaxWidth()
                    .background(OfflineTvTheme.Menu.background).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BasicText(title, style = TextStyle(Color.White, 22.sp, FontWeight.Bold))
                    BasicText(message, style = TextStyle(OfflineTvTheme.Colors.textSoft, 14.sp))
                    if (automatic) BasicText("Continúa en $seconds s", style = TextStyle(OfflineTvTheme.Colors.accentCyan, 12.sp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (primary != null) PromptAction(label, Modifier.focusRequester(focus), primary) { }
                        if (live != null) PromptAction("Directo", if (primary == null) Modifier.focusRequester(focus) else Modifier, live) { automatic = false }
                        PromptAction("Cancelar", if (primary == null && live == null) Modifier.focusRequester(focus) else Modifier, cancel) { automatic = false }
                    }
                }
            }
        }
    }
}

@Composable private fun PromptAction(label: String, modifier: Modifier, action: Runnable, focusedAction: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.background(if (focused) Color.White else Color.Transparent, OfflineTvTheme.Menu.shape)
        .onFocusChanged { focused = it.isFocused; if (focused) focusedAction() }
        .tvButtonSemantics(true, label).clickable { action.run() }.padding(horizontal = 12.dp, vertical = 10.dp)) {
        BasicText(label, style = TextStyle(if (focused) OfflineTvTheme.Colors.focusInk else Color.White, 13.sp, FontWeight.Bold))
    }
}
