package br.com.gruponaveg.services.printerservice.image

import android.graphics.Bitmap


class Image {
    fun getPixelsFast(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        // Retrieve pixel data into the pixels array
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert the 1D pixel array into a 2D array
        val result = Array(height) { IntArray(width) }
        for (row in 0 until height) {
            for (col in 0 until width) {
                result[row][col] = pixels[row * width + col]
            }
        }

        return result
    }

    fun recollectSlice(y: Int, x: Int, img: Array<IntArray>): ByteArray {
        val slices = byteArrayOf(0, 0, 0)
        var yy = y
        var i = 0
        while (yy < y + 24 && i < 3) {
            var slice: Byte = 0
            for (b in 0..7) {
                val yyy = yy + b
                if (yyy >= img.size) {
                    continue
                }
                val col = img[yyy][x]
                val v = shouldPrintColor(col)
                slice = (slice.toInt() or ((if (v) 1 else 0) shl (7 - b)).toByte().toInt()).toByte()
            }
            slices[i] = slice
            yy += 8
            i++
        }

        return slices
    }

    private fun shouldPrintColor(col: Int): Boolean {
        val threshold = 127
        val luminance: Int
        val a = col shr 24 and 0xff
        if (a != 0xff) { // Ignore transparencies
            return false
        }
        val r = col shr 16 and 0xff
        val g = col shr 8 and 0xff
        val b = col and 0xff

        luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

        return luminance < threshold
    }
}