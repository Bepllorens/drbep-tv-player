package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.input.key.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsEntry @JvmOverloads constructor(@JvmField val title: String, @JvmField val detail: String,
    @JvmField val value: String, @JvmField val action: Runnable,
    @JvmField val valueProvider: java.util.function.Supplier<String>? = null)
class SettingsCategory(@JvmField val title: String, @JvmField val detail: String,
    @JvmField val entries: List<SettingsEntry>)

object SettingsPanelComposeBinder {
    @JvmStatic fun bind(view: ComposeView, categories: List<SettingsCategory>, version: String,
        selected: Int, onCategory: java.util.function.IntConsumer, initialPins: Set<String>,
        savePins: java.util.function.Consumer<Set<String>>, close: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            var categoryIndex by remember { mutableIntStateOf(selected.coerceIn(categories.indices)) }
            val firstFocus = rememberTvInitialFocusRequester(true)
            val entryFocus = remember { FocusRequester() }
            var query by remember { mutableStateOf("") }
            var pins by remember { mutableStateOf(initialPins.toSet()) }
            val allEntries = remember(categories) { categories.flatMap { it.entries }.distinctBy { SettingsSearchIndex.key(it.title) } }
            val visibleGroups = categories.mapIndexed { index, group ->
                if (index == 0) SettingsCategory(group.title, "Mantén OK sobre una opción para fijarla o quitarla", allEntries.filter { SettingsSearchIndex.key(it.title) in pins }) else group
            }
            val group = visibleGroups[categoryIndex]
            val results = if (query.isBlank()) group.entries else allEntries.filter {
                SettingsSearchIndex.matches(query, it.title + " " + it.detail + " " + it.value)
            }
            val compact = LocalConfiguration.current.screenWidthDp < 600
            Box(Modifier.fillMaxSize().tvPanelBackHandler(close).background(OfflineTvTheme.Menu.scrim)) {
                Column((if (compact) Modifier.fillMaxWidth() else Modifier.width((650f * OfflineTvTheme.menuWidthScale()).dp))
                    .fillMaxHeight().background(Brush.horizontalGradient(
                        0f to Color(0xFF0B141E).copy(alpha = .97f),
                        .86f to Color(0xFF0B141E).copy(alpha = .90f),
                        1f to Color.Transparent)).padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        BasicText("Ajustes", style = TextStyle(Color.White, 27.sp, FontWeight.Bold))
                        BasicText("DRBEP", style = TextStyle(OfflineTvTheme.Colors.textSoft, 13.sp))
                    }
                    Spacer(Modifier.height(8.dp))
                    BasicText("← Categorías   ·   Opciones →   ·   Mantén OK para fijar ★",
                        style = TextStyle(OfflineTvTheme.Colors.textSoft, 12.sp))
                    Spacer(Modifier.height(10.dp))
                    var searchFocused by remember { mutableStateOf(false) }
                    BasicTextField(query, { query = it.take(80) }, singleLine = true,
                        textStyle = TextStyle(Color.White, 15.sp),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { searchFocused = it.isFocused }
                            .background(Color.White.copy(alpha = if (searchFocused) .20f else .08f)).padding(10.dp),
                        decorationBox = { inner ->
                            Box { if (query.isEmpty()) BasicText("Buscar ajustes…", style = TextStyle(OfflineTvTheme.Colors.textSoft, 15.sp)); inner() }
                        })
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        LazyColumn(Modifier.width(if (compact) 140.dp else 168.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemsIndexed(visibleGroups) { index, category ->
                                SettingsRow(category.title, "", "", index == categoryIndex,
                                    (if (index == selected) Modifier.fillMaxWidth().focusRequester(firstFocus) else Modifier.fillMaxWidth())
                                        .focusProperties { if (results.isNotEmpty()) right = entryFocus },
                                    onFocus = { categoryIndex = index; query = ""; onCategory.accept(index) },
                                    action = { categoryIndex = index; query = ""; onCategory.accept(index) })
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            BasicText(if (query.isBlank()) group.title else "Resultados", style = TextStyle(Color.White, 22.sp, FontWeight.Bold))
                            Spacer(Modifier.height(6.dp))
                            BasicText(if (query.isBlank()) group.detail else if (results.size == 1) "1 opción encontrada" else "${results.size} opciones encontradas", style = TextStyle(OfflineTvTheme.Colors.textSoft, 12.sp))
                            Spacer(Modifier.height(16.dp))
                            key(categoryIndex, query) {
                                LazyColumn(state = rememberLazyListState(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (results.isEmpty()) item {
                                        BasicText(if (query.isBlank()) "Fija opciones desde las otras categorías manteniendo OK." else "No hay coincidencias. Prueba con otra palabra.", style = TextStyle(OfflineTvTheme.Colors.textSoft, 14.sp))
                                    }
                                    itemsIndexed(results, key = { _, entry -> SettingsSearchIndex.key(entry.title) }) { entryIndex, entry ->
                                        var liveValue by remember(entry) { mutableStateOf(entry.value) }
                                        LaunchedEffect(entry.valueProvider) {
                                            val provider = entry.valueProvider ?: return@LaunchedEffect
                                            while (true) {
                                                liveValue = provider.get()
                                                kotlinx.coroutines.delay(1000L)
                                            }
                                        }
                                        SettingsRow((if (SettingsSearchIndex.key(entry.title) in pins) "★ " else "") + entry.title, entry.detail, liveValue, false, if (entryIndex == 0) Modifier.fillMaxWidth().focusRequester(entryFocus) else Modifier.fillMaxWidth(),
                                            longAction = {
                                                val id = SettingsSearchIndex.key(entry.title)
                                                pins = if (id in pins) pins - id else pins + id
                                                savePins.accept(pins)
                                            },
                                            action = { entry.action.run() })
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        SettingsRow("Volver", "", "", false, action = { close.run() })
                        BasicText(version, style = TextStyle(OfflineTvTheme.Colors.textSoft, 11.sp))
                    }
                }
            }
        }
    }
}

@Composable private fun SettingsRow(title: String, detail: String, value: String, selected: Boolean,
    modifier: Modifier = Modifier, onFocus: (() -> Unit)? = null, longAction: (() -> Unit)? = null, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }
    val ink = if (focused) OfflineTvTheme.Colors.focusInk else Color.White
    Column(modifier.onPreviewKeyEvent { event ->
        if (longAction == null || event.key.nativeKeyCode !in listOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER)) false
        else {
            val native = event.nativeKeyEvent
            if (event.type == KeyEventType.KeyDown) {
                if (native.repeatCount == 0) held = false
                if (native.isLongPress || native.repeatCount > 0) held = true
            } else if (event.type == KeyEventType.KeyUp && !native.isCanceled) {
                if (held) longAction() else action()
                held = false
            }
            true
        }
    }.onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus?.invoke() }
        .background(if (focused) Color.White else if (selected) Color.White.copy(alpha = .10f) else Color.Transparent,
            OfflineTvTheme.Menu.shape)
        .tvButtonSemantics(true, title).clickable(onClick = action).padding(horizontal = 12.dp, vertical = 11.dp)) {
        BasicText(title, style = TextStyle(ink, 15.sp, FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (value.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            BasicText(value, style = TextStyle(if (focused) ink else Color(0xFF65DCEE), 12.sp, FontWeight.Medium))
        }
        if (detail.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            BasicText(detail, style = TextStyle(if (focused) ink.copy(alpha = .75f) else OfflineTvTheme.Colors.textSoft, 12.sp),
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
