package com.drbep.tvplayer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity

object TimelineScaleComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TimelineScaleUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TimelineScaleRow(model)
        }
    }
}

@Composable
private fun TimelineScaleRow(model: TimelineScaleUiModel) {
    val density = LocalDensity.current
    fun pxToDp(px: Int): Dp = with(density) { px.toDp() }

    Row {
        Spacer(modifier = Modifier.width(pxToDp(model.leadingWidthPx)))
        model.slots.forEach { slot ->
            BasicText(
                text = slot.label,
                modifier = Modifier
                    .width(pxToDp(slot.widthPx))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                style = TextStyle(
                    color = Color(slot.textColor),
                    fontSize = 11.sp
                )
            )
        }
    }
}
