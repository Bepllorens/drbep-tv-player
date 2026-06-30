package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MiniGuideComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: MiniGuideUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            MiniGuidePanel(model)
        }
    }
}

@Composable
private fun MiniGuidePanel(model: MiniGuideUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20B111A))
            .padding(
                horizontal = if (compact) 18.dp else 56.dp,
                vertical = if (compact) 18.dp else 42.dp
            )
    ) {
        PanelHeader(model.title, model.subtitle, compact)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(model.items) { _, item ->
                MiniGuideRow(item)
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String, subtitle: String, compact: Boolean) {
    if (title.isEmpty() && subtitle.isEmpty()) return
    Column(modifier = Modifier.padding(bottom = if (compact) 12.dp else 18.dp)) {
        if (title.isNotEmpty()) {
            BasicText(
                text = title,
                style = TextStyle(color = Color.White, fontSize = if (compact) 20.sp else 28.sp, fontWeight = FontWeight.Bold)
            )
        }
        if (subtitle.isNotEmpty()) {
            BasicText(
                text = subtitle,
                modifier = Modifier.padding(top = if (compact) 5.dp else 8.dp),
                style = TextStyle(color = Color(0xFFB7C5D8), fontSize = if (compact) 12.sp else 14.sp)
            )
        }
    }
}

@Composable
private fun MiniGuideRow(item: MiniGuideProgramRowUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 6.dp else 8.dp)
            .background(Color(0xFF2A2F3A), RoundedCornerShape(14.dp))
            .tvButtonSemantics(item.onClick != null)
            .clickable(enabled = item.onClick != null) { item.onClick?.run() }
            .padding(if (compact) 12.dp else 14.dp)
    ) {
        BasicText(
            text = item.time,
            style = TextStyle(color = Color(0xFF8BC4FF), fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(if (compact) 7.dp else 8.dp))
        Box(
            modifier = Modifier
                .background(Color(item.badgeColor), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicText(
                text = item.badge,
                style = TextStyle(color = Color(0xFFEAF2FF), fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
        BasicText(
            text = item.title,
            style = TextStyle(color = Color.White, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.Bold)
        )
        if (item.progress >= 0) {
            Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0x332B3B4D), RoundedCornerShape(999.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((item.progress.coerceIn(0, 100) / 100f))
                        .height(8.dp)
                        .background(Color(0xFF61B3FF), RoundedCornerShape(999.dp))
                )
            }
        }
        if (item.meta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
            BasicText(
                text = item.meta,
                style = TextStyle(color = Color(0xFFC7D2E2), fontSize = if (compact) 12.sp else 13.sp)
            )
        }
    }
}
