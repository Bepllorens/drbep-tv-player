package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
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
    fun bindFormats(composeView: ComposeView?, label: String) {
        if (composeView == null) return
        composeView.setStableContent("playback-formats", label) { current ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                current.split("  ·  ").filter { it.isNotBlank() }.take(2).forEach { value ->
                    val video = value.contains("VISION") || value.startsWith("HDR")
                    val dolby = value.startsWith("DOLBY ")
                    Row(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color(0xEB10151E))
                            .border(1.dp, Color(0x557B879C), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Canvas(Modifier.size(24.dp)) {
                            val ink = Color(0xFFEAF0FA)
                            val stroke = Stroke(width = 1.5.dp.toPx())
                            if (video) {
                                drawRoundRect(ink, Offset(0f,size.height*.16f), Size(size.width,size.height*.64f),
                                    androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style=stroke)
                                drawLine(ink,Offset(size.width*.3f,size.height*.94f),Offset(size.width*.7f,size.height*.94f),1.5.dp.toPx())
                            } else {
                                drawLine(ink,Offset(size.width*.2f,size.height*.35f),Offset(size.width*.2f,size.height*.65f),3.dp.toPx())
                                drawLine(ink,Offset(size.width*.4f,size.height*.2f),Offset(size.width*.4f,size.height*.8f),3.dp.toPx())
                                drawArc(ink,-65f,130f,false,Offset(size.width*.3f,0f),Size(size.width*.65f,size.height),style=stroke)
                            }
                        }
                        Column(verticalArrangement=Arrangement.spacedBy(1.dp)) {
                            BasicText(if (dolby) "DOLBY" else "VÍDEO",
                                style=TextStyle(color=Color(0xFFB8C5D9),fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.3.sp))
                            BasicText(value.removePrefix("DOLBY "),maxLines=1,
                                style=TextStyle(color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold,letterSpacing=.3.sp))
                        }
                    }
                }
            }
        }
    }
    @JvmStatic
    fun bind(composeView: ComposeView?, model: SurfaceBadgeUiModel) {
        if (composeView == null) return
        composeView.setStableContent("surface-badge", model) { currentModel ->
            SurfaceBadge(currentModel)
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
