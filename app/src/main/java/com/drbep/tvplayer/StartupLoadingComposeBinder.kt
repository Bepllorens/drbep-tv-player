package com.drbep.tvplayer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object StartupLoadingComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: StartupLoadingUiModel) {
        if (composeView == null) return
        composeView.setStableContent("startup-loading", model) { currentModel ->
            StartupLoadingPanel(currentModel)
        }
    }
}

@Composable
private fun StartupLoadingPanel(model: StartupLoadingUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val pulse by rememberInfiniteTransition(label = "startupPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "startupPulseAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x9A000000))
            .padding(if (compact) 18.dp else 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (compact) 420.dp else 560.dp)
                .fillMaxWidth(if (compact) 0.92f else 0.46f)
                .background(
                    Brush.verticalGradient(listOf(Color(0xF0182636), Color(0xF00A111B))),
                    RoundedCornerShape(if (compact) 22.dp else 28.dp)
                )
                .border(1.dp, Color(0x668EB0C5), RoundedCornerShape(if (compact) 22.dp else 28.dp))
                .padding(if (compact) 18.dp else 24.dp)
        ) {
            BasicText(
                text = model.title,
                style = TextStyle(
                    color = Color.White,
                    fontSize = if (compact) 21.sp else 26.sp,
                    fontWeight = FontWeight.Black
                )
            )
            Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
            BasicText(
                text = model.step,
                style = TextStyle(
                    color = Color(0xFF9BD0FF),
                    fontSize = if (compact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            BasicText(
                text = model.detail,
                style = TextStyle(
                    color = Color(0xFFD5E3F2),
                    fontSize = if (compact) 12.sp else 14.sp
                )
            )
            Spacer(modifier = Modifier.height(if (compact) 14.dp else 18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0x263E78A0), RoundedCornerShape(999.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(4.dp)
                        .alpha(pulse)
                        .background(Color(0xFF6AA8FF), RoundedCornerShape(999.dp))
                )
            }
        }
    }
}
