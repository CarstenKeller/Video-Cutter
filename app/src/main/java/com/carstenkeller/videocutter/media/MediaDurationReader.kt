package com.carstenkeller.videocutter.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object MediaDurationReader {
    fun readDurationUs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            durationMs * 1000
        } finally {
            retriever.release()
        }
    }
}
