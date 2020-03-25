package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections

/**
 * Created by Seth on 04/03/18.
 */
@MainThread
class ConnectionListLoader(
    private val mCtx: Context, private val mListener: OnLoaderChangedListener
) : LoaderManager.LoaderCallbacks<Cursor> {

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val selection = "type = ?"
        val selectionArgs = arrayOf(Connections.TYPE_SSH.toString())

        return ConnectionLoader(
            mCtx, Connections.CONTENT_URI, Connections.PROJECTION, selection, selectionArgs, Connections.SORT_ORDER_DEFAULT
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        mListener.onLoaderChanged(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mListener.onLoaderChanged(null)
    }

    interface OnLoaderChangedListener {

        fun onLoaderChanged(newCursor: Cursor?)
    }
}
