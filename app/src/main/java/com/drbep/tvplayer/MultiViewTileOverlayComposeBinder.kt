package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
        composeView.setStableContent("multiview-tile", model) { currentModel ->
            MultiViewTileOverlay(currentModel)
        }
    }
}

@Composable
private fun MultiViewTileOverlay(model: MultiViewTileOverlayUiModel) {
    if (!model.visible) return
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .alpha(if (model.active) 1f else 0.9f)
                .background(
                    if (model.active) Color(0xE50A3540) else Color(0xCC172433),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TileText(
                    text = model.slotLabel.ifBlank { "Ventana" },
                    color = if (model.active) Color(0xFFFFD47A) else Color(0xFFAFC8DF),
                    fontSize = 12,
                    modifier = Modifier.weight(1f)
                )
                if (model.active && model.activeLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TilePill(model.activeLabel, Color(0xE0FFD47A), Color(0xFF101722))
                }
            }
            if (model.label.isNotBlank()) {
                TileText(
                    text = model.label,
                    color = Color.White,
                    fontSize = 14,
                    bold = true,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            if (model.active && model.hintLabel.isNotBlank()) {
                TileText(
                    text = model.hintLabel,
                    color = Color(0xFFD1E4F5),
                    fontSize = 11,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
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
private fun TilePill(text: String, background: Color, foreground: Color) {
    BasicText(
        text = text,
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = foreground,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    )
}

@Composable
private fun TileText(
    text: String,
    color: Color,
    fontSize: Int,
    bold: Boolean = false,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color,
            fontSize = fontSize.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    )
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
