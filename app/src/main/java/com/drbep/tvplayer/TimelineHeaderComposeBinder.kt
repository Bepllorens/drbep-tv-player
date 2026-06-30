package com.drbep.tvplayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TimelineHeaderComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TimelineHeaderUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TimelineHeader(model)
        }
    }
}

@Composable
private fun TimelineHeader(model: TimelineHeaderUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = model.title,
                style = TextStyle(
                    color = Color.White,
                    fontSize = if (compact) 20.sp else 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (model.windowLabel.isNotEmpty()) {
                BasicText(
                    text = model.windowLabel,
                    modifier = Modifier.padding(top = 8.dp),
                    style = TextStyle(
                        color = Color(0xFFDDEBFA),
                        fontSize = if (compact) 13.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        if (model.actions.isNotEmpty()) {
            Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
            Row {
                model.actions.forEachIndexed { index, action ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
                    }
                    TimelineHeaderAction(action, compact)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineHeaderAction(action: TimelineHeaderUiModel.TimelineHeaderActionUiModel, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    BasicText(
        text = action.label,
        modifier = Modifier
            .alpha(if (focused) 1f else 0.9f)
            .background(if (focused) Color(0xFF2F89C5) else Color(0x264F86A8), RoundedCornerShape(14.dp))
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(action.onClick != null)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                    action.onDown?.run()
                    true
                } else {
                    false
                }
            }
            .combinedClickable(
                enabled = action.onClick != null,
                onClick = { action.onClick?.run() }
            )
            .padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 5.dp else 6.dp
            ),
        style = TextStyle(
            color = Color.White,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )
    )
}
