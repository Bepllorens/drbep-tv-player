package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MultiViewTileOverlayComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: MultiViewTileOverlayUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            MultiViewTileOverlay(model)
        }
    }
}

@Composable
private fun MultiViewTileOverlay(model: MultiViewTileOverlayUiModel) {
    if (!model.visible) return
    Box(modifier = Modifier.fillMaxSize()) {
        TileChip(
            text = model.label,
            color = if (model.active) Color(0xCC0E3E46) else Color(0xCC243447),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .alpha(if (model.active) 1f else 0.9f)
        )
        if (model.audioVisible && model.audioLabel.isNotBlank()) {
            TileChip(
                text = model.audioLabel,
                color = Color(0xCCB77C12),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.TileChip(text: String, color: Color, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier
            .background(color, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    )
}
