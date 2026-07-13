package com.drbep.tvplayer

import android.widget.SeekBar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

object TimeshiftBarComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: TimeshiftBarUiModel) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TimeshiftBar(model)
        }
    }
}

@Composable
private fun TimeshiftBar(model: TimeshiftBarUiModel) {
    var dragging by remember { mutableStateOf(false) }
    var displayLabel by remember { mutableStateOf(model.statusLabel) }
    var displayedProgress by remember { mutableIntStateOf(model.progress) }

    LaunchedEffect(model.statusLabel, model.progress, dragging) {
        if (!dragging) {
            displayLabel = model.statusLabel
            displayedProgress = model.progress
        }
    }

    val latestModel by rememberUpdatedState(model)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xC8121820), RoundedCornerShape(16.dp))
            .border(
                width = if (model.focused) 2.dp else 0.dp,
                color = if (model.focused) Color(0xFF66A7FF) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = displayLabel,
                modifier = Modifier.weight(1f),
                style = TextStyle(color = Color(0xFFDFF0FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            if (model.liveVisible) {
                BasicText(
                    text = stringResource(R.string.timeshift_live_button),
                    modifier = Modifier
                        .background(Color(0xFF203246), RoundedCornerShape(14.dp))
                        .tvButtonSemantics(model.onLiveClick != null)
                        .clickable(enabled = model.onLiveClick != null) { model.onLiveClick?.run() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        AndroidView(
            factory = { context ->
                SeekBar(context).apply {
                    max = 1000
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (!fromUser) return
                            displayedProgress = progress
                            val label = latestModel.previewLabelProvider?.provide(progress)
                            if (label != null) {
                                displayLabel = label
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            dragging = true
                            latestModel.onSeekStart?.run()
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            val progress = seekBar?.progress ?: displayedProgress
                            latestModel.seekCommitHandler?.seekTo(progress)
                            dragging = false
                            latestModel.onSeekEnd?.run()
                        }
                    })
                }
            },
            update = { seekBar ->
                if (!dragging && seekBar.progress != displayedProgress) {
                    seekBar.progress = displayedProgress
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
