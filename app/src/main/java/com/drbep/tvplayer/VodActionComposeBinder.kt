package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object VodActionComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: VodActionUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VodAction(model)
        }
    }
}

@Composable
private fun VodAction(model: VodActionUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val radius = if (model.primary) 10.dp else 8.dp
    val fill = when {
        model.primary && model.focused -> Color.White
        model.primary -> Color(0xFFFFD782)
        model.focused -> Color(0xFF263E58)
        else -> Color(0xFF172536)
    }
    val stroke = when {
        model.primary && model.focused -> Color(0xFFFFD782)
        model.primary -> Color.White
        model.focused -> Color(0xFF74BFFF)
        else -> Color(0xFF2B4057)
    }
    val textColor = if (model.primary) Color(0xFF111820) else Color.White
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(radius))
            .background(fill)
            .border(if (model.focused) 3.dp else 1.dp, stroke, RoundedCornerShape(radius))
            .tvButtonSemantics()
            .padding(horizontal = if (compact) 10.dp else 14.dp),
        contentAlignment = if (model.primary) Alignment.Center else Alignment.CenterStart
    ) {
        BasicText(
            text = model.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = textColor,
                fontSize = if (model.primary) {
                    if (compact) 13.sp else 15.sp
                } else {
                    if (compact) 14.sp else 16.sp
                },
                fontWeight = FontWeight.Bold
            )
        )
    }
}
