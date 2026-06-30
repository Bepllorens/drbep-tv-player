package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.viewinterop.AndroidView

object TimelineChannelLabelComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TimelineChannelLabelUiModel, imageBinder: TimelineChannelLabelImageBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TimelineChannelLabel(model, imageBinder)
        }
    }
}

@Composable
private fun TimelineChannelLabel(model: TimelineChannelLabelUiModel, imageBinder: TimelineChannelLabelImageBinder) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A2532))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            modifier = Modifier.size(26.dp),
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
        Spacer(modifier = Modifier.width(8.dp))
        BasicText(
            text = model.name,
            style = TextStyle(color = Color.White, fontSize = 12.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
