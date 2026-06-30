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
private fun TimelineProgramDetailCard(model: TimelineProgramDetailUiModel, imageBinder: TimelineProgramDetailImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xD018202A))
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 7.dp else 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (model.imageUrl.isNotEmpty()) {
            AndroidView(
                modifier = Modifier
                    .size(width = if (compact) 62.dp else 84.dp, height = if (compact) 88.dp else 118.dp)
                    .clip(RoundedCornerShape(10.dp)),
                factory = { context ->
                    FrameLayout(context).apply {
                        setBackgroundColor(0xFF16202A.toInt())
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
                    frame.isVisible = true
                    imageBinder.bind(frame.getChildAt(0) as ImageView, model)
                }
            )
            Spacer(modifier = Modifier.size(if (compact) 10.dp else 12.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.text.BasicText(
                    text = model.title,
                    modifier = Modifier.weight(1f),
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = Color.White, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
                )
                if (model.statusLabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF2D74D6))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        androidx.compose.foundation.text.BasicText(
                            text = model.statusLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(color = Color.White, fontSize = if (compact) 9.sp else 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            if (model.meta.isNotEmpty()) {
                androidx.compose.foundation.text.BasicText(
                    text = model.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = Color(0xFFCFE0F4), fontSize = if (compact) 11.sp else 12.sp)
                )
            }
            if (model.description.isNotEmpty()) {
                androidx.compose.foundation.text.BasicText(
                    text = model.description,
                    maxLines = if (compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = Color(0xFFB7C9DB), fontSize = if (compact) 11.sp else 12.sp)
                )
            }
            if (model.actionHint.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x331D8BFF))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    androidx.compose.foundation.text.BasicText(
                        text = model.actionHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Color(0xFF9EC5FF), fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
