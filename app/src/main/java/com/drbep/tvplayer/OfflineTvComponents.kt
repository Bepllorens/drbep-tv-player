package com.drbep.tvplayer

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
    val configuration = LocalConfiguration.current
    val isTv = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    val isTablet = !isTv && configuration.smallestScreenWidthDp >= 600
    val controlHeight = when {
        isTv -> OfflineTvTheme.Control.tvHeight
        isTablet -> OfflineTvTheme.Control.tabletHeight
        else -> OfflineTvTheme.Control.phoneHeight
    }
    val actionFontSize = when {
        isTv -> OfflineTvTheme.Typography.tvAction
        isTablet -> OfflineTvTheme.Typography.tabletAction
        else -> OfflineTvTheme.Typography.phoneAction
    }
    var focused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(
        targetValue = if (focused && enabled) OfflineTvTheme.Control.focusScale else 1f,
        label = "tvActionFocusScale"
    )
    val background = when {
        focused -> OfflineTvTheme.Colors.focusSurface
        selected -> OfflineTvTheme.Colors.chipSelected
        else -> OfflineTvTheme.Colors.chip
    }
    val textColor = if (highlighted) OfflineTvTheme.Colors.textWarning else OfflineTvTheme.Colors.textPrimary
    BasicText(
        text = label,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .scale(focusScale)
            .heightIn(min = controlHeight)
            .alpha(if (enabled) 1f else 0.45f)
            .border(
                width = if (focused) OfflineTvTheme.Control.focusBorder else 0.dp,
                color = if (focused) OfflineTvTheme.Colors.focus else Color.Transparent,
                shape = RoundedCornerShape(OfflineTvTheme.Radius.chip)
            )
            .background(background, RoundedCornerShape(OfflineTvTheme.Radius.chip))
            .tvButtonSemantics(enabled, label, selected)
            .combinedClickable(
                enabled = enabled,
                onClick = { onClick?.run() },
                onLongClick = onLongClick?.let { { it.run() } }
            )
            .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 8.dp else 10.dp),
        style = TextStyle(
            color = textColor,
            fontSize = actionFontSize,
            fontWeight = FontWeight.Bold,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
