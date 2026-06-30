package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TimelineProgramBlockComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TimelineProgramBlockUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TimelineProgramBlock(model)
        }
    }
}

@Composable
private fun TimelineProgramBlock(model: TimelineProgramBlockUiModel) {
    if (model.empty) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            BasicText(
                text = model.title,
                style = TextStyle(color = Color(0xFFBFD0E6), fontSize = 12.sp)
            )
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (model.statusLabel.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .background(Color(0x3331D0AA), RoundedCornerShape(999.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                BasicText(
                    text = model.statusLabel,
                    style = TextStyle(color = Color(0xFFE9FFF8), fontSize = 8.sp)
                )
            }
        }
        BasicText(
            text = model.title,
            style = TextStyle(color = Color.White, fontSize = 11.sp)
        )
        BasicText(
            text = model.time,
            style = TextStyle(color = Color(0xFFD8E6F5), fontSize = 11.sp)
        )
    }
}
