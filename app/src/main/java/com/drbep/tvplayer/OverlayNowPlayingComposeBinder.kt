package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object OverlayNowPlayingComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: ChannelOverlayUi.NowPlayingModel) {
        if (composeView == null) return
        composeView.setStableContent("overlay-now-playing", model) { currentModel ->
            OverlayNowPlayingCard(model = currentModel)
        }
    }
}

@Composable
private fun OverlayNowPlayingCard(model: ChannelOverlayUi.NowPlayingModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xCC144A5B), Color(0xA02A3547))),
                RoundedCornerShape(22.dp)
            )
            .border(1.dp, Color(0x5B8EB0C5), RoundedCornerShape(22.dp))
            .padding(if (compact) 8.dp else 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            BasicText(
                text = "VIENDO AHORA",
                style = TextStyle(
                    color = Color(0xFF8FB5D4),
                    fontSize = if (compact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (model.contextLabel.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .wrapContentWidth()
                        .background(Color(0x263E78A0), RoundedCornerShape(999.dp))
                        .padding(start = 3.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)
                ) {
                    if (model.contextInitials.isNotBlank()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(if (compact) 17.dp else 19.dp)
                                .background(Color(model.contextAccentColor), RoundedCornerShape(999.dp))
                        ) {
                            BasicText(
                                text = model.contextInitials,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = if (compact) 7.sp else 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    BasicText(
                        text = model.contextLabel,
                        style = TextStyle(
                            color = Color(0xFFE5F4FF),
                            fontSize = if (compact) 9.sp else 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.padding(top = if (compact) 2.dp else 3.dp))
        BasicText(
            text = model.title,
            style = TextStyle(
                color = Color.White,
                fontSize = if (compact) 12.sp else 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.padding(top = 2.dp))
        BasicText(
            text = model.route,
            style = TextStyle(
                color = Color(0xFFF9FDFF),
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (model.qualityVisible) {
            Spacer(modifier = Modifier.padding(top = 2.dp))
            BasicText(
                text = model.quality,
                style = TextStyle(
                    color = Color(0xFF9BD0FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .wrapContentWidth()
                    .background(Color(0x1A4F86A8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.padding(top = 2.dp))
        BasicText(
            text = model.meta,
            style = TextStyle(
                color = Color(0xFFCFE0F4),
                fontSize = if (compact) 9.sp else 10.sp
            )
        )
        Spacer(modifier = Modifier.padding(top = if (compact) 3.dp else 4.dp))
        BasicText(
            text = model.recent,
            style = TextStyle(
                color = Color(0xFFB8CBDF),
                fontSize = if (compact) 9.sp else 10.sp
            )
        )
    }
}
