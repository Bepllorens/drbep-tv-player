package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.android.awaitFrame

object PlaybackDiagnosticsPanelComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: PlaybackDiagnosticsPanelUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            PlaybackDiagnosticsPanel(model)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PlaybackDiagnosticsPanel(model: PlaybackDiagnosticsPanelUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val firstActionRequester = remember { FocusRequester() }
    LaunchedEffect(model) {
        awaitFrame()
        if (!model.actions.isNullOrEmpty()) {
            firstActionRequester.requestFocus()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (compact) 0.95f else 0.68f)
                .fillMaxHeight(if (compact) 0.86f else 0.80f)
                .clip(RoundedCornerShape(if (compact) 22.dp else 30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xF21A2634), Color(0xF209111A))))
                .padding(if (compact) 16.dp else 24.dp)
        ) {
            BasicText(
                text = model.title,
                style = TextStyle(color = Color.White, fontSize = if (compact) 21.sp else 27.sp, fontWeight = FontWeight.Black),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (model.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                BasicText(
                    text = model.subtitle,
                    style = TextStyle(color = Color(0xFF8FB5D9), fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (model.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF20344A))
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    BasicText(
                        text = model.summary,
                        style = TextStyle(color = Color(0xFFE2EEF8), fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Bold),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
            ) {
                groupedRows(model.rows ?: emptyList()).forEach { group ->
                    item(key = "section-${group.section}") {
                        DiagnosticsSection(group.section, group.rows, compact)
                    }
                }
                if (!model.notes.isNullOrEmpty()) {
                    item(key = "notes") {
                        DiagnosticsNotes(model.notes, compact)
                    }
                }
            }
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
            ) {
                (model.actions ?: emptyList()).forEachIndexed { index, action ->
                    DiagnosticsActionButton(
                        action = action,
                        compact = compact,
                        modifier = Modifier.weight(1f),
                        requester = if (index == 0) firstActionRequester else null
                    )
                }
            }
        }
    }
}

private data class DiagnosticsRowGroup(val section: String, val rows: List<PlaybackDiagnosticsRowUiModel>)

private fun groupedRows(rows: List<PlaybackDiagnosticsRowUiModel>): List<DiagnosticsRowGroup> {
    if (rows.isEmpty()) return emptyList()
    val groups = linkedMapOf<String, MutableList<PlaybackDiagnosticsRowUiModel>>()
    rows.forEach { row ->
        val section = row.section.ifBlank { "Detalle" }
        groups.getOrPut(section) { mutableListOf() }.add(row)
    }
    return groups.map { DiagnosticsRowGroup(it.key, it.value) }
}

@Composable
private fun DiagnosticsSection(section: String, rows: List<PlaybackDiagnosticsRowUiModel>, compact: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xAA101A26))
            .padding(if (compact) 12.dp else 15.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)
    ) {
        BasicText(
            text = section.uppercase(),
            style = TextStyle(color = Color(0xFF76A9D8), fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        rows.forEach { row ->
            DiagnosticsRow(row, compact)
        }
    }
}

@Composable
private fun DiagnosticsRow(row: PlaybackDiagnosticsRowUiModel, compact: Boolean) {
    val toneColor = when (row.tone.lowercase()) {
        "error" -> Color(0xFFFF7A8A)
        "warn" -> Color(0xFFFFD47A)
        "ok" -> Color(0xFF80E0A7)
        else -> Color(0xFFD5E3F2)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        BasicText(
            text = row.label,
            modifier = Modifier.weight(if (compact) 0.42f else 0.34f),
            style = TextStyle(color = Color(0xFF91A8BF), fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        BasicText(
            text = row.value.ifBlank { "-" },
            modifier = Modifier.weight(1f),
            style = TextStyle(color = toneColor, fontSize = if (compact) 12.sp else 14.sp, fontWeight = if (row.tone.isBlank()) FontWeight.Normal else FontWeight.Bold),
            maxLines = if (compact) 3 else 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DiagnosticsNotes(notes: List<String>, compact: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x77203044))
            .padding(if (compact) 12.dp else 15.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)
    ) {
        notes.forEach { note ->
            BasicText(
                text = note,
                style = TextStyle(color = Color(0xFFC7D8EA), fontSize = if (compact) 12.sp else 13.sp),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiagnosticsActionButton(action: TvMessageActionUiModel, compact: Boolean, modifier: Modifier, requester: FocusRequester?) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        focused -> Color(0xFFFFD47A)
        action.destructive -> Color(0xFF683142)
        else -> Color(0xFF203044)
    }
    val textColor = if (focused) Color(0xFF101722) else Color.White
    Box(
        modifier = modifier
            .height(if (compact) 44.dp else 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(action.onClick != null)
            .clickable(enabled = action.onClick != null) { action.onClick?.run() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = action.label,
            style = TextStyle(color = textColor, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
