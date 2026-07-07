package dev.matheus.fluviapp.services.printerservice.qrcode


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import dev.matheus.fluviapp.exceptions.QRCodeException
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

class QRCodeGenerator {
    @JvmOverloads
    @Throws(QRCodeException::class)
    fun generate(textValue: String, size: Int = 150): Bitmap {
        try {
            val hintMap: Map<EncodeHintType, Any> = setEncodingBehavior()

            val bm: BitMatrix = getByteMatrix(textValue, size, hintMap)

            return getImage(bm)
        } catch (e: WriterException) {
            throw QRCodeException("QRCode generation error", e)
        }
    }

    private fun setEncodingBehavior(): Map<EncodeHintType, Any> {
        val hintMap: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
        hintMap[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hintMap[EncodeHintType.MARGIN] = 1
        hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
        return hintMap
    }


    @Throws(WriterException::class)
    private fun getByteMatrix(textValue: String, size: Int, hintMap: Map<EncodeHintType, Any>): BitMatrix {
        val qrCodeWriter = QRCodeWriter()
        return qrCodeWriter.encode(textValue, BarcodeFormat.QR_CODE, size, size, hintMap)
    }


    private fun getImage(bm: BitMatrix): Bitmap {
        val width = bm.width
        val height = bm.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)

        // Fill the bitmap with white color
        canvas.drawColor(Color.WHITE)

        // Draw black pixels where the BitMatrix indicates
        for (i in 0 until width) {
            for (j in 0 until height) {
                if (bm[i, j]) {
                    bitmap.setPixel(i, j, Color.BLACK)
                }
            }
        }

        return bitmap
    }
}