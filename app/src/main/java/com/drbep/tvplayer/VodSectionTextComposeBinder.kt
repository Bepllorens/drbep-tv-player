package com.drbep.tvplayer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

object VodSectionTextComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: VodSectionTextUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VodSectionText(model)
        }
    }
}

@Composable
private fun VodSectionText(model: VodSectionTextUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    BasicText(
        text = model.text,
        modifier = Modifier.fillMaxWidth(),
        maxLines = if (model.title) 1 else 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = if (model.title) Color.White else Color(0xFFB7C4D6),
            fontSize = if (model.title) {
                if (compact) 14.sp else 15.sp
            } else {
                if (compact) 11.sp else 12.sp
            },
            fontWeight = if (model.title) FontWeight.Bold else FontWeight.Normal
        )
    )
}
