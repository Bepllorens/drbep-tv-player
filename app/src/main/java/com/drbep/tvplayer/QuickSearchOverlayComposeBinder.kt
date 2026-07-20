package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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

object QuickSearchOverlayComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: QuickSearchOverlayUiModel) {
        if (composeView == null) return
        composeView.setStableContent("quick-search-overlay", model) { currentModel ->
            QuickSearchOverlay(currentModel)
        }
    }
}

@Composable
private fun QuickSearchOverlay(model: QuickSearchOverlayUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 14.dp else 18.dp))
            .background(Color(0xE0161D27))
            .border(1.dp, Color(0xFF28415C), RoundedCornerShape(if (compact) 14.dp else 18.dp))
            .padding(if (compact) 12.dp else 14.dp)
    ) {
        BasicText(
            text = model.title.uppercase(),
            style = TextStyle(color = Color(0xFF9FD0FF), fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(3.dp))
        BasicText(
            text = model.query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color.White, fontSize = if (compact) 18.sp else 22.sp, fontWeight = FontWeight.Bold)
        )
        if (model.result.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            BasicText(
                text = model.result,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color(0xFFD5E6F8), fontSize = if (compact) 12.sp else 13.sp)
            )
        }
        if (model.hint.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            BasicText(
                text = model.hint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color(0xFF96AFC8), fontSize = if (compact) 11.sp else 12.sp)
            )
        }
    }
}
