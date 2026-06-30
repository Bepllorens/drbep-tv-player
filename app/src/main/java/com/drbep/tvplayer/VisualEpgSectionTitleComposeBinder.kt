package com.drbep.tvplayer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

object VisualEpgSectionTitleComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, title: String, first: Boolean) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VisualEpgSectionTitle(title, first)
        }
    }
}

@Composable
private fun VisualEpgSectionTitle(title: String, first: Boolean) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    BasicText(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 2.dp,
                top = if (first) 2.dp else if (compact) 10.dp else 14.dp,
                end = 2.dp,
                bottom = if (compact) 6.dp else 8.dp
            ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = Color.White, fontSize = if (compact) 15.sp else 18.sp, fontWeight = FontWeight.Bold)
    )
}
