package com.drbep.tvplayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TouchHomeHubComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TouchHomeHubUiModel) {
        if (composeView == null) return
        composeView.setStableContent("touch-home-hub", model) { currentModel ->
            TouchHomeHub(currentModel)
        }
    }
}

@Composable
private fun TouchHomeHub(model: TouchHomeHubUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OfflineTvTheme.Colors.panelGlass, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicText(
            text = model.title,
            style = TextStyle(color = Color.White, fontSize = if (compact) 15.sp else 16.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = model.subtitle,
            style = TextStyle(color = OfflineTvTheme.Colors.textMuted, fontSize = if (compact) 11.sp else 12.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        TouchHomeSection(model.libraryTitle, OfflineTvTheme.Colors.card.copy(alpha = 0.8f), OfflineTvTheme.Colors.textSoft, OfflineTvTheme.Colors.card, OfflineTvTheme.Colors.chipSelected, model.libraryActions, compact)
        Spacer(modifier = Modifier.height(8.dp))
        TouchHomeSection(model.accessTitle, OfflineTvTheme.Colors.chip.copy(alpha = 0.8f), OfflineTvTheme.Colors.accentGold, OfflineTvTheme.Colors.card, OfflineTvTheme.Colors.focusSurface, model.accessActions, compact)
    }
}

@Composable
private fun TouchHomeSection(
    title: String,
    sectionColor: Color,
    titleColor: Color,
    inactiveColor: Color,
    activeColor: Color,
    actions: List<ZapActionItem>,
    compact: Boolean
) {
    val columns = if (compact) 2 else 4
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(sectionColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = title,
            style = TextStyle(color = titleColor, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        actions.chunked(columns).forEachIndexed { index, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    TouchHomeAction(item, Modifier.weight(1f), inactiveColor, activeColor, compact)
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            if (index != actions.chunked(columns).lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TouchHomeAction(item: ZapActionItem, modifier: Modifier, inactiveColor: Color, activeColor: Color, compact: Boolean) {
    TvActionChip(
        label = item.label,
        enabled = item.enabled,
        selected = item.selected,
        highlighted = item.highlighted,
        modifier = modifier,
        compact = compact,
        onClick = item.onClick,
        onLongClick = item.onLongClick
    )
}
