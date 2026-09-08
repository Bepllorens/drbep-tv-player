package com.drbep.tvplayer

import android.widget.ImageView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

object U7dBrowserComposeBinder {
    private val memory = U7dNavigationMemory()
    @JvmStatic fun bind(view: ComposeView, channelKey: String, title: String, rows: List<U7dBrowserRow>, close: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent { Browser(channelKey, title, rows, close, memory) }
    }
}

private fun U7dBrowserRow.positionKey() = "$day\n$meta\n$title"

@Composable private fun Browser(channelKey: String, title: String, rows: List<U7dBrowserRow>, close: Runnable, memory: U7dNavigationMemory) {
    val days = remember(rows) { rows.map { it.day }.distinct() }
    val saved = remember(rows) { memory.get(channelKey) }
    var day by remember(rows) { mutableStateOf(saved?.day?.takeIf { it in days } ?: days.firstOrNull().orEmpty()) }
    val visible = remember(day, rows) { rows.filter { it.day == day } }
    val restoredRow = remember(day, rows) { visible.firstOrNull { it.positionKey() == saved?.rowKey } }
    var selected by remember(day) { mutableStateOf(restoredRow ?: visible.firstOrNull()) }
    val listState = remember(day) { LazyListState(firstVisibleItemIndex = visible.indexOf(restoredRow).coerceAtLeast(0)) }
    var detailMode by remember(day) { mutableStateOf(false) }
    val rowFocus = remember(visible) { visible.associateWith { FocusRequester() } }
    var restoreRowFocus by remember { mutableStateOf(false) }
    LaunchedEffect(restoreRowFocus) {
        if (restoreRowFocus) {
            selected?.let { rowFocus[it]?.requestFocus() }
            restoreRowFocus = false
        }
    }
    val back = Runnable {
        if (detailMode) { detailMode = false; restoreRowFocus = true } else close.run()
    }
    val initial = rememberTvInitialFocusRequester(days.isNotEmpty(), rows)
    Column(Modifier.fillMaxSize().background(OfflineTvTheme.Colors.backdrop)
        .tvPanelBackHandler(back).padding(28.dp)) {
        BasicText(title, style = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { index, value ->
                Action(value, if (restoredRow == null && index == days.indexOf(day)) Modifier.focusRequester(initial) else Modifier,
                    active = day == value) { day = value; detailMode = false; memory.save(channelKey, value, "") }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            LazyColumn(Modifier.weight(1.15f).fillMaxHeight(), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible) { row ->
                    var focus by remember { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().height(84.dp)
                        .border(2.dp, if (focus) OfflineTvTheme.Colors.focus else Color.Transparent, RoundedCornerShape(12.dp))
                        .background(if (focus) OfflineTvTheme.Colors.focusSurface else OfflineTvTheme.Colors.chip, RoundedCornerShape(12.dp))
                        .focusRequester(rowFocus.getValue(row))
                        .then(if (row === restoredRow) Modifier.focusRequester(initial) else Modifier)
                        .onFocusChanged { focus = it.isFocused; if (it.isFocused) { selected = row; detailMode = false; memory.save(channelKey, day, row.positionKey()) } }
                        .clickable { selected = row; detailMode = true }.padding(12.dp)) {
                        BasicText(row.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(4.dp))
                        BasicText(row.meta, style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 12.sp))
                    }
                }
            }
            selected?.let { row ->
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    key(row) {
                        var loadPoster by remember { mutableStateOf(false) }
                        LaunchedEffect(row) { delay(150); loadPoster = true }
                        Box(Modifier.fillMaxWidth().height(150.dp)) {
                            if (loadPoster) AndroidView(factory = { context -> ImageView(context).apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                row.artwork?.accept(this)
                            } }, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    BasicText(row.title, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold))
                    BasicText(row.meta, style = TextStyle(color = OfflineTvTheme.Colors.accentCyan, fontSize = 13.sp))
                    Spacer(Modifier.height(8.dp))
                    BasicText(row.description, modifier = Modifier.weight(1f), maxLines = 7,
                        overflow = TextOverflow.Ellipsis, style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 14.sp))
                    if (detailMode) {
                        val playFocus = rememberTvInitialFocusRequester(true, row)
                        Action("Ver desde el principio", Modifier.fillMaxWidth().focusRequester(playFocus)) { row.play.run() }
                    } else {
                        BasicText("OK: ver opciones", style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 13.sp))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BasicText("Arriba: días · OK: ficha · Atrás: volver", style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 12.sp))
    }
}

@Composable private fun Action(label: String, modifier: Modifier = Modifier, active: Boolean = false, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.border(2.dp, if (focused) OfflineTvTheme.Colors.focus else Color.Transparent, RoundedCornerShape(10.dp))
        .background(if (active || focused) OfflineTvTheme.Colors.focusSurface else OfflineTvTheme.Colors.chip, RoundedCornerShape(10.dp))
        .onFocusChanged { focused = it.isFocused }.clickable(onClick = action).padding(12.dp)) {
        BasicText(label, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
}
