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

object SleepTimerWarningComposeBinder {
    @JvmStatic fun bind(view: ComposeView, remaining: java.util.function.Supplier<String>,
        cancel: Runnable, extend: Runnable, close: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            val focus = rememberTvInitialFocusRequester(true)
            var left by remember { mutableStateOf(remaining.get()) }
            LaunchedEffect(Unit) { while (true) { left = remaining.get(); kotlinx.coroutines.delay(1000L) } }
            Box(Modifier.fillMaxSize().tvPanelBackHandler(close).background(Color.Black.copy(alpha = .65f)),
                contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 500.dp).fillMaxWidth().background(Color(0xF20B141E))
                    .padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    BasicText("Temporizador · $left", style = TextStyle(Color.White, 24.sp, FontWeight.Bold))
                    BasicText("DRBEP se cerrará y detendrá la reproducción al terminar la cuenta atrás.",
                        style = TextStyle(OfflineTvTheme.Colors.textSoft, 15.sp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimerAction("Seguir viendo", Modifier.focusRequester(focus), cancel)
                        TimerAction("+15 min", Modifier, extend)
                        TimerAction("Cerrar aviso", Modifier, close)
                    }
                }
            }
        }
    }
}
@Composable private fun TimerAction(label: String, modifier: Modifier, action: Runnable) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.onFocusChanged { focused = it.isFocused }
        .background(if (focused) Color.White else Color.Transparent, OfflineTvTheme.Menu.shape)
        .tvButtonSemantics(true, label).clickable { action.run() }.padding(12.dp)) {
        BasicText(label, style = TextStyle(if (focused) OfflineTvTheme.Colors.focusInk else Color.White, 14.sp, FontWeight.Bold))
    }
}
