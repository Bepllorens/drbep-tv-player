package com.drbep.tvplayer

import android.widget.ImageView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
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
import kotlinx.coroutines.android.awaitFrame

object U7dBrowserComposeBinder {
    private val memory = U7dNavigationMemory()
    @JvmStatic fun bind(view: ComposeView, channelKey: String, title: String, rows: List<U7dBrowserRow>, close: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent { Browser(channelKey, title, rows, close, memory) }
    }
}

private fun U7dBrowserRow.positionKey() = "$day\n$meta\n$title"

@Composable private fun Browser(channelKey: String, title: String, rows: List<U7dBrowserRow>, close: Runnable, memory: U7dNavigationMemory) {
    val days = remember(rows) { rows.sortedByDescending { it.startMs }.map { it.day }.distinct() }
    val saved = remember(rows) { memory.get(channelKey) }
    val initialDay = remember(rows) { saved?.day?.takeIf { it in days } ?: days.firstOrNull().orEmpty() }
    var day by remember(rows) { mutableStateOf(saved?.day?.takeIf { it in days } ?: days.firstOrNull().orEmpty()) }
    val visible = remember(day, rows) { rows.filter { it.day == day }.sortedBy { it.startMs } }
    val restoredRow = remember(day, rows) { visible.firstOrNull { it.positionKey() == memory.rowForDay(channelKey, day) } }
    var selected by remember(day, rows) { mutableStateOf(restoredRow ?: visible.firstOrNull()) }
    val listState = remember(day, rows) { LazyListState(firstVisibleItemIndex = visible.indexOf(restoredRow).coerceAtLeast(0)) }
    var detailMode by remember(day) { mutableStateOf(false) }
    val rowFocus = remember(visible) { visible.associateWith { FocusRequester() } }
    val dayFocus = remember(days) { days.associateWith { FocusRequester() } }
    var pendingDayFocus by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingDayFocus) {
        pendingDayFocus?.let { value ->
            awaitFrame(); awaitFrame()
            dayFocus[value]?.requestFocus()
            pendingDayFocus = null
        }
    }
    var restoreRowFocus by remember { mutableStateOf(false) }
    LaunchedEffect(restoreRowFocus) {
        if (restoreRowFocus) {
            selected?.let { rowFocus[it]?.requestFocus() }
            restoreRowFocus = false
        }
    }
    val back = Runnable { if (detailMode) { detailMode = false; restoreRowFocus = true } else close.run() }
    val initial = rememberTvInitialFocusRequester(days.isNotEmpty(), rows)
    Column(Modifier.fillMaxSize().background(OfflineTvTheme.Menu.background).tvPanelBackHandler(back).padding(28.dp)) {
        BasicText(title, style = TextStyle(color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(18.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(Modifier.width(150.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                BasicText("DÍAS", style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 11.sp))
                Spacer(Modifier.height(4.dp))
                days.forEach { value ->
                    val timestamp = rows.first { it.day == value }.startMs
                    var focus by remember(value) { mutableStateOf(false) }
                    val active = day == value
                    Row(Modifier.fillMaxWidth()
                        .focusRequester(dayFocus.getValue(value))
                        .then(if (saved == null && value == initialDay) Modifier.focusRequester(initial) else Modifier)
                        .focusProperties { if (active) right = selected?.let { rowFocus[it] } ?: FocusRequester.Default }
                        .onFocusChanged { focus = it.isFocused }
                        .border(2.dp, if (focus) OfflineTvTheme.Colors.focus else Color.Transparent, OfflineTvTheme.Menu.shape)
                        .background(if (active) OfflineTvTheme.Menu.selected else Color.Transparent, OfflineTvTheme.Menu.shape)
                        .tvButtonSemantics(true, value, active)
                        .clickable { day = value; pendingDayFocus = value; detailMode = false; memory.save(channelKey, value, memory.rowForDay(channelKey, value)) }
                        .padding(vertical = 12.dp, horizontal = 8.dp)) {
                        Box(Modifier.width(3.dp).height(36.dp).background(if (active) OfflineTvTheme.Colors.accentCyan else Color.Transparent))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            BasicText(U7dDayLabels.primary(timestamp, System.currentTimeMillis(), value), maxLines = 1,
                                style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal))
                            BasicText(U7dDayLabels.secondary(timestamp, value), maxLines = 1,
                                style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 12.sp))
                        }
                    }
                }
            }
            Column(Modifier.weight(1.15f).fillMaxHeight()) {
                BasicText(day, style = TextStyle(color = OfflineTvTheme.Colors.accentCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                BasicText("${visible.size} programas", style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 11.sp))
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(visible) { index, row ->
                        val section = U7dDayLabels.section(row.startMs)
                        if (index == 0 || section != U7dDayLabels.section(visible[index - 1].startMs)) {
                            BasicText(section, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                                style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                        var focus by remember(row) { mutableStateOf(false) }
                        Column(Modifier.fillMaxWidth().height(76.dp)
                            .focusRequester(rowFocus.getValue(row))
                            .then(if (saved != null && day == initialDay && row === (restoredRow ?: visible.firstOrNull())) Modifier.focusRequester(initial) else Modifier)
                            .focusProperties { left = dayFocus.getValue(day) }
                            .border(2.dp, if (focus) OfflineTvTheme.Colors.focus else Color.Transparent, OfflineTvTheme.Menu.shape)
                            .background(if (focus) OfflineTvTheme.Menu.selected else Color.Transparent, OfflineTvTheme.Menu.shape)
                            .onFocusChanged { focus = it.isFocused; if (it.isFocused) { selected = row; detailMode = false; memory.save(channelKey, day, row.positionKey()) } }
                            .tvButtonSemantics(true, row.title)
                            .clickable { selected = row; detailMode = true }.padding(10.dp)) {
                            BasicText(row.meta, style = TextStyle(color = OfflineTvTheme.Colors.accentCyan, fontSize = 12.sp))
                            Spacer(Modifier.height(4.dp))
                            BasicText(row.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
            selected?.let { row ->
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    key(row) {
                        var loadPoster by remember { mutableStateOf(false) }
                        LaunchedEffect(row) { delay(150); loadPoster = true }
                        Box(Modifier.fillMaxWidth().height(140.dp)) {
                            if (loadPoster) AndroidView(factory = { context -> ImageView(context).apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER; row.artwork?.accept(this)
                            } }, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    BasicText(row.title, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold))
                    BasicText(row.meta, style = TextStyle(color = OfflineTvTheme.Colors.accentCyan, fontSize = 12.sp))
                    Spacer(Modifier.height(8.dp))
                    BasicText(row.description, modifier = Modifier.weight(1f), maxLines = 6, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 14.sp))
                    if (detailMode) {
                        val playFocus = rememberTvInitialFocusRequester(true, row)
                        Action("Ver desde el principio", Modifier.fillMaxWidth().focusRequester(playFocus)) { row.play.run() }
                    } else BasicText("OK: ver opciones", style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 12.sp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BasicText("Izquierda: días · Derecha: programas · OK: seleccionar · Atrás: volver",
            style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = 12.sp))
    }
}

@Composable private fun Action(label: String, modifier: Modifier = Modifier, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier.border(2.dp, if (focused) OfflineTvTheme.Colors.focus else Color.Transparent, OfflineTvTheme.Menu.shape)
        .background(if (focused) OfflineTvTheme.Menu.selected else Color.Transparent, OfflineTvTheme.Menu.shape)
        .onFocusChanged { focused = it.isFocused }.tvButtonSemantics(true, label).clickable(onClick = action).padding(12.dp)) {
        BasicText(label, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
}
