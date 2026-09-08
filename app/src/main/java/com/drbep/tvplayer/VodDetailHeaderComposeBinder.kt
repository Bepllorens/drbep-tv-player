package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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

object VodDetailHeaderComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: VodDetailHeaderUiModel, posterBinder: VodPosterBinder?) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VodDetailHeader(model, posterBinder)
        }
    }
}

@Composable
private fun VodDetailHeader(model: VodDetailHeaderUiModel, posterBinder: VodPosterBinder?) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AndroidView(
            modifier = Modifier
                .size(width = if (compact) 112.dp else 150.dp, height = if (compact) 154.dp else 202.dp)
                .clip(RoundedCornerShape(if (compact) 10.dp else 14.dp))
                .background(OfflineTvTheme.Colors.backdrop),
            factory = { context ->
                FrameLayout(context).apply {
                    addView(
                        AppCompatImageView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            contentDescription = null
                        }
                    )
                }
            },
            update = { frame ->
                posterBinder?.bind(frame.getChildAt(0) as ImageView, model)
            }
        )
        Spacer(modifier = Modifier.width(if (compact) 12.dp else 18.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = model.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color.White, fontSize = if (compact) 19.sp else 23.sp, fontWeight = FontWeight.Bold)
            )
            if (model.meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
                BasicText(
                    text = model.meta,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = OfflineTvTheme.Colors.accentCyan, fontSize = if (compact) 13.sp else 15.sp)
                )
            }
            if (model.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
                BasicText(
                    text = model.description,
                    maxLines = if (compact) 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = OfflineTvTheme.Colors.textSoft, fontSize = if (compact) 12.sp else 14.sp)
                )
            }
            if (model.progressLabel.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2F3A25))
                        .border(1.dp, OfflineTvTheme.Colors.focus, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = if (compact) 9.dp else 10.dp)
                ) {
                    BasicText(
                        text = model.progressLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Color(0xFFFFD082), fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
