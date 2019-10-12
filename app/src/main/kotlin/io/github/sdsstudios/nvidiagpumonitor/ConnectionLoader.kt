package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.loader.content.CursorLoader

class ConnectionLoader(
    context: Context,
    uri: Uri,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?
) : CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder) {

    override fun loadInBackground(): Cursor? {
        var cursor: Cursor? = null
        try {
            cursor = super.loadInBackground()
        } catch (e: IllegalStateException) {
            e.message?.let { Log.e(e.javaClass.name, it) }
        }

        return cursor
    }
}
