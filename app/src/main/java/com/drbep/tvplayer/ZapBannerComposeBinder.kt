package com.drbep.tvplayer

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

object ZapBannerComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView?, model: ZapBannerUiModel, logoBinder: ZapLogoBinder) {
        if (composeView == null) return
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            ZapBanner(model = model, logoBinder = logoBinder)
        }
    }
}

@Composable
private fun ZapBanner(model: ZapBannerUiModel, logoBinder: ZapLogoBinder) {
    val compact = LocalConfiguration.current.screenWidthDp < 600
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xD5121820), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0x337BAFD3), RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(model = model, logoBinder = logoBinder, compact = compact)
            Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChipText(
                        text = model.channelBadge,
                        textColor = Color(0xFFDDEEFF),
                        background = Color(0x264F86A8),
                        modifier = Modifier.wrapContentWidth()
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicText(
                        text = model.channelTitle,
                        style = TextStyle(color = Color.White, fontSize = if (compact) 13.sp else 14.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    if (model.qualityVisible) {
                        Spacer(modifier = Modifier.width(4.dp))
                        ChipText(
                            text = model.qualityText,
                            textColor = Color(0xFFA9D6FF),
                            background = Color(0x264F86A8),
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(if (compact) 3.dp else 4.dp))
                BasicText(
                    text = model.programTitle,
                    style = TextStyle(color = Color.White, fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                BasicText(
                    text = model.programMeta,
                    style = TextStyle(color = Color(0xFFCFE0F4), fontSize = if (compact) 10.sp else 11.sp)
                )
                if (model.nextProgramVisible) {
                    Spacer(modifier = Modifier.height(2.dp))
                    BasicText(
                        text = model.nextProgram,
                        style = TextStyle(color = Color(0xFF94BCE2), fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 5.dp else 6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = model.remainingText,
                style = TextStyle(color = Color(0xFFA7C3DE), fontSize = if (compact) 10.sp else 11.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(Color(0x4058859F), RoundedCornerShape(999.dp))
            ) {
                if (model.progressVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(model.progress.coerceIn(0, 100) / 100f)
                            .height(5.dp)
                            .background(Color(0xFF6AA8FF), RoundedCornerShape(999.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            BasicText(
                text = model.endTimeText,
                style = TextStyle(color = Color(0xFFDDEEFF), fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 5.dp else 6.dp))
        if (compact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                model.actions.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { item ->
                            ZapActionChip(
                                item = item,
                                compact = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                model.actions.forEach { item ->
                    ZapActionChip(
                        item = item,
                        compact = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelLogo(model: ZapBannerUiModel, logoBinder: ZapLogoBinder, compact: Boolean) {
    AndroidView(
        modifier = Modifier.size(if (compact) 50.dp else 56.dp),
        factory = { context ->
            FrameLayout(context).apply {
                background = ContextCompat.getDrawable(context, R.drawable.channel_logo_plate_bg)
                setPadding(1, 1, 1, 1)
                addView(
                    AppCompatImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        contentDescription = context.getString(R.string.channel_logo_content_description)
                    }
                )
            }
        },
        update = { frame ->
            val imageView = frame.getChildAt(0) as ImageView
            val size = if (compact) 48 else 54
            logoBinder.bind(imageView, model.logoUrl, model.channelTitle, size, size)
        }
    )
}

@Composable
private fun ChipText(
    text: String,
    textColor: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(16.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun ZapActionChip(item: ZapActionItem, compact: Boolean, modifier: Modifier = Modifier) {
    val background = if (item.selected) Color(0xFF5D93E8) else Color(0x264F86A8)
    val stroke = if (item.selected) Color(0xFFB8D2FF) else Color(0x5B99C2E0)
    val textColor = if (item.highlighted) Color(0xFFFFE08A) else Color.White
    val fontSize = when {
        compact && item.label.length >= 9 -> 8.sp
        compact -> 9.sp
        item.label.length >= 9 -> 8.sp
        item.label.length >= 7 -> 9.sp
        else -> 10.sp
    }

    Box(
        modifier = modifier
            .height(if (compact) 30.dp else 32.dp)
            .alpha(if (item.enabled) 1f else 0.45f)
            .background(background, RoundedCornerShape(16.dp))
            .border(1.dp, stroke, RoundedCornerShape(16.dp))
            .tvButtonSemantics(item.enabled)
            .clickable(enabled = item.enabled) { item.onClick?.run() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = item.label,
            style = TextStyle(
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = if (compact) 6.dp else 4.dp, vertical = 6.dp)
        )
    }
}
