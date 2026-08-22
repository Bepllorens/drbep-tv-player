package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

object TouchFilterPickerComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TouchFilterPickerUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TouchFilterPicker(model)
        }
    }
}

@Composable
private fun TouchFilterPicker(model: TouchFilterPickerUiModel) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val compact = widthDp < 600
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xB3000000))
            .tvPanelBackHandler(model.onClose)
            .clickable(enabled = model.onClose != null) { model.onClose?.run() }
            .padding(horizontal = if (compact) 14.dp else 42.dp, vertical = if (compact) 16.dp else 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (compact) 1f else 0.78f)
                .clip(RoundedCornerShape(if (compact) 24.dp else 30.dp))
                .background(
                    Brush.verticalGradient(listOf(OfflineTvTheme.Colors.chip.copy(alpha = 0.95f), OfflineTvTheme.Colors.backdrop.copy(alpha = 0.98f)))
                )
                .clickable(enabled = false) {}
                .padding(if (compact) 16.dp else 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = model.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = if (compact) 22.sp else 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    if (model.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicText(
                            text = model.subtitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                color = OfflineTvTheme.Colors.textSoft,
                                fontSize = if (compact) 12.sp else 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                if (model.selectedLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    TouchFilterChip(model.selectedLabel)
                }
            }
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
            val selectedIndex = model.selectedIndex.coerceIn(0, model.rows.lastIndex.coerceAtLeast(0))
            val initialIndex = if (model.selectedIndex >= 0) (selectedIndex - 2).coerceAtLeast(0) else 0
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 330.dp else 450.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
            ) {
                items(model.rows) { row ->
                    TouchFilterRow(row, compact)
                }
            }
        }
    }
}

@Composable
private fun TouchFilterRow(row: TouchFilterPickerRowUiModel, compact: Boolean) {
    val background = if (row.selected) OfflineTvTheme.Colors.focus else OfflineTvTheme.Colors.chip
    val titleColor = if (row.selected) OfflineTvTheme.Colors.backdropAccent else Color.White
    val subtitleColor = if (row.selected) OfflineTvTheme.Colors.backdropAccent else OfflineTvTheme.Colors.textSoft
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .tvButtonSemantics(row.onClick != null)
            .clickable(enabled = row.onClick != null) { row.onClick?.run() }
            .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = row.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = titleColor,
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Black
                )
            )
            if (row.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                BasicText(
                    text = row.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = subtitleColor,
                        fontSize = if (compact) 12.sp else 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        if (row.locked || row.selected) {
            Spacer(modifier = Modifier.width(10.dp))
            TouchFilterChip(if (row.locked) "PIN" else "Activo")
        }
    }
}

@Composable
private fun TouchFilterChip(text: String) {
    BasicText(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(OfflineTvTheme.Colors.card.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = TextStyle(
            color = OfflineTvTheme.Colors.textSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    )
}
