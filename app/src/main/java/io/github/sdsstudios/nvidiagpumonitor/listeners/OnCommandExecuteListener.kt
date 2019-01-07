package io.github.sdsstudios.nvidiagpumonitor.listeners

import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate

interface OnCommandExecuteListener {
    fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String?>

    fun onConnect()

    fun onDisconnect()
}