package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.android.awaitFrame

object TvOptionsPanelComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TvOptionsPanelUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TvOptionsPanel(model)
        }
    }
}

@Composable
private fun TvOptionsPanel(model: TvOptionsPanelUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val panelWidth = if (compact) Modifier.fillMaxWidth(0.94f) else Modifier.fillMaxWidth(0.44f)
    val firstRowRequester = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(model) {
        awaitFrame()
        if (!model.rows.isNullOrEmpty()) {
            firstRowRequester.requestFocus()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = panelWidth
                .clip(RoundedCornerShape(if (compact) 22.dp else 28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF21A2634), Color(0xF20B111A))
                    )
                )
                .padding(if (compact) 16.dp else 22.dp)
        ) {
            BasicText(
                text = model.title,
                style = TextStyle(
                    color = Color.White,
                    fontSize = if (compact) 21.sp else 26.sp,
                    fontWeight = FontWeight.Black
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (model.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = model.message,
                    style = TextStyle(color = Color(0xFFC3D2E4), fontSize = if (compact) 12.sp else 14.sp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(if (compact) 14.dp else 18.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 330.dp else 430.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
            ) {
                itemsIndexed(model.rows ?: emptyList()) { index, row ->
                    TvOptionsRow(row, index, compact, if (index == 0) firstRowRequester else null)
                }
            }
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))
            TvOptionsBackButton(model.backLabel, model.onBack, compact)
        }
    }
}

@Composable
private fun TvOptionsRow(row: TvOptionsPanelRowUiModel, index: Int, compact: Boolean, focusRequester: FocusRequester?) {
    var focused by remember { mutableStateOf(false) }
    val background = if (focused) Color(0xFFFFD47A) else Color(0xFF203044)
    val titleColor = if (focused) Color(0xFF101722) else Color.White
    val indexColor = if (focused) Color(0xFF203044) else Color(0xFF93B5D4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 52.dp else 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(row.onClick != null)
            .clickable(enabled = row.onClick != null) { row.onClick?.run() }
            .padding(horizontal = if (compact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 30.dp else 34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (focused) Color(0xFFFFFFFF) else Color(0xFF162234)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = row.indexLabel.ifBlank { (index + 1).toString() },
                style = TextStyle(color = indexColor, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Black)
            )
        }
        Spacer(modifier = Modifier.width(if (compact) 12.dp else 14.dp))
        BasicText(
            text = row.label,
            modifier = Modifier.weight(1f),
            style = TextStyle(color = titleColor, fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        BasicText(
            text = "OK",
            style = TextStyle(color = if (focused) Color(0xFF203044) else Color(0xFF6E8BA8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun TvOptionsBackButton(label: String, onBack: Runnable?, compact: Boolean) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 44.dp else 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) Color(0xFF4F6E91) else Color(0xFF152131))
            .onFocusChanged { focused = it.isFocused }
            .tvButtonSemantics(onBack != null)
            .clickable(enabled = onBack != null) { onBack?.run() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = Color.White, fontSize = if (compact) 14.sp else 15.sp, fontWeight = FontWeight.Bold)
        )
    }
}
