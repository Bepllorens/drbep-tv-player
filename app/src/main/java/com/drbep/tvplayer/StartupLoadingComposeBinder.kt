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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
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
    private data class LoadingState(val model: StartupLoadingUiModel?)
    @JvmStatic
    fun bind(composeView: ComposeView?, model: StartupLoadingUiModel) {
        if (composeView == null) return
        setState(composeView, LoadingState(model))
    }

    @JvmStatic
    fun hide(composeView: ComposeView?) {
        if (composeView != null) setState(composeView, LoadingState(null))
    }

    private fun setState(view: ComposeView, state: LoadingState) {
        view.setStableContent("startup-loading", state) { current ->
            // GONE alone keeps the infinite animation alive in an attached ComposeView.
            current.model?.let { StartupLoadingPanel(it) }
        }
    }
}

@Composable
private fun StartupLoadingPanel(model: StartupLoadingUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val motion by rememberInfiniteTransition(label = "startupMotion").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "startupSweep")
    Box(Modifier.fillMaxSize().background(OfflineTvTheme.Menu.background)
        .padding(if (compact) 24.dp else 56.dp), contentAlignment = Alignment.CenterStart) {
        androidx.compose.foundation.layout.Row(
            Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(R.drawable.drbep_menu_logo), "DRBEP TV",
                Modifier.size(if (compact) 76.dp else 112.dp).graphicsLayer { alpha = 0.72f + motion * 0.28f })
            Spacer(Modifier.width(if (compact) 20.dp else 32.dp))
            Column(Modifier.weight(1f)) {
                BasicText(model.title, style = TextStyle(Color.White, if (compact) 24.sp else 30.sp, FontWeight.Bold))
                Spacer(Modifier.height(16.dp))
                BasicText(model.step, style = TextStyle(OfflineTvTheme.Colors.accentCyan, if (compact) 14.sp else 17.sp, FontWeight.Medium))
                Spacer(Modifier.height(8.dp))
                BasicText(model.detail, style = TextStyle(OfflineTvTheme.Colors.textSoft, if (compact) 12.sp else 14.sp))
                Spacer(Modifier.height(22.dp))
                // Indeterminate animation: startup stages do not provide a real percentage.
                androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                    drawLine(Color.White.copy(alpha = 0.12f), androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                        androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = size.height)
                    val start = motion * size.width * 0.72f
                    drawLine(Color(0xFF8CD9F8), androidx.compose.ui.geometry.Offset(start, size.height / 2),
                        androidx.compose.ui.geometry.Offset(start + size.width * 0.28f, size.height / 2), strokeWidth = size.height)
                }
            }
        }
    }
}
