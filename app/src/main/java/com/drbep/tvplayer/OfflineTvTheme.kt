package com.drbep.tvplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object OfflineTvTheme {
    const val PALETTE_AURORA = "aurora"
    const val PALETTE_GRAPHITE = "graphite"
    const val PALETTE_EMERALD = "emerald"
    const val PALETTE_HIGH_CONTRAST = "high-contrast"

    private data class Palette(
        val id: String,
        val backdrop: Color,
        val backdropAccent: Color,
        val surfaceDeep: Color,
        val panelGlass: Color,
        val panelSoft: Color,
        val card: Color,
        val chip: Color,
        val chipSelected: Color,
        val focus: Color,
        val focusInk: Color,
        val accentCyan: Color,
        val accentSecondary: Color,
        val accentGold: Color,
        val textSoft: Color,
        val textMuted: Color,
    )

    private val aurora = Palette(
        id = PALETTE_AURORA,
        backdrop = Color(0xFF090B16),
        backdropAccent = Color(0xFF171128),
        surfaceDeep = Color(0xFF1A1730),
        panelGlass = Color(0xE6171128),
        panelSoft = Color(0xB3251D45),
        card = Color(0xFF3A2857),
        chip = Color(0xFF251D45),
        chipSelected = Color(0xFF7940FF),
        focus = Color(0xFFA889FF),
        focusInk = Color(0xFF171128),
        accentCyan = Color(0xFF66DFF5),
        accentSecondary = Color(0xFF9C6BFF),
        accentGold = Color(0xFFE1C474),
        textSoft = Color(0xFFB9B3CA),
        textMuted = Color(0xFFACA8C0),
    )

    private val graphite = Palette(
        id = PALETTE_GRAPHITE,
        backdrop = Color(0xFF080D12),
        backdropAccent = Color(0xFF101A24),
        surfaceDeep = Color(0xFF0D1821),
        panelGlass = Color(0xE6101A24),
        panelSoft = Color(0xB31A3040),
        card = Color(0xFF1A3040),
        chip = Color(0xFF13232F),
        chipSelected = Color(0xFF276A7C),
        focus = Color(0xFF63E6EB),
        focusInk = Color(0xFF081218),
        accentCyan = Color(0xFF63E6EB),
        accentSecondary = Color(0xFF579CFF),
        accentGold = Color(0xFFE2C46D),
        textSoft = Color(0xFFC0CBD4),
        textMuted = Color(0xFF9DAAB5),
    )

    private val emerald = Palette(
        id = PALETTE_EMERALD,
        backdrop = Color(0xFF07110F),
        backdropAccent = Color(0xFF10221E),
        surfaceDeep = Color(0xFF0C1B17),
        panelGlass = Color(0xE610221E),
        panelSoft = Color(0xB31B382F),
        card = Color(0xFF1B382F),
        chip = Color(0xFF132A24),
        chipSelected = Color(0xFF257B62),
        focus = Color(0xFF61E2B6),
        focusInk = Color(0xFF07110F),
        accentCyan = Color(0xFF76D7FF),
        accentSecondary = Color(0xFF4FBF98),
        accentGold = Color(0xFFE2C875),
        textSoft = Color(0xFFC2D1CC),
        textMuted = Color(0xFF9BAEA8),
    )

    private val highContrast = Palette(
        id = PALETTE_HIGH_CONTRAST,
        backdrop = Color(0xFF000000),
        backdropAccent = Color(0xFF07131D),
        surfaceDeep = Color(0xFF0D141A),
        panelGlass = Color(0xF0000000),
        panelSoft = Color(0xE6111A22),
        card = Color(0xFF1C2832),
        chip = Color(0xFF101820),
        chipSelected = Color(0xFF005FCC),
        focus = Color(0xFFFFD600),
        focusInk = Color(0xFF000000),
        accentCyan = Color(0xFF00E5FF),
        accentSecondary = Color(0xFF78AFFF),
        accentGold = Color(0xFFFFD600),
        textSoft = Color(0xFFF1F5F8),
        textMuted = Color(0xFFD5DEE5),
    )

    private var activePalette by mutableStateOf(aurora)

    @JvmStatic
    fun normalizePaletteId(id: String?): String = when (id?.trim()?.lowercase()) {
        PALETTE_GRAPHITE -> PALETTE_GRAPHITE
        PALETTE_EMERALD -> PALETTE_EMERALD
        PALETTE_HIGH_CONTRAST -> PALETTE_HIGH_CONTRAST
        else -> PALETTE_AURORA
    }

    @JvmStatic
    fun applyPalette(id: String?) {
        activePalette = when (normalizePaletteId(id)) {
            PALETTE_GRAPHITE -> graphite
            PALETTE_EMERALD -> emerald
            PALETTE_HIGH_CONTRAST -> highContrast
            else -> aurora
        }
    }

    @JvmStatic
    fun activePaletteId(): String = activePalette.id

    @JvmStatic
    fun surfaceDeepArgb(): Int = activePalette.surfaceDeep.value.toInt()

    @JvmStatic
    fun focusArgb(): Int = activePalette.focus.value.toInt()

    @JvmStatic
    fun cardArgb(): Int = activePalette.card.value.toInt()

    @JvmStatic
    fun chipSelectedArgb(): Int = activePalette.chipSelected.value.toInt()

    @JvmStatic
    fun accentCyanArgb(): Int = activePalette.accentCyan.value.toInt()

    @JvmStatic
    fun accentSecondaryArgb(): Int = activePalette.accentSecondary.value.toInt()

    @JvmStatic
    fun accentGoldArgb(): Int = activePalette.accentGold.value.toInt()

    @JvmStatic
    fun textSoftArgb(): Int = activePalette.textSoft.value.toInt()

    object Colors {
        val backdrop get() = activePalette.backdrop
        val backdropAccent get() = activePalette.backdropAccent
        val surfaceDeep get() = activePalette.surfaceDeep
        val panelGlass get() = activePalette.panelGlass
        val panelSoft get() = activePalette.panelSoft
        val card get() = activePalette.card
        val chip get() = activePalette.chip
        val chipSelected get() = activePalette.chipSelected
        val focus get() = activePalette.focus
        val focusInk get() = activePalette.focusInk
        val focusSurface get() = activePalette.card
        val accentCyan get() = activePalette.accentCyan
        val accentSecondary get() = activePalette.accentSecondary
        val accentGold get() = activePalette.accentGold
        val textPrimary get() = Color.White
        val textSoft get() = activePalette.textSoft
        val textMuted get() = activePalette.textMuted
        val textWarning get() = activePalette.accentGold
        val overlayScrim = Color(0xCC000000)
        val statusLive = Color(0xFFD5202A)
        val statusSuccess = Color(0xFF80E0A7)
        val statusError = Color(0xFFFF7A8A)
        val statusScheduled = Color(0xFFAF7A21)
        val destructive = Color(0xFF643040)
        val guideBackdrop get() = activePalette.backdrop.copy(alpha = 0.85f)
        val guidePanel get() = activePalette.backdropAccent.copy(alpha = 0.95f)
        val guideChannel get() = activePalette.chip
        val guideTrack get() = activePalette.backdrop
        val guideProgram get() = activePalette.card
        val guideProgramEmpty get() = activePalette.surfaceDeep
        val guideProgramLive = Color(0xFF276B49)
        val guideProgramScheduled = Color(0xFF6E4A16)
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
        val guide = 14.sp
        val guideMeta = 12.sp
        val title = 24.sp
    }
}
