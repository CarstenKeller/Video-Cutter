package com.carstenkeller.videocutter.extract

enum class ExtractFormat(val label: String) {
    GIF("GIF"),
    MP4("MP4"),
}

/**
 * Feste Längen-Voreinstellungen für den Schnellausschnitt. Alle drei Presets
 * (2s/5s/10s) erlauben GIF. Nur eine frei gewählte, über 10s hinausgehende
 * Dauer ("Länger…") ist GIF-seitig gesperrt, da die Datei sonst sehr groß
 * und die Erzeugung spürbar langsam wird.
 */
enum class ExtractDurationPreset(val seconds: Int, val allowsGif: Boolean) {
    SHORT(2, true),
    MEDIUM(5, true),
    LONG(10, true),
}
