package io.github.getsixtyfour.openpyn.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.github.getsixtyfour.openpyn.R

@Suppress("unused")
class ConnectPreferenceFragment : PreferenceFragmentCompat() {
    override fun addPreferencesFromResource(preferencesResId: Int) {
        preferenceScreen = preferenceManager.inflateFromResource(context, preferencesResId, preferenceScreen)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_connect)
    }
}
