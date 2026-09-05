package com.carstenkeller.videocutter.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Speichert exportierte Videos im öffentlichen Album "Video Cutter" innerhalb der
 * Movies-Sammlung des Geräts. Der Album-Ordner wird von MediaStore automatisch
 * angelegt, sobald der erste Eintrag mit diesem RELATIVE_PATH eingefügt wird –
 * es ist also kein manuelles Erstellen des Verzeichnisses nötig oder (unter
 * Scoped Storage ab API 29) überhaupt direkt möglich.
 */
object MediaStoreSaver {

    private const val ALBUM_NAME = "Video Cutter"

    fun saveVideoToAlbum(context: Context, sourceFile: File, displayName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$ALBUM_NAME")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = resolver.insert(collection, values)
            ?: error("MediaStore-Eintrag konnte nicht erstellt werden")

        resolver.openOutputStream(itemUri)?.use { out ->
            sourceFile.inputStream().use { input -> input.copyTo(out) }
        } ?: error("Ausgabestream konnte nicht geöffnet werden")

        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(itemUri, values, null, null)

        return itemUri
    }
}
