package com.carstenkeller.videocutter.extract

enum class ExtractFormat(val label: String) {
    GIF("GIF"),
    MP4("MP4"),
}

/**
 * Feste Längen-Voreinstellungen für den Schnellausschnitt. GIF ist bewusst
 * nur bis zu einer gewissen Länge erlaubt, da die Datei sonst sehr groß
 * und die Erzeugung spürbar langsam wird.
 */
enum class ExtractDurationPreset(val seconds: Int, val allowsGif: Boolean) {
    SHORT(2, true),
    MEDIUM(5, true),
    LONG(10, false),
}
