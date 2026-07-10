package com.drbep.tvplayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TouchControlsComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TouchControlsBarUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TouchControlsBar(model)
        }
    }
}

@Composable
private fun TouchControlsBar(model: TouchControlsBarUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xD411161D), RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        if (model.contextTitle.isNotBlank() || model.contextSubtitle.isNotBlank()) {
            TouchContextHeader(model)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val actionCount = model.actions.size.coerceAtLeast(1)
            val minimumContentWidth =
                (78.dp * actionCount) + (8.dp * (actionCount - 1).coerceAtLeast(0)) + 24.dp
            val shouldCenter = minimumContentWidth <= maxWidth
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = if (shouldCenter) Arrangement.Center else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                model.actions.forEachIndexed { index, item ->
                    TouchControlChip(item = item)
                    if (index < model.actions.lastIndex) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Composable
private fun TouchContextHeader(model: TouchControlsBarUiModel) {
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .background(Color(0xB51B2A38), RoundedCornerShape(16.dp))
            .clickable(enabled = model.onContextClick != null) { model.onContextClick?.run() }
            .tvButtonSemantics(model.onContextClick != null)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = model.contextTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = Color(0xFF9BD0FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            )
            if (model.contextSubtitle.isNotBlank()) {
                BasicText(
                    text = model.contextSubtitle,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        if (model.onContextClick != null) {
            Spacer(modifier = Modifier.width(10.dp))
            BasicText(
                text = stringResource(id = R.string.touch_context_change),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(Color(0xFF2D6EA3), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TouchControlChip(item: ZapActionItem, modifier: Modifier = Modifier) {
    BasicText(
        text = item.label,
        modifier = modifier
            .alpha(if (item.enabled) 1f else 0.45f)
            .background(
                color = if (item.selected) Color(0xFF2D6EA3) else Color(0xFF203246),
                shape = RoundedCornerShape(18.dp)
            )
            .tvButtonSemantics(item.enabled)
            .combinedClickable(
                enabled = item.enabled,
                onClick = { item.onClick?.run() },
                onLongClick = item.onLongClick?.let { { it.run() } }
            )
            .defaultMinSize(minWidth = 78.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = TextStyle(
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
}
