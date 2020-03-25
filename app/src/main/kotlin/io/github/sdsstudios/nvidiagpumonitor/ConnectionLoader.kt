package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.loader.content.CursorLoader
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ

class ConnectionLoader(
    context: Context, uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?
) : CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder) {

    @RequiresPermission(PERMISSION_READ)
    override fun loadInBackground(): Cursor? {
        var cursor: Cursor? = null
        try {
            cursor = super.loadInBackground()
        } catch (e: IllegalStateException) {
            Log.e(this.javaClass.simpleName, "", e)
        }

        return cursor
    }
}
