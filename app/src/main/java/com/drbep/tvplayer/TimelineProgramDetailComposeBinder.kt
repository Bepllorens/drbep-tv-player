package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible

object TimelineProgramDetailComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TimelineProgramDetailUiModel, imageBinder: TimelineProgramDetailImageBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TimelineProgramDetailCard(model, imageBinder)
        }
    }
}

@Composable
internal fun TimelineProgramDetailCard(model: TimelineProgramDetailUiModel, imageBinder: TimelineProgramDetailImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val metadata=rememberProgrammeMetadata(model.programmeMetadata)
    var imageUrl by remember(model) { mutableStateOf(model.imageUrl) }
    LaunchedEffect(model) {
        val loader = model.artworkLoader ?: return@LaunchedEffect
        delay(350)
        try {
            val loaded = kotlinx.coroutines.runInterruptible(Dispatchers.IO) { loader.call() }
            if (!loaded.isNullOrBlank()) imageUrl = loaded
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { /* Keep the fallback without blocking navigation. */ }
    }
    val imageModel = remember(model, imageUrl) {
        TimelineProgramDetailUiModel(model.title, model.meta, model.description, imageUrl)
    }

    var description by remember(model) { mutableStateOf(model.description) }
    LaunchedEffect(model) {
        val loader = model.descriptionLoader ?: return@LaunchedEffect
        delay(350) // Moving through the grid must not make a request for every crossed cell.
        try {
            val loaded = withContext(Dispatchers.IO) { loader.call() }
            if (!loaded.isNullOrBlank()) description = loaded
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { /* Keep the supplied guide text when offline. */ }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 112.dp else 108.dp)
            .background(OfflineTvTheme.Menu.background)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (imageUrl.isNotEmpty() || model.artworkLoader != null) {
            AndroidView(
                modifier = Modifier
                    .size(width = if (compact) 62.dp else 124.dp, height = if (compact) 88.dp else 76.dp)
                    .clip(OfflineTvTheme.Menu.shape),
                factory = { context ->
                    FrameLayout(context).apply {
                        setBackgroundColor(OfflineTvTheme.surfaceDeepArgb())
                        addView(
                            AppCompatImageView(context).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                contentDescription = null
                            }
                        )
                    }
                },
                update = { frame ->
                    frame.isVisible = true
                    imageBinder.bind(frame.getChildAt(0) as ImageView, imageModel)
                }
            )
            Spacer(modifier = Modifier.size(if (compact) 10.dp else 12.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            androidx.compose.foundation.text.BasicText(
                text=model.title, maxLines=1, overflow=TextOverflow.Ellipsis,
                style=TextStyle(color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold))
            androidx.compose.foundation.text.BasicText(
                text=model.meta, maxLines=1, overflow=TextOverflow.Ellipsis,
                style=TextStyle(color=OfflineTvTheme.Colors.textSoft,fontSize=11.sp))
            if(model.programmeMetadata!=null) ProgrammeMetadataRow(metadata,true)
            androidx.compose.foundation.text.BasicText(
                text=metadata.description.ifBlank { description }, modifier=Modifier.height(if(model.programmeMetadata==null) 32.dp else 16.dp), maxLines=if(model.programmeMetadata==null) 2 else 1,
                overflow=TextOverflow.Ellipsis,
                style=TextStyle(color=OfflineTvTheme.Colors.textMuted,fontSize=12.sp,lineHeight=16.sp))
            if (model.actionHint.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(OfflineTvTheme.Menu.shape)
                        .background(OfflineTvTheme.Menu.row)
                        .padding(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    androidx.compose.foundation.text.BasicText(
                        text = model.actionHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = OfflineTvTheme.Colors.accentCyan, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
