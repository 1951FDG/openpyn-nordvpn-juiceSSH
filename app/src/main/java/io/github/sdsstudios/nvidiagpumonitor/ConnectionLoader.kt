package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.loader.content.CursorLoader

class ConnectionLoader : CursorLoader {

    constructor(context: Context) : super(context)

    constructor(
        context: Context, uri: Uri, projection: Array<String>?,
        selection: String?, selectionArgs: Array<String>?, sortOrder: String?
    ) : super(context, uri, projection, selection, selectionArgs, sortOrder)

    override fun loadInBackground(): Cursor? {
        var cursor: Cursor? = null
        try {
            cursor = super.loadInBackground()
        } catch (e: IllegalStateException) {
            Log.e(e.javaClass.name, e.message)
        }

        return cursor
    }
}
