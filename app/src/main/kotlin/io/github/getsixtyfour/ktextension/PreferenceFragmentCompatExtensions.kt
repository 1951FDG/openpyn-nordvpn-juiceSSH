package io.github.getsixtyfour.ktextension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceFragmentCompat

fun PreferenceFragmentCompat.setTitle(activity: FragmentActivity) {
    val title: CharSequence? = preferenceScreen.title
    activity.title = title
    (activity as? AppCompatActivity)?.supportActionBar?.title = title
}
