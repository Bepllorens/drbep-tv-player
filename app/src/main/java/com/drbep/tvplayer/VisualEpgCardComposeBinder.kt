package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

object VisualEpgCardComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: VisualEpgCardUiModel, imageBinder: VisualEpgCardImageBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            VisualEpgCard(model, imageBinder)
        }
    }
}

@Composable
private fun VisualEpgCard(model: VisualEpgCardUiModel, imageBinder: VisualEpgCardImageBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    val borderColor = when {
        model.focused -> Color(0xFF68B6FF)
        model.scheduled -> Color(0xFFAF7A21)
        else -> Color(0xFF284156)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = if (model.focused) 1.03f else 1f
                scaleY = if (model.focused) 1.03f else 1f
            }
            .clip(shape)
            .background(if (model.focused) Color(0xFF213447) else Color(0xFF17232F))
            .border(2.dp, borderColor, shape)
            .padding(if (compact) 6.dp else 8.dp)
            .tvButtonSemantics(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 88.dp else 104.dp)
                .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
                .background(Color(0xFF0E1820))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    FrameLayout(context).apply {
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
                    imageBinder.bind(frame.getChildAt(0) as ImageView, model)
                }
            )
            if (model.badgeLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (compact) 4.dp else 6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (model.scheduled) Color(0xCC8E5B16) else Color(0xCC214A72))
                        .padding(horizontal = if (compact) 6.dp else 7.dp, vertical = if (compact) 2.dp else 3.dp)
                ) {
                    BasicText(
                        text = model.badgeLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = Color.White, fontSize = if (compact) 9.sp else 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
        BasicText(
            text = model.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color.White, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(if (compact) 3.dp else 4.dp))
        BasicText(
            text = model.timeLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color(0xFFC9D8E8), fontSize = if (compact) 9.sp else 10.sp)
        )
    }
}
