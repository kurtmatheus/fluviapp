package br.com.gruponaveg.services.printerservice

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import br.com.gruponaveg.exceptions.BarcodeSizeException
import br.com.gruponaveg.exceptions.QRCodeException
import br.com.gruponaveg.services.printerservice.image.Image
import br.com.gruponaveg.services.printerservice.printer.Printer
import br.com.gruponaveg.services.printerservice.qrcode.QRCodeGenerator
import java.io.File
import java.util.Locale

class PrinterService(
    private val printer: Printer
) {

    init {
        open()
        write(HW_INIT)
    }

    fun print(text: String) {
        write(text.toByteArray())
    }

    fun printLn(text: String) {
        print(text + CARRIAGE_RETURN)
    }

    @JvmOverloads
    fun lineBreak(nbLine: Int = 1) {
        for (i in 0 until nbLine) {
            write(CTL_LF)
        }
    }

    @JvmOverloads
    @Throws(QRCodeException::class)
    fun printQRCode(value: String, size: Int = 150) {
        val q = QRCodeGenerator()
        printImage(q.generate(value, size))
    }

    fun setTextSizeNormal() {
        setTextSize(1, 1)
    }

    fun setTextSize2H() {
        setTextSize(1, 2)
    }

    fun setTextSize2W() {
        setTextSize(2, 1)
    }

    fun setText4Square() {
        setTextSize(2, 2)
    }

    private fun setTextSize(width: Int, height: Int) {
        if (height == 2 && width == 2) {
            write(TXT_NORMAL)
            write(TXT_4SQUARE)
        } else if (height == 2) {
            write(TXT_NORMAL)
            write(TXT_2HEIGHT)
        } else if (width == 2) {
            write(TXT_NORMAL)
            write(TXT_2WIDTH)
        } else {
            write(TXT_NORMAL)
        }
    }

    fun setTextTypeBold() {
        setTextType("B")
    }

    fun setTextTypeUnderline() {
        setTextType("U")
    }

    fun setTextType2Underline() {
        setTextType("U2")
    }

    fun setTextTypeBoldUnderline() {
        setTextType("BU")
    }

    fun setTextTypeBold2Underline() {
        setTextType("BU2")
    }

    fun setTextTypeNormal() {
        setTextType("NORMAL")
    }

    private fun setTextType(type: String) {
        if (type.equals("B", ignoreCase = true)) {
            write(TXT_BOLD_ON)
            write(TXT_UNDERL_OFF)
        } else if (type.equals("U", ignoreCase = true)) {
            write(TXT_BOLD_OFF)
            write(TXT_UNDERL_ON)
        } else if (type.equals("U2", ignoreCase = true)) {
            write(TXT_BOLD_OFF)
            write(TXT_UNDERL2_ON)
        } else if (type.equals("BU", ignoreCase = true)) {
            write(TXT_BOLD_ON)
            write(TXT_UNDERL_ON)
        } else if (type.equals("BU2", ignoreCase = true)) {
            write(TXT_BOLD_ON)
            write(TXT_UNDERL2_ON)
        } else if (type.equals("NORMAL", ignoreCase = true)) {
            write(TXT_BOLD_OFF)
            write(TXT_UNDERL_OFF)
        }
    }

    fun cutPart() {
        cut("PART")
    }

    fun cutFull() {
        cut("FULL")
    }

    private fun cut(mode: String) {
        for (i in 0..4) {
            write(CTL_LF)
        }
        if (mode.uppercase(Locale.getDefault()) == "PART") {
            write(PAPER_PART_CUT)
        } else {
            write(PAPER_FULL_CUT)
        }
    }

    @Throws(BarcodeSizeException::class)
    fun printBarcode(code: String, bc: String, width: Int, height: Int, pos: String, font: String) {
        // Align Bar Code()
        write(TXT_ALIGN_CT)
        // Height
        if (height in 2..6) {
            write(BARCODE_HEIGHT)
        } else {
            throw BarcodeSizeException("Incorrect Height")
        }
        //Width
        if (width in 1..255) {
            write(BARCODE_WIDTH)
        } else {
            throw BarcodeSizeException("Incorrect Width")
        }
        //Font
        if (font.equals("B", ignoreCase = true)) {
            write(BARCODE_FONT_B)
        } else {
            write(BARCODE_FONT_A)
        }
        //Position
        if (pos.equals("OFF", ignoreCase = true)) {
            write(BARCODE_TXT_OFF)
        } else if (pos.equals("BOTH", ignoreCase = true)) {
            write(BARCODE_TXT_BTH)
        } else if (pos.equals("ABOVE", ignoreCase = true)) {
            write(BARCODE_TXT_ABV)
        } else {
            write(BARCODE_TXT_BLW)
        }
        when (bc.uppercase(Locale.getDefault())) {
            "UPC-A" -> write(BARCODE_UPC_A)
            "UPC-E" -> write(BARCODE_UPC_E)
            "EAN13" -> write(BARCODE_EAN13)
            "EAN8" -> write(BARCODE_EAN8)
            "CODE39" -> write(BARCODE_CODE39)
            "ITF" -> write(BARCODE_ITF)
            "NW7" -> write(BARCODE_NW7)
            else -> write(BARCODE_EAN13)
        }
        //Print Code
        if (code != "") {
            write(code.toByteArray())
            write(CTL_LF)
        } else {
            throw BarcodeSizeException("Incorrect Value")
        }
    }

    fun setTextFontA() {
        setTextFont("A")
    }

    fun setTextFontB() {
        setTextFont("B")
    }

    private fun setTextFont(font: String) {
        if (font.equals("B", ignoreCase = true)) {
            write(TXT_FONT_B)
        } else {
            write(TXT_FONT_A)
        }
    }

    fun setTextAlignCenter() {
        setTextAlign("CENTER")
    }

    fun setTextAlignRight() {
        setTextAlign("RIGHT")
    }

    fun setTextAlignLeft() {
        setTextAlign("LEFT")
    }

    private fun setTextAlign(align: String) {
        if (align.equals("CENTER", ignoreCase = true)) {
            write(TXT_ALIGN_CT)
        } else if (align.equals("RIGHT", ignoreCase = true)) {
            write(TXT_ALIGN_RT)
        } else {
            write(TXT_ALIGN_LT)
        }
    }

    fun setTextDensity(density: Int) {
        when (density) {
            0 -> write(PD_N50)
            1 -> write(PD_N37)
            2 -> write(PD_N25)
            3 -> write(PD_N12)
            4 -> write(PD_0)
            5 -> write(PD_P12)
            6 -> write(PD_P25)
            7 -> write(PD_P37)
            8 -> write(PD_P50)
        }
    }

    fun setTextNormal() {
        setTextProperties("LEFT", "A", "NORMAL", 1, 1, 9)
    }

    fun setTextProperties(align: String, font: String, type: String, width: Int, height: Int, density: Int) {
        setTextAlign(align)
        setTextFont(font)
        setTextType(type)
        setTextSize(width, height)
        setTextDensity(density)
    }

    fun printImage(filePath: String) {
        val img = File(filePath)
        printImage(BitmapFactory.decodeFile(img.absolutePath))
    }

    fun printImage(bitmap: Bitmap) {
        val img = Image() // Assuming Image is some printer-related class
        val pixels: Array<IntArray> = img.getPixelsFast(bitmap) // Utilizing the fast pixel retrieval method from earlier
        var y = 0
        while (y < pixels.size) {
            write(LINE_SPACE_24) // Assuming these are constants representing printer commands
            write(SELECT_BIT_IMAGE_MODE)
            write(byteArrayOf((0x00ff and pixels[y].size).toByte(), ((0xff00 and pixels[y].size) shr 8).toByte()))
            for (x in pixels[y].indices) {
                write(img.recollectSlice(y, x, pixels)) // Assuming this method is related to printer image data
            }
            write(CTL_LF) // Assuming these are printer control characters
            y += 24
        }
        //        bus.write(CTL_LF);
        //        bus.write(LINE_SPACE_30);
    }

    fun setCharCode(code: String) {
        when (code) {
            "USA" -> write(CHARCODE_PC437)
            "JIS" -> write(CHARCODE_JIS)
            "MULTILINGUAL" -> write(CHARCODE_PC850)
            "PORTUGUESE" -> write(CHARCODE_PC860)
            "CA_FRENCH" -> write(CHARCODE_PC863)
            "NORDIC" -> write(CHARCODE_PC865)
            "WEST_EUROPE" -> write(CHARCODE_WEU)
            "GREEK" -> write(CHARCODE_GREEK)
            "HEBREW" -> write(CHARCODE_HEBREW)
            "WPC1252" -> write(CHARCODE_PC1252)
            "CIRILLIC2" -> write(CHARCODE_PC866)
            "LATIN2" -> write(CHARCODE_PC852)
            "EURO" -> write(CHARCODE_PC858)
            "THAI42" -> write(CHARCODE_THAI42)
            "THAI11" -> write(CHARCODE_THAI11)
            "THAI13" -> write(CHARCODE_THAI13)
            "THAI14" -> write(CHARCODE_THAI14)
            "THAI16" -> write(CHARCODE_THAI16)
            "THAI17" -> write(CHARCODE_THAI17)
            "THAI18" -> write(CHARCODE_THAI18)
            else -> write(CHARCODE_PC865)
        }
    }

    fun openCashDrawerPin2() {
        write(CD_KICK_2)
    }

    fun openCashDrawerPin5() {
        write(CD_KICK_5)
    }

    private fun open() {
        printer.open()
    }

    fun close() {
        printer.close()
    }

    fun beep() {
        write(BEEPER)
    }

    private fun write(command: ByteArray?) {
        printer.write(command)
    }

    companion object {
        private val CARRIAGE_RETURN = System.lineSeparator()
    }
}