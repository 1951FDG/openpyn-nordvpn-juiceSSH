package io.github.sdsstudios.nvidiagpumonitor.loaders

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.Loader
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnLoaderChangedListener

@MainThread
class ConnectionListLoader(
    private val mCtx: Context,
    private val mListener: OnLoaderChangedListener
) : LoaderCallbacks<Cursor> {

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val selection = "type = ?"
        val selectionArgs = arrayOf(Connections.TYPE_SSH.toString())

        return ConnectionLoader(
            context = mCtx,
            uri = Connections.CONTENT_URI,
            projection = Connections.PROJECTION,
            selection = selection,
            selectionArgs = selectionArgs,
            sortOrder = Connections.SORT_ORDER_DEFAULT
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        mListener.onLoaderChanged(cursor)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mListener.onLoaderChanged(null)
    }
}
