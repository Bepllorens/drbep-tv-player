package com.drbep.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

/** A shared, non-focusable strip. Loading never changes list focus or row geometry. */
@Composable
internal fun rememberProgrammeMetadata(model: ProgrammeMetadataUiModel?): ProgrammeMetadata {
    var value by remember(model?.key, model?.initial) { mutableStateOf(model?.initial ?: ProgrammeMetadata.EMPTY) }
    LaunchedEffect(model?.key, model?.initial) {
        val loader=model?.loader ?: return@LaunchedEffect
        delay(450)
        try {
            val loaded=runInterruptible(Dispatchers.IO) { loader.call() }
            ensureActive()
            value=loaded
        } catch(cancelled: CancellationException) { throw cancelled }
          catch(_: Exception) { /* Keep cached guide metadata. */ }
    }
    return value
}

@Composable
internal fun ProgrammeMetadataStrip(model: ProgrammeMetadataUiModel?, compact: Boolean = false) {
    if(model==null)return
    ProgrammeMetadataRow(rememberProgrammeMetadata(model),compact)
}

@Composable
internal fun ProgrammeMetadataRow(data: ProgrammeMetadata, compact: Boolean = false) {
    // Reserve one line even while the optional details are loading.
    val size=if(compact) 10.sp else 11.sp
    val genreColor=remember(data.genre) { Color(android.graphics.Color.parseColor(ProgrammePresentation.color(data.genre))) }
    Row(Modifier.fillMaxWidth().height(22.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        if(data.genre.isNotBlank()) BasicText(data.genre,Modifier.weight(1f,fill=false),maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(genreColor,size,FontWeight.SemiBold))
        if(data.year.isNotBlank()) BasicText(data.year,style=TextStyle(Color(0xFFC6D8E4),size))
        if(data.age.isNotBlank()) {
            val ageColor=when(data.age) { "TP" -> Color(0xFF68D5A1); "+18","18" -> Color(0xFFFF8D8D); else -> Color(0xFFF1CB73) }
            BasicText(data.age,Modifier.background(ageColor.copy(alpha=.13f),RoundedCornerShape(4.dp)).padding(horizontal=5.dp,vertical=1.dp),style=TextStyle(ageColor,size,FontWeight.Bold))
        }
        if(data.rating.isNotBlank()) BasicText("★ ${data.rating}",style=TextStyle(Color(0xFFF1CB73),size))
    }
}
