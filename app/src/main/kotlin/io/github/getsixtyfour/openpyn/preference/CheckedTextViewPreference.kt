package io.github.getsixtyfour.openpyn.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceViewHolder

@Suppress("unused")
class CheckedTextViewPreference : CheckBoxPreference {

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context, attrs, defStyleAttr, defStyleRes
    )

    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    )

    constructor(context: Context) : super(
        context
    )

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        (holder.findViewById(android.R.id.title) as Checkable).isChecked = mChecked
    }
}
