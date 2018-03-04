package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.Button

/**
 * Created by Seth on 04/03/18.
 */

class ConnectButton(
        context: Context?,
        attrs: AttributeSet?
) : Button(context, attrs) {

    fun applyConnectingStyle() {
        setText(R.string.btn_connecting)
        setBackgroundColor(getColor(R.color.colorConnecting))
        setTextColor(getColor(R.color.colorWhiteButtonText))
        isEnabled = false
    }

    fun applyConnectStyle() {
        setText(R.string.btn_connect)
        setBackgroundColor(getColor(R.color.colorConnect))
        setTextColor(getColor(R.color.colorWhiteButtonText))
        isEnabled = true
    }

    fun applyDisconnectStyle() {
        setText(R.string.btn_disconnect)
        setBackgroundColor(getColor(R.color.colorDisconnect))
        setTextColor(getColor(R.color.colorWhiteButtonText))
        isEnabled = true
    }

    private fun getColor(id: Int): Int {
        return ContextCompat.getColor(context, id)
    }
}