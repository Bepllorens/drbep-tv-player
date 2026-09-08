package com.drbep.tvplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object VodActionsHeaderComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: VodActionsHeaderUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VodActionsHeader(model)
        }
    }
}

@Composable
private fun VodActionsHeader(model: VodActionsHeaderUiModel) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = model.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color.White, fontSize = if (compact) 18.sp else 20.sp, fontWeight = FontWeight.Bold)
        )
        if (model.meta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
            BasicText(
                text = model.meta,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 12.sp else 13.sp)
            )
        }
    }
}
