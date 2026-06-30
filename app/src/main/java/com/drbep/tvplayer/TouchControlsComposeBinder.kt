package com.drbep.tvplayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TouchControlsComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TouchControlsBarUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TouchControlsBar(model)
        }
    }
}

@Composable
private fun TouchControlsBar(model: TouchControlsBarUiModel) {
    Box(
        modifier = Modifier
            .background(Color(0xAA11161D), RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            model.actions.forEach { item ->
                TouchControlChip(item)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TouchControlChip(item: ZapActionItem) {
    BasicText(
        text = item.label,
        modifier = Modifier
            .alpha(if (item.enabled) 1f else 0.45f)
            .background(
                color = if (item.selected) Color(0xFF2D6EA3) else Color(0xFF203246),
                shape = RoundedCornerShape(18.dp)
            )
            .tvButtonSemantics(item.enabled)
            .combinedClickable(
                enabled = item.enabled,
                onClick = { item.onClick?.run() },
                onLongClick = item.onLongClick?.let { { it.run() } }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = TextStyle(
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    )
}
