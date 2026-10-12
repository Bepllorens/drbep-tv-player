package com.drbep.tvplayer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Same hierarchy for guide and catchup; reserved lines keep neighbouring actions still. */
@Composable
internal fun ProgrammeSummary(title: String, metadata: String, compact: Boolean = false) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(title, Modifier.fillMaxWidth().height(if(compact) 42.dp else 54.dp), maxLines=2,
            overflow=TextOverflow.Ellipsis, style=TextStyle(color=Color.White,fontSize=if(compact) 16.sp else 21.sp,fontWeight=FontWeight.Bold))
        BasicText(metadata, Modifier.fillMaxWidth().height(38.dp), maxLines=2,
            overflow=TextOverflow.Ellipsis, style=TextStyle(color=Color(android.graphics.Color.parseColor(ProgrammePresentation.color(metadata))),fontSize=12.sp))
    }
}
