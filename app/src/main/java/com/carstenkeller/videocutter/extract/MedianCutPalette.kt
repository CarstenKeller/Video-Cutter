package com.carstenkeller.videocutter.extract

/**
 * Median-Cut-Farbreduktion auf [maxColors] Farben, aus einer Stichprobe
 * tatsächlich im Video vorkommender Pixel gebaut.
 *
 * Ersetzt die vorherige feste 3-3-2-Bit-Farbaufteilung (8 Rot-, 8 Grün-, nur
 * 4 Blau-Stufen), die unabhängig vom tatsächlichen Bildinhalt war und bei
 * normalen Videoaufnahmen zu sichtbarem Banding und "wenigen wirksamen
 * Farben" führte (Nutzerfeedback) - die 256 verfügbaren Palette-Plätze
 * wurden dabei größtenteils auf Farbtöne verteilt, die im Bild gar nicht
 * vorkamen. Median-Cut verteilt die Plätze stattdessen nach tatsächlich
 * vorkommenden Farben: wiederholtes Teilen der Pixel-Punktwolke entlang der
 * jeweils breitesten Farbachse (R, G oder B) am Median, bis [maxColors]
 * Teilmengen ("Boxen") entstanden sind; jede Box liefert eine Palettenfarbe
 * (ihren Mittelwert).
 *
 * Der beim Bauen entstehende Baum der Teilungsentscheidungen wird direkt als
 * Klassifizierungsbaum für [indexOf] weiterverwendet, statt bei jedem Pixel
 * den Abstand zu allen 256 Palettenfarben zu berechnen (bei hunderttausenden
 * Pixeln pro Bild zu teuer) - ein Baum-Durchlauf braucht nur wenige
 * Vergleiche (Tiefe ~log2(maxColors)).
 */
class MedianCutPalette private constructor(
    private val root: Node,
    val colors: IntArray,
) {
    /** Palette-Index (0 bis colors.size-1) für ein Pixel im ARGB_8888-Format. */
    fun indexOf(pixel: Int): Int {
        var node = root
        while (node is Node.Split) {
            val value = channelValue(pixel, node.channelIndex)
            node = if (value <= node.threshold) node.left else node.right
        }
        return (node as Node.Leaf).index
    }

    private sealed class Node {
        class Split(val channelIndex: Int, val threshold: Int, val left: Node, val right: Node) : Node()
        class Leaf(val index: Int) : Node()
    }

    companion object {
        private const val CHANNEL_R = 0
        private const val CHANNEL_G = 1
        private const val CHANNEL_B = 2

        private fun channelValue(pixel: Int, channelIndex: Int): Int = when (channelIndex) {
            CHANNEL_R -> (pixel shr 16) and 0xFF
            CHANNEL_G -> (pixel shr 8) and 0xFF
            else -> pixel and 0xFF
        }

        /** [samplePixels] sollte eine Stichprobe tatsächlicher Bildpixel sein (ARGB_8888). */
        fun build(samplePixels: IntArray, maxColors: Int = 256): MedianCutPalette {
            val colors = IntArray(maxColors)
            var nextIndex = 0

            fun average(points: List<Int>): Int {
                if (points.isEmpty()) return 0
                var r = 0L
                var g = 0L
                var b = 0L
                for (p in points) {
                    r += channelValue(p, CHANNEL_R)
                    g += channelValue(p, CHANNEL_G)
                    b += channelValue(p, CHANNEL_B)
                }
                val n = points.size
                return (((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt())
            }

            fun leaf(points: List<Int>): Node.Leaf {
                val index = nextIndex.coerceAtMost(maxColors - 1)
                colors[index] = average(points)
                nextIndex++
                return Node.Leaf(index)
            }

            fun widestChannel(points: List<Int>): Int {
                val rRange = points.maxOf { channelValue(it, CHANNEL_R) } - points.minOf { channelValue(it, CHANNEL_R) }
                val gRange = points.maxOf { channelValue(it, CHANNEL_G) } - points.minOf { channelValue(it, CHANNEL_G) }
                val bRange = points.maxOf { channelValue(it, CHANNEL_B) } - points.minOf { channelValue(it, CHANNEL_B) }
                return when {
                    rRange >= gRange && rRange >= bRange -> CHANNEL_R
                    gRange >= bRange -> CHANNEL_G
                    else -> CHANNEL_B
                }
            }

            fun split(points: List<Int>, budget: Int): Node {
                if (points.size <= 1 || budget <= 1 || nextIndex >= maxColors) {
                    return leaf(points)
                }
                val ch = widestChannel(points)
                val range = points.maxOf { channelValue(it, ch) } - points.minOf { channelValue(it, ch) }
                if (range == 0) return leaf(points)

                val sorted = points.sortedBy { channelValue(it, ch) }
                val medianPos = sorted.size / 2
                val threshold = channelValue(sorted[medianPos - 1], ch)
                val left = sorted.subList(0, medianPos)
                val right = sorted.subList(medianPos, sorted.size)
                val leftBudget = budget / 2
                return Node.Split(ch, threshold, split(left, leftBudget), split(right, budget - leftBudget))
            }

            val root = if (samplePixels.isEmpty()) leaf(emptyList()) else split(samplePixels.toList(), maxColors)
            return MedianCutPalette(root, colors)
        }
    }
}
