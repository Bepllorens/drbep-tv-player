package com.drbep.tvplayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TvPanel(
    modifier: Modifier = Modifier,
    background: Color = OfflineTvTheme.Colors.panelSoft,
    radius: Dp = OfflineTvTheme.Radius.panel,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.background(background, RoundedCornerShape(radius)),
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvActionChip(
    label: String,
    enabled: Boolean,
    selected: Boolean,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    centered: Boolean = true,
    compact: Boolean = false,
    onClick: Runnable? = null,
    onLongClick: Runnable? = null
) {
    val background = if (selected) OfflineTvTheme.Colors.chipSelected else OfflineTvTheme.Colors.chip
    val textColor = if (highlighted) OfflineTvTheme.Colors.textWarning else OfflineTvTheme.Colors.textPrimary
    BasicText(
        text = label,
        modifier = modifier
            .height(if (compact) 36.dp else 40.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .background(background, RoundedCornerShape(OfflineTvTheme.Radius.chip))
            .tvButtonSemantics(enabled)
            .combinedClickable(
                enabled = enabled,
                onClick = { onClick?.run() },
                onLongClick = onLongClick?.let { { it.run() } }
            )
            .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 8.dp else 10.dp),
        style = TextStyle(
            color = textColor,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
