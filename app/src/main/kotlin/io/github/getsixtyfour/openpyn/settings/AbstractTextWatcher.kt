package io.github.getsixtyfour.openpyn.settings

import android.content.Context
import android.text.Editable
import android.text.TextUtils.TruncateAt.END
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import io.github.getsixtyfour.openpyn.R

abstract class AbstractTextWatcher(protected val textInputLayout: TextInputLayout) : TextWatcher {

    protected abstract val submitButtonId: Int
    protected val ctx: Context = textInputLayout.context.applicationContext
    private val button by lazy { textInputLayout.rootView.findViewById(submitButtonId) as? Button }

    init {
        textInputLayout.isHelperTextEnabled = true
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {
        when (val errorText = validate("$s")) {
            null -> {
                textInputLayout.isErrorEnabled = false
                button?.isEnabled = true
            }
            else -> {
                textInputLayout.isErrorEnabled = true
                val errorView: TextView = textInputLayout.findViewById(R.id.textinput_error)
                errorView.maxLines = 1
                errorView.ellipsize = END
                textInputLayout.error = errorText
                button?.isEnabled = false
            }
        }
    }

    protected abstract fun validate(s: String): String?
}
