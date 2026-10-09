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

object AppUpdateComposeBinder {
    private data class State(val title: String, val message: String, val progress: Int,
        val update: Runnable?, val close: Runnable?, val required: Boolean)

    @JvmStatic fun bind(view: ComposeView, title: String, message: String, progress: Int,
        update: Runnable?, close: Runnable?, required: Boolean) {
        val value = State(title, message, progress, update, close, required)
        @Suppress("UNCHECKED_CAST")
        val existing = view.tag as? MutableState<State>
        if (existing != null) { existing.value = value; return }
        val state = mutableStateOf(value)
        view.tag = state
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            val model = state.value
            val modal = model.update != null
            val back = remember(model.close, model.required) { Runnable { if (!model.required) model.close?.run() } }
            val focus = rememberTvInitialFocusRequester(modal, back)
            Box(Modifier.fillMaxSize().tvPanelBackHandler(back)
                .background(if (modal) Color.Black.copy(alpha = .35f) else Color.Transparent),
                contentAlignment = if (modal) Alignment.Center else Alignment.TopEnd) {
                Column(Modifier.padding(24.dp).widthIn(max = if (modal) 430.dp else 310.dp).fillMaxWidth()
                    .background(OfflineTvTheme.Menu.background).padding(if (modal) 20.dp else 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText(model.title, style = TextStyle(Color.White, if (modal) 21.sp else 16.sp, FontWeight.Bold))
                    BasicText(model.message, style = TextStyle(OfflineTvTheme.Colors.textSoft, if (modal) 14.sp else 12.sp))
                    if (model.progress >= 0) {
                        Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = .16f))) {
                            Box(Modifier.fillMaxWidth(model.progress.coerceIn(0, 100) / 100f)
                                .height(3.dp).background(Color.White))
                        }
                    }
                    if (modal) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        UpdateAction("Actualizar", Modifier.focusRequester(focus), model.update!!)
                        if (!model.required) UpdateAction("Más tarde", Modifier, back)
                    }
                }
            }
        }
    }
}

@Composable private fun UpdateAction(label: String, modifier: Modifier, action: Runnable) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.background(if (focused) Color.White else Color.Transparent, OfflineTvTheme.Menu.shape)
        .onFocusChanged { focused = it.isFocused }.tvButtonSemantics(true, label)
        .clickable { action.run() }.padding(horizontal = 18.dp, vertical = 12.dp)) {
        BasicText(label, style = TextStyle(if (focused) OfflineTvTheme.Colors.focusInk else Color.White, 15.sp, FontWeight.Bold))
    }
}
