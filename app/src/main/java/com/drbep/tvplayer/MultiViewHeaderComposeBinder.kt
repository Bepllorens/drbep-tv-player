package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MultiViewHeaderComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: MultiViewHeaderUiModel) {
        if (composeView == null) return
        composeView.setStableContent("multiview-header", model) { currentModel ->
            MultiViewHeader(currentModel)
        }
    }
}

@Composable
private fun MultiViewHeader(model: MultiViewHeaderUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvPanelBackHandler(model.onCloseClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicText(
            text = model.title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (model.hint.isNotBlank()) {
            BasicText(
                text = model.hint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = Color(0xFFA8C5DE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        BasicText(
            text = model.closeLabel,
            modifier = Modifier
                .background(Color(0xCC203246), RoundedCornerShape(15.dp))
                .tvButtonSemantics(model.onCloseClick != null)
                .clickable(enabled = model.onCloseClick != null) { model.onCloseClick?.run() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
