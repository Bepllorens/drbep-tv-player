package com.drbep.tvplayer

import androidx.compose.ui.graphics.Color
import java.util.Locale

/** Supplement labels with colour; keyboard focus always keeps its own contrast. */
object ContentStatusColors {
    val live = Color(0xFF79D9A3)
    val pending = Color(0xFFF2CB70)
    val upcoming = Color(0xFF8EB6FF)
    val unavailable = Color(0xFFA3ADBA)
    val error = Color(0xFFFF9299)
    fun label(value:String):Color {
        val s=value.lowercase(Locale.ROOT)
        return when {
            listOf("error","fall","failed").any(s::contains) -> error
            listOf("caduc","expir","cancel","no disponible","unavailable").any(s::contains) -> unavailable
            listOf("pendiente","programad","scheduled","guardad","pending").any(s::contains) -> pending
            listOf("grabando","recording","complet","finaliz","disponible","available","en emisión","ahora","directo").any(s::contains) -> live
            else -> upcoming
        }
    }
}
