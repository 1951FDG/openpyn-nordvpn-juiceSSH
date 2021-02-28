package io.github.sdsstudios.nvidiagpumonitor.loaders

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.loader.content.CursorLoader
import mu.KotlinLogging

class ConnectionLoader(
    context: Context,
    uri: Uri,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?
) : CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder) {

    private val logger = KotlinLogging.logger {}

    override fun loadInBackground(): Cursor? {
        var cursor: Cursor? = null
        try {
            cursor = super.loadInBackground()
        } catch (e: IllegalStateException) {
            logger.error(e) { "" }
        }

        return cursor
    }
}
