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

object PlaybackIdleWarningComposeBinder {
    @JvmStatic fun bind(view: ComposeView, remaining: java.util.function.Supplier<String>, keepWatching: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            val focus = rememberTvInitialFocusRequester(true)
            var seconds by remember { mutableStateOf(remaining.get()) }
            LaunchedEffect(Unit) { while (true) { seconds = remaining.get(); kotlinx.coroutines.delay(1000L) } }
            Box(Modifier.fillMaxSize().tvPanelBackHandler(keepWatching).background(Color.Black.copy(alpha=.55f)), contentAlignment=Alignment.Center) {
                Column(Modifier.widthIn(max=500.dp).fillMaxWidth().background(Color(0xF20B141E)).padding(28.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    BasicText("¿Sigues viendo?", style=TextStyle(Color.White,24.sp,FontWeight.Bold))
                    BasicText("Pulsa una tecla o toca la pantalla para continuar. DRBEP detendrá la reproducción en $seconds segundos.", style=TextStyle(OfflineTvTheme.Colors.textSoft,15.sp))
                    var focused by remember { mutableStateOf(false) }
                    Box(Modifier.focusRequester(focus).onFocusChanged {focused=it.isFocused}
                        .background(if(focused) Color.White else Color.Transparent,OfflineTvTheme.Menu.shape)
                        .tvButtonSemantics(true,"Seguir viendo").clickable {keepWatching.run()}.padding(12.dp)) {
                        BasicText("Seguir viendo",style=TextStyle(if(focused) OfflineTvTheme.Colors.focusInk else Color.White,15.sp,FontWeight.Bold))
                    }
                }
            }
        }
    }
}
