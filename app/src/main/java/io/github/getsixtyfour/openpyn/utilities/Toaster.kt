package io.github.getsixtyfour.openpyn.utilities

import android.content.Context
import es.dmoral.toasty.Toasty
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnOutputLineListener

class Toaster(context: Context) : OnOutputLineListener {
    private val mContext: Context = context.applicationContext
    override fun spam(message: CharSequence) {
        Toasty.info(mContext, message, 1, false).show()
    }

    override fun debug(message: CharSequence) {
        Toasty.info(mContext, message, 1, false).show()
    }

    override fun verbose(message: CharSequence) {
        Toasty.info(mContext, message, 1, false).show()
    }

    override fun info(message: CharSequence) {
        Toasty.info(mContext, message, 1, false).show()
    }

    override fun notice(message: CharSequence) {
        Toasty.info(mContext, message, 1, false).show()
    }

    override fun warning(message: CharSequence) {
        Toasty.warning(mContext, message, 1, false).show()
    }

    override fun success(message: CharSequence) {
        Toasty.success(mContext, message, 1, false).show()
    }

    override fun error(message: CharSequence) {
        Toasty.error(mContext, message, 1, false).show()
    }

    override fun critical(message: CharSequence) {
        Toasty.error(mContext, message, 1, false).show()
    }
}