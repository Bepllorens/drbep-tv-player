package com.drbep.tvplayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

object TouchHomeHubComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TouchHomeHubUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TouchHomeHub(model)
        }
    }
}

@Composable
private fun TouchHomeHub(model: TouchHomeHubUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xC4121820), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicText(
            text = model.title,
            style = TextStyle(color = Color.White, fontSize = if (compact) 15.sp else 16.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = model.subtitle,
            style = TextStyle(color = Color(0xFFA8C5DE), fontSize = if (compact) 11.sp else 12.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        TouchHomeSection(model.libraryTitle, Color(0xFF332B5F68), Color(0xFFA8DCE2), Color(0xFF244252), Color(0xFF1F90A2), model.libraryActions, compact)
        Spacer(modifier = Modifier.height(8.dp))
        TouchHomeSection(model.accessTitle, Color(0xFF334A3126), Color(0xFFF1C79A), Color(0xFF4C3427), Color(0xFFB46B29), model.accessActions, compact)
    }
}

@Composable
private fun TouchHomeSection(
    title: String,
    sectionColor: Color,
    titleColor: Color,
    inactiveColor: Color,
    activeColor: Color,
    actions: List<ZapActionItem>,
    compact: Boolean
) {
    val columns = if (compact) 2 else 4
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(sectionColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = title,
            style = TextStyle(color = titleColor, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        actions.chunked(columns).forEachIndexed { index, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    TouchHomeAction(item, Modifier.weight(1f), inactiveColor, activeColor, compact)
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            if (index != actions.chunked(columns).lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TouchHomeAction(item: ZapActionItem, modifier: Modifier, inactiveColor: Color, activeColor: Color, compact: Boolean) {
    BasicText(
        text = item.label,
        modifier = modifier
            .height(48.dp)
            .alpha(if (item.enabled) 1f else 0.45f)
            .background(if (item.selected) activeColor else inactiveColor, RoundedCornerShape(14.dp))
            .tvButtonSemantics(item.enabled)
            .combinedClickable(
                enabled = item.enabled,
                onClick = { item.onClick?.run() },
                onLongClick = item.onLongClick?.let { { it.run() } }
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        style = TextStyle(
            color = Color.White,
            fontSize = if (compact && item.label.length >= 12) 11.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
