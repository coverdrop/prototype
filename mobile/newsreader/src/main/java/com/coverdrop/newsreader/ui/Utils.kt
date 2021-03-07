package com.coverdrop.newsreader.ui

import android.graphics.*
import com.squareup.picasso.Transformation

class RoundedTransformation(private val radiusDp: Int) : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val paint = Paint().also {
            it.isAntiAlias = true
            it.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val output =
            Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val radius = radiusDp.toFloat()
        canvas.drawRoundRect(
            RectF(0f, 0f, source.width.toFloat(), source.height.toFloat()),
            radius, radius, paint
        )

        if (source != output) source.recycle()
        return output
    }

    override fun key() = "rounded $radiusDp"
}

fun byteArrayToHex(array: ByteArray, useDelimiter: Boolean = false): String {
    val sb = java.lang.StringBuilder(array.size * 2)
    var isFirst = true
    for (b in array) {
        if (!isFirst && useDelimiter) sb.append(':')
        sb.append(String.format("%02X", b))
        isFirst = false
    }

    return sb.toString()
}

fun hexToByteArray(string: String): ByteArray {
    return ByteArray(string.length / 2) {
        string.substring(2 * it, 2 * it + 2).toInt(radix = 16).toByte()
    }
}
