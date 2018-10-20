package io.github.sdsstudios.nvidiagpumonitor

import android.database.Cursor

/**
 * Created by Seth on 04/03/18.
 */

interface ConnectionListLoaderFinishedCallback {
    fun onLoaderFinished(newCursor: Cursor?)
}
