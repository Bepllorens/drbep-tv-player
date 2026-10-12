package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
            BoxWithConstraints(Modifier.fillMaxSize().tvPanelBackHandler(back)
                .background(if (modal) Color.Black.copy(alpha = .45f) else Color.Transparent),
                contentAlignment = if (modal) Alignment.Center else Alignment.TopEnd) {
                val availableHeight = (maxHeight - 48.dp).coerceAtLeast(120.dp)
                Column(Modifier.padding(24.dp).widthIn(max = if (modal) 560.dp else 310.dp).fillMaxWidth()
                    .heightIn(max = availableHeight)
                    .background(if (modal) Brush.verticalGradient(listOf(Color(0xFA1A293B), Color(0xF5101926)))
                        else OfflineTvTheme.Menu.background, OfflineTvTheme.Menu.shape)
                    .padding(if (modal) 28.dp else 14.dp)) {
                    BasicText(model.title, style = TextStyle(Color.White, if (modal) 24.sp else 16.sp,
                        FontWeight.Bold, lineHeight = if (modal) 31.sp else 22.sp))
                    Spacer(Modifier.height(if (modal) 22.dp else 8.dp))
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(if (modal) 14.dp else 8.dp)) {
                        model.message.split("\n").filter { it.isNotBlank() }.forEach { line ->
                            val version = modal && (line.startsWith("Instalada:") || line.startsWith("Disponible:"))
                            if (version) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    BasicText(line.substringBefore(":"), Modifier.width(90.dp),
                                        style = TextStyle(OfflineTvTheme.Colors.textSoft, 13.sp, lineHeight = 21.sp))
                                    BasicText(line.substringAfter(":").trim(), Modifier.weight(1f),
                                        style = TextStyle(if (line.startsWith("Disponible:")) Color(0xFF84D9D0) else Color.White,
                                            15.sp, FontWeight.Medium, lineHeight = 22.sp))
                                }
                            } else {
                                if (line.startsWith("•")) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        BasicText("•", style = TextStyle(Color(0xFF84D9D0), 15.sp, lineHeight = 23.sp))
                                        BasicText(line.removePrefix("•").trim(), style = TextStyle(OfflineTvTheme.Colors.textSoft,
                                            if (modal) 15.sp else 12.sp, lineHeight = if (modal) 23.sp else 18.sp))
                                    }
                                } else BasicText(line, style = TextStyle(OfflineTvTheme.Colors.textSoft,
                                    if (modal) 14.sp else 12.sp, lineHeight = if (modal) 22.sp else 18.sp))
                            }
                        }
                    }
                    if (model.progress >= 0) {
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = .16f))) {
                            Box(Modifier.fillMaxWidth(model.progress.coerceIn(0, 100) / 100f)
                                .height(3.dp).background(Color.White))
                        }
                    }
                    if (modal) {
                        Spacer(Modifier.height(26.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            UpdateAction("Actualizar", Modifier.focusRequester(focus), model.update!!)
                            if (!model.required) UpdateAction("Más tarde", Modifier, back)
                        }
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
