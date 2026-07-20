package com.drbep.tvplayer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object OfflineTvTheme {
    object Colors {
        val panelGlass = Color(0xC8121820)
        val panelSoft = Color(0x78222A34)
        val card = Color(0xFF24394D)
        val chip = Color(0xFF2A3440)
        val chipSelected = Color(0xFF2A7C86)
        val focus = Color(0xFF66A7FF)
        val focusSurface = Color(0xFF314B66)
        val textPrimary = Color.White
        val textSoft = Color(0xFFDFF0FF)
        val textMuted = Color(0xFF8FB5D4)
        val textWarning = Color(0xFFFFE08A)
    }

    object Radius {
        val panel = 18.dp
        val bar = 16.dp
        val chip = 14.dp
    }

    object Spacing {
        val xSmall = 4.dp
        val small = 8.dp
        val medium = 12.dp
        val large = 18.dp
        val xLarge = 24.dp
    }

    object Control {
        val phoneHeight = 44.dp
        val tabletHeight = 48.dp
        val tvHeight = 52.dp
        val focusBorder = 3.dp
        const val focusScale = 1.045f
    }

    object Typography {
        val phoneAction = 13.sp
        val tabletAction = 14.sp
        val tvAction = 16.sp
        val body = 16.sp
        val title = 24.sp
    }
}
