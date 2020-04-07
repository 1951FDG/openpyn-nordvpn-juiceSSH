package io.github.getsixtyfour.openpyn.dialog

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.github.getsixtyfour.openpyn.R

/**
 * This fragment shows Connect settings preferences only.
 */
@Suppress("unused")
class ConnectPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_connect)
    }
}
