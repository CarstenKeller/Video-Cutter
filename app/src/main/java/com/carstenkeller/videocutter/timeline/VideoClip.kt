package com.carstenkeller.videocutter.timeline

import android.net.Uri
import java.util.UUID

/**
 * Ein einzelner Abschnitt in der Timeline: Referenz auf eine Quelldatei plus
 * Trimm-Grenzen (in Mikrosekunden) innerhalb dieser Quelle.
 */
data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri,
    val sourceDurationUs: Long,
    val trimStartUs: Long = 0L,
    val trimEndUs: Long = sourceDurationUs,
) {
    val durationUs: Long get() = (trimEndUs - trimStartUs).coerceAtLeast(MIN_DURATION_US)

    companion object {
        /** Mindestlänge eines Clips, um degenerierte (Null-Länge) Abschnitte zu vermeiden. */
        const val MIN_DURATION_US = 200_000L
    }
}
