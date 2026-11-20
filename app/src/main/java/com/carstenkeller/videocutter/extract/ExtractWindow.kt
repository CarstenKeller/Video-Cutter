package com.carstenkeller.videocutter.extract

/**
 * Schnellausschnitt-Fenster fester Länge, das auf der Zeitleiste verschoben
 * wird (siehe [com.carstenkeller.videocutter.ui.components.TimelineView]).
 * [startUs]/[durationUs] sind absolute Mikrosekunden innerhalb der
 * Quelldatei des aktiven Clips.
 */
data class ExtractWindow(
    val startUs: Long,
    val durationUs: Long,
)
