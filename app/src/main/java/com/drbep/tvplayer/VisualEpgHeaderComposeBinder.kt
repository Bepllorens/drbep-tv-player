package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object VisualEpgHeaderComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: VisualEpgHeaderUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VisualEpgHeader(model)
        }
    }
}

@Composable
private fun VisualEpgHeader(model: VisualEpgHeaderUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            androidx.compose.foundation.text.BasicText(
                text = model.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color.White, fontSize = if (compact) 19.sp else 23.sp, fontWeight = FontWeight.Bold)
            )
            if (model.subtitle.isNotEmpty()) {
                androidx.compose.foundation.text.BasicText(
                    text = model.subtitle,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = Color(0xFFDDEBFA), fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }
        Spacer(modifier = Modifier.size(if (compact) 8.dp else 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
            model.actions.forEach { action ->
                VisualEpgHeaderChip(action, compact)
            }
        }
    }
}

@Composable
private fun VisualEpgHeaderChip(action: VisualEpgHeaderUiModel.VisualEpgHeaderActionUiModel, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    androidx.compose.foundation.text.BasicText(
        text = action.label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (focused) Color(0xFF2E7BD8) else Color(0xFF1C334A))
            .clickable(enabled = action.onClick != null) { action.onClick?.run() }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && action.onDown != null) {
                    action.onDown.run()
                    true
                } else {
                    false
                }
            }
            .tvButtonSemantics(enabled = action.onClick != null)
            .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 7.dp else 9.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = Color.White, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Bold)
    )
}
