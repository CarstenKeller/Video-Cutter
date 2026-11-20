package com.carstenkeller.videocutter.extract

import android.graphics.Bitmap
import java.io.OutputStream

/**
 * Minimaler, bewusst vereinfachter GIF89a-Encoder für kurze Video-Ausschnitte.
 *
 * Eine bewusste Vereinfachung gegenüber einem vollwertigen GIF-Encoder
 * (Begründung: die Alternative wäre deutlich fehleranfälliger und lässt
 * sich in dieser Umgebung nicht auf einem echten Gerät testen, bevor der
 * Nutzer es tut – ein garantiert spezifikationskonformes, wenn auch
 * größeres Ergebnis ist hier die sicherere Wahl):
 *
 * Die LZW-Kompression wird nicht wirklich genutzt: jeder Pixel wird als
 * eigener, fester 9-Bit-Code geschrieben (kein wachsendes Wörterbuch,
 * keine Code-Breiten-Anpassung durch den Encoder selbst). Wichtig dabei:
 * ein konformer Decoder baut sein Wörterbuch trotzdem mit (das ist Teil
 * des Decoder-Algorithmus, unabhängig davon, welche Codes der Encoder
 * tatsächlich benutzt) und erhöht automatisch die Code-Breite, sobald das
 * Wörterbuch 512 Einträge erreicht (2^9) - danach erwartet er 10-Bit-Codes.
 * Ohne Gegenmaßnahme würden Encoder und Decoder nach spätestens 254
 * Pixeln bezüglich der Code-Breite auseinanderlaufen und das Bild wäre ab
 * dort unlesbar (genau das war der Fehler in einer früheren Version
 * dieser Datei: GIFs mit mehr als rund 250 Pixeln ließen sich nicht
 * abspielen). Fix: alle 100 Pixel wird vorsorglich ein Clear-Code
 * gesendet, der das Wörterbuch beim Decoder zurücksetzt, bevor die
 * kritische Schwelle von 512 Einträgen erreicht wird - der Encoder bleibt
 * dadurch dauerhaft bei 9 Bit pro Code.
 *
 * Die Farbpalette selbst ist dagegen *nicht* mehr fest, sondern wird dem
 * Encoder von außen als [palette] übergeben (siehe [MedianCutPalette]) -
 * eine feste, vom Bildinhalt unabhängige Farbaufteilung führte bei normalen
 * Videos zu sichtbarem Banding (Nutzerfeedback: "wenige Farben").
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
    private val palette: MedianCutPalette,
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
        val bytes = ByteArray(256 * 3)
        for (index in 0 until 256) {
            val packed = palette.colors.getOrElse(index) { 0 }
            bytes[index * 3] = ((packed shr 16) and 0xFF).toByte()
            bytes[index * 3 + 1] = ((packed shr 8) and 0xFF).toByte()
            bytes[index * 3 + 2] = (packed and 0xFF).toByte()
        }
        return bytes
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
        var codesSinceClear = 0
        for (pixel in pixels) {
            if (codesSinceClear >= MAX_CODES_BEFORE_CLEAR) {
                packer.writeCode(CLEAR_CODE, CODE_BITS)
                codesSinceClear = 0
            }
            packer.writeCode(palette.indexOf(pixel), CODE_BITS)
            codesSinceClear++
        }
        packer.writeCode(END_CODE, CODE_BITS)
        packer.flush()
    }

    private fun writeLeShort(value: Int) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
    }

    companion object {
        private const val MIN_CODE_SIZE = 8
        private const val CODE_BITS = 9
        private const val CLEAR_CODE = 256
        private const val END_CODE = 257
        // Muss deutlich unter 512-258=254 liegen (Marge für abweichende
        // Decoder-Implementierungsdetails beim genauen Zeitpunkt der
        // Code-Breiten-Erhöhung).
        private const val MAX_CODES_BEFORE_CLEAR = 100
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
