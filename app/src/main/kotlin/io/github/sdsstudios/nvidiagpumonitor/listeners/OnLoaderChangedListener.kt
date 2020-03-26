package io.github.sdsstudios.nvidiagpumonitor.listeners

import android.database.Cursor

interface OnLoaderChangedListener {

    fun onLoaderChanged(newCursor: Cursor?)
}
