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

/** A separate focus window keeps the underlying home selection intact when cancelled. */
object HomeExitConfirmationComposeBinder {
    @JvmStatic
    fun bind(view: ComposeView, cancel: Runnable, exit: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            val initialFocus = rememberTvInitialFocusRequester(true, cancel)
            Box(Modifier.fillMaxSize().tvPanelBackHandler(cancel).background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 430.dp).fillMaxWidth().background(OfflineTvTheme.Menu.background)
                    .padding(30.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    BasicText("¿Salir de DRBEP?", style = TextStyle(Color.White, 26.sp, FontWeight.Bold))
                    BasicText("Se detendrá la reproducción y volverás al menú del Fire TV.",
                        style = TextStyle(OfflineTvTheme.Colors.textSoft, 15.sp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExitAction("Seguir viendo", Modifier.focusRequester(initialFocus), cancel)
                        ExitAction("Salir", Modifier, exit)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExitAction(label: String, modifier: Modifier, action: Runnable) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.background(if (focused) Color.White else Color.Transparent, OfflineTvTheme.Menu.shape)
        .onFocusChanged { focused = it.isFocused }.tvButtonSemantics(true, label)
        .clickable { action.run() }.padding(horizontal = 18.dp, vertical = 12.dp)) {
        BasicText(label, style = TextStyle(if (focused) OfflineTvTheme.Colors.focusInk else Color.White, 15.sp, FontWeight.Bold))
    }
}
