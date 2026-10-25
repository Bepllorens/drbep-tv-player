package com.drbep.tvplayer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val channelStyle = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
private val programmeStyle = TextStyle(Color(0xFFB7CAD8), 11.sp)

/** One layout node per row, with a bounded text measurer shared by the TV screen. */
@Composable
internal fun TvChannelRowText(title: String, programme: String, progress: Float?,
    measurer: TextMeasurer, modifier: Modifier) {
    // The focusable parent already exposes the complete channel/programme label.
    Spacer(modifier.drawWithCache {
        android.os.Trace.beginSection("DRBEP.tvRowTextMeasure")
        val channel: androidx.compose.ui.text.TextLayoutResult
        val current: androidx.compose.ui.text.TextLayoutResult
        try {
            val constraints = Constraints(maxWidth = size.width.toInt().coerceAtLeast(1))
            channel = measurer.measure(title, style = channelStyle, maxLines = 1,
                overflow = TextOverflow.Ellipsis, constraints = constraints)
            current = measurer.measure(programme, style = programmeStyle, maxLines = 1,
                overflow = TextOverflow.Ellipsis, constraints = constraints)
        } finally { android.os.Trace.endSection() }
        val gap = 3.dp.toPx()
        val trackHeight = 1.dp.toPx()
        val height = channel.size.height + gap + current.size.height +
            if (progress != null) gap + trackHeight else 0f
        val top = ((size.height - height) / 2f).coerceAtLeast(0f)
        val programmeTop = top + channel.size.height + gap
        onDrawBehind {
            drawText(channel, topLeft = Offset(0f, top))
            drawText(current, topLeft = Offset(0f, programmeTop))
            if (progress != null) {
                val position = Offset(0f, programmeTop + current.size.height + gap)
                drawRect(Color(0xFF354B5B), topLeft = position, size = Size(size.width, trackHeight))
                drawRect(Color(0xFF67DBEA), topLeft = position,
                    size = Size(size.width * progress.coerceIn(0f, 1f), trackHeight))
            }
        }
    })
}
