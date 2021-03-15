package io.github.getsixtyfour.openpyn.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import io.github.getsixtyfour.openpyn.R
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnOutputLineListener

/**
 * @see <a href="https://github.com/xolox/python-coloredlogs/blob/master/coloredlogs/__init__.py">python-coloredlogs</a>
 * <br>
 * @see <a href="https://material.io/design/usability/accessibility.html#color-and-contrast">Accessibility - Material Design</a>
 * <br>
 * @see <a href="https://github.com/LeaVerou/contrast-ratio">Contrast Ratio</a>
 */
open class Toaster(context: Context) : OnOutputLineListener {

    private val mContext: Context = context.applicationContext
    private val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun spam(message: CharSequence) {
        makeToast(message, Toast.LENGTH_LONG, R.color.toast_spam_color, R.color.toast_dark_color_on_background).show()
    }

    override fun debug(message: CharSequence) {
        makeToast(message, Toast.LENGTH_LONG, R.color.toast_debug_color, R.color.toast_dark_color_on_background).show()
    }

    override fun verbose(message: CharSequence) {
        makeToast(message, Toast.LENGTH_LONG, R.color.toast_verbose_color, R.color.toast_dark_color_on_background).show()
    }

    override fun info(message: CharSequence) {
        makeToast(message).show()
    }

    override fun notice(message: CharSequence) {
        makeToast(message).show()
    }

    override fun warning(message: CharSequence) {
        makeToast(message, Toast.LENGTH_LONG, R.color.toast_warning_color, R.color.toast_color_on_background).show()
    }

    override fun success(message: CharSequence) {
        makeToast(message, Toast.LENGTH_LONG, R.color.toast_success_color, R.color.toast_dark_color_on_background).show()
    }

    override fun error(message: CharSequence) {
        makeToast(message, Toast.LENGTH_LONG, R.color.toast_error_color, R.color.toast_dark_color_on_background).show()
    }

    override fun critical(message: CharSequence) {
        makeToast(message, Toast.LENGTH_LONG, R.color.toast_critical_color, R.color.toast_dark_color_on_background).show()
    }

    @SuppressLint("InflateParams")
    @CheckResult
    protected fun makeToast(
        message: CharSequence, duration: Int = Toast.LENGTH_LONG, @ColorRes tintColorId: Int? = null, @ColorRes textColorId: Int? = null
    ): Toast {
        val view = (mInflater.inflate(R.layout.toast_notification, null) as ViewGroup).apply {
            tintColorId?.let { ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(ContextCompat.getColor(mContext, it))) }
        }

        view.findViewById<TextView>(android.R.id.message).apply {
            setText(message)
            textColorId?.let { setTextColor(ContextCompat.getColor(mContext, it)) }
        }

        return Toast(mContext).apply {
            setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, yOffset)
            setDuration(duration)
            setView(view)
        }
    }
}
