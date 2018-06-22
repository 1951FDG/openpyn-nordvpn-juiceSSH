package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.support.annotation.UiThread
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet

/**
 * Created by Seth on 04/03/18.
 */

class ConnectButton(
        context: Context?,
        attrs: AttributeSet?
) : AppCompatButton(context, attrs) {

    @UiThread
    fun applyConnectingStyle() {
        setText(R.string.btn_connecting)
        ViewCompat.setBackgroundTintList(this, ContextCompat.getColorStateList(context, R.color.colorConnecting))
        setTextColor(getColor(R.color.colorWhiteButtonText))
        isClickable = false
        isEnabled = true
    }

    @UiThread
    fun applyConnectStyle() {
        setText(R.string.btn_connect)
        ViewCompat.setBackgroundTintList(this, ContextCompat.getColorStateList(context, R.color.colorConnect))
        setTextColor(getColor(R.color.colorWhiteButtonText))
        isClickable = true
        isEnabled = true
    }

    @UiThread
    fun applyDisconnectingStyle() {
        setText(R.string.btn_disconnecting)
        ViewCompat.setBackgroundTintList(this, ContextCompat.getColorStateList(context, R.color.colorConnecting))
        setTextColor(getColor(R.color.colorWhiteButtonText))
        isClickable = false
        isEnabled = true
    }

    @UiThread
    fun applyDisconnectStyle() {
        setText(R.string.btn_disconnect)
        ViewCompat.setBackgroundTintList(this, ContextCompat.getColorStateList(context, R.color.colorDisconnect))
        setTextColor(getColor(R.color.colorWhiteButtonText))
        isClickable = true
        isEnabled = true
    }

    private fun getColor(id: Int): Int {
        return ContextCompat.getColor(context, id)
    }
}