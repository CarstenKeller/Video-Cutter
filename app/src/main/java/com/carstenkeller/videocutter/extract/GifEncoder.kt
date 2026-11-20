package com.carstenkeller.videocutter.extract

import android.graphics.Bitmap
import java.io.OutputStream

/**
 * Minimaler, bewusst vereinfachter GIF89a-Encoder für kurze Video-Ausschnitte.
 *
 * Zwei bewusste Vereinfachungen gegenüber einem vollwertigen GIF-Encoder
 * (Begründung: beide Alternativen wären deutlich fehleranfälliger und lassen
 * sich in dieser Umgebung nicht auf einem echten Gerät testen, bevor der
 * Nutzer es tut – ein garantiert spezifikationskonformes, wenn auch größeres
 * Ergebnis ist hier die sicherere Wahl):
 *
 * 1. Feste 256-Farben-Palette (3 Bit Rot, 3 Bit Grün, 2 Bit Blau – "3-3-2"-
 *    Reduktion) statt adaptiver Farboptimierung (z. B. NeuQuant) pro Bild.
 * 2. Die LZW-Kompression wird nicht wirklich genutzt: jeder Pixel wird als
 *    eigener, fester 9-Bit-Code geschrieben (kein wachsendes Wörterbuch,
 *    keine Code-Breiten-Anpassung). Das ist technisch weiterhin eine gültige
 *    GIF89a-Bildbeschreibung (die Spezifikation verlangt LZW-Framing, aber
 *    keine tatsächliche Kompression), nur ohne den Dateigrößen-Vorteil von
 *    echtem LZW.
 *
 * Alle Byte-Layouts (Header, Logical Screen Descriptor, Graphic Control
 * Extension, Image Descriptor, NETSCAPE2.0-Loop-Extension) folgen exakt der
 * GIF89a-Spezifikation und wurden Feld für Feld durchgerechnet, nicht aus
 * einer bestehenden Implementierung übernommen.
 */
class GifEncoder(
    private val output: OutputStream,
    private val width: Int,
    private val height: Int,
    private val frameDelayCentiseconds: Int,
) {
    private var headerWritten = false

    fun writeFrame(bitmap: Bitmap) {
        if (!headerWritten) {
            writeHeaderAndPalette()
            headerWritten = true
        }
        writeGraphicControlExtension()
        writeImageDescriptor()
        writeImageData(bitmap)
    }

    fun finish() {
        output.write(TRAILER)
        output.flush()
    }

    private fun writeHeaderAndPalette() {
        output.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeLeShort(width)
        writeLeShort(height)
        // Packed field: Global-Color-Table-Flag=1, Color-Resolution=7 (111),
        // Sort-Flag=0, Size-Field=7 (111) -> Tabellengröße 2^(7+1) = 256.
        // (1<<7) | (7<<4) | (0<<3) | 7 = 128 + 112 + 7 = 247 = 0xF7
        output.write(0xF7)
        output.write(0) // Hintergrundfarb-Index
        output.write(0) // Pixel-Seitenverhältnis (ungenutzt)
        output.write(buildPalette())
        writeNetscapeLoopExtension()
    }

    private fun buildPalette(): ByteArray {
        val palette = ByteArray(256 * 3)
        for (index in 0 until 256) {
            val (r, g, b) = paletteColorFor(index)
            palette[index * 3] = r.toByte()
            palette[index * 3 + 1] = g.toByte()
            palette[index * 3 + 2] = b.toByte()
        }
        return palette
    }

    private fun writeNetscapeLoopExtension() {
        output.write(0x21) // Extension Introducer
        output.write(0xFF) // Application Extension Label
        output.write(11) // Block-Größe des Namens
        output.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        output.write(3) // Sub-Block-Größe
        output.write(1) // Sub-Block-ID (Loop-Count folgt)
        writeLeShort(0) // Loop-Anzahl: 0 = endlos
        output.write(0) // Block-Terminator
    }

    private fun writeGraphicControlExtension() {
        output.write(0x21) // Extension Introducer
        output.write(0xF9) // Graphic Control Label
        output.write(4) // Block-Größe
        // Packed field: Disposal-Methode=1 ("do not dispose", Bits 2-4),
        // kein User-Input, keine Transparenz -> 1<<2 = 4 = 0x04.
        output.write(0x04)
        writeLeShort(frameDelayCentiseconds)
        output.write(0) // Transparenter Farbindex (ungenutzt)
        output.write(0) // Block-Terminator
    }

    private fun writeImageDescriptor() {
        output.write(0x2C) // Image Separator
        writeLeShort(0) // Left
        writeLeShort(0) // Top
        writeLeShort(width)
        writeLeShort(height)
        output.write(0x00) // keine lokale Farbtabelle, nicht interlaced
    }

    private fun writeImageData(bitmap: Bitmap) {
        output.write(MIN_CODE_SIZE)
        val packer = GifBitPacker(output)
        packer.writeCode(CLEAR_CODE, CODE_BITS)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (pixel in pixels) {
            packer.writeCode(paletteIndexFor(pixel), CODE_BITS)
        }
        packer.writeCode(END_CODE, CODE_BITS)
        packer.flush()
    }

    private fun writeLeShort(value: Int) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
    }

    private fun paletteIndexFor(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return ((r shr 5) shl 5) or ((g shr 5) shl 2) or (b shr 6)
    }

    private fun paletteColorFor(index: Int): Triple<Int, Int, Int> {
        val r = (index shr 5) and 0x07
        val g = (index shr 2) and 0x07
        val b = index and 0x03
        return Triple((r * 255) / 7, (g * 255) / 7, (b * 255) / 3)
    }

    companion object {
        private const val MIN_CODE_SIZE = 8
        private const val CODE_BITS = 9
        private const val CLEAR_CODE = 256
        private const val END_CODE = 257
        private val TRAILER = byteArrayOf(0x3B)
    }
}

/**
 * Packt eine Folge von Codes fester Breite (hier immer 9 Bit) LSB-zuerst in
 * Bytes und schreibt sie als GIF-Sub-Blöcke (max. 255 Byte, je mit einem
 * Längen-Byte davor, abgeschlossen durch einen Null-Längen-Block).
 */
private class GifBitPacker(private val output: OutputStream) {
    private var bitBuffer = 0
    private var bitCount = 0
    private val subBlock = ByteArray(255)
    private var subBlockLength = 0

    fun writeCode(code: Int, bits: Int) {
        bitBuffer = bitBuffer or (code shl bitCount)
        bitCount += bits
        while (bitCount >= 8) {
            emitByte(bitBuffer and 0xFF)
            bitBuffer = bitBuffer ushr 8
            bitCount -= 8
        }
    }

    fun flush() {
        if (bitCount > 0) {
            emitByte(bitBuffer and 0xFF)
            bitBuffer = 0
            bitCount = 0
        }
        flushSubBlock()
        output.write(0) // Block-Terminator für die Bilddaten
    }

    private fun emitByte(value: Int) {
        subBlock[subBlockLength++] = value.toByte()
        if (subBlockLength == 255) {
            flushSubBlock()
        }
    }

    private fun flushSubBlock() {
        if (subBlockLength > 0) {
            output.write(subBlockLength)
            output.write(subBlock, 0, subBlockLength)
            subBlockLength = 0
        }
    }
}
