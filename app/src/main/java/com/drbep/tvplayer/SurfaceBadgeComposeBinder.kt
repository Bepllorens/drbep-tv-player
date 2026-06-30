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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SurfaceBadgeComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: SurfaceBadgeUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            SurfaceBadge(model)
        }
    }
}

@Composable
private fun SurfaceBadge(model: SurfaceBadgeUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    if (model.fullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(model.backgroundColor))
                .padding(if (compact) 28.dp else 56.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = model.text,
                style = TextStyle(
                    color = Color(model.textColor),
                    fontSize = if (compact) 18.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = if (compact) 25.sp else 34.sp
                )
            )
        }
        return
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(model.backgroundColor))
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(999.dp))
            .padding(
                horizontal = if (model.large) if (compact) 12.dp else 16.dp else 10.dp,
                vertical = if (model.large) if (compact) 7.dp else 9.dp else 5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = model.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = Color(model.textColor),
                fontSize = if (model.large) if (compact) 13.sp else 15.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}
