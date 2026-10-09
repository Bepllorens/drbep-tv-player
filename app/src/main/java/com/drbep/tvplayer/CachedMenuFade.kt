package com.drbep.tvplayer

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Static menu ramps: a bounded set of tiny textures, with a shader owned by each brush. */
internal object CachedMenuFade {
    private data class Ramp(val horizontal: Boolean, val stops: List<Pair<Float, Int>>)
    private val textures = object : LinkedHashMap<Ramp, Bitmap>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Ramp, Bitmap>?) = size > 16
    }
    fun horizontal(vararg stops: Pair<Float, Color>): Brush = make(true, stops.toList())
    fun vertical(vararg stops: Pair<Float, Color>): Brush = make(false, stops.toList())
    private fun make(horizontal: Boolean, stops: List<Pair<Float, Color>>): Brush {
        val key = Ramp(horizontal, stops.map { it.first to it.second.toArgb() })
        val bitmap = synchronized(textures) {
            textures.getOrPut(key) {
                val w = if (horizontal) 512 else 2
                val h = if (horizontal) 2 else 512
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
                    val paint = Paint().apply {
                        shader = LinearGradient(0f, 0f, if (horizontal) w.toFloat() else 0f,
                            if (horizontal) 0f else h.toFloat(), key.stops.map { it.second }.toIntArray(),
                            key.stops.map { it.first }.toFloatArray(), Shader.TileMode.CLAMP)
                    }
                    Canvas(it).drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                }
            }
        }
        return object : ShaderBrush() {
            override fun createShader(size: Size): Shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply { setScale(size.width.coerceAtLeast(1f) / bitmap.width, size.height.coerceAtLeast(1f) / bitmap.height) })
            }
        }
    }
}
