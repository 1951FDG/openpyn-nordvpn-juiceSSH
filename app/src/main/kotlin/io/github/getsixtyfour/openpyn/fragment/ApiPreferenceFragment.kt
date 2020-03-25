package io.github.getsixtyfour.openpyn.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.getsixtyfour.ktextension.setTitle
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.SettingsActivity.Companion

/**
 * This fragment shows API settings preferences only.
 */
class ApiPreferenceFragment : PreferenceFragmentCompat() {

    override fun onDetach() {
        super.onDetach()

        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(false)

        findPreference<Preference>("pref_geo_client")?.let(Companion::bindPreferenceSummaryToValue)
        findPreference<Preference>("pref_api_ipdata")?.let(Companion::bindPreferenceSummaryToValue)
        findPreference<Preference>("pref_api_ipinfo")?.let(Companion::bindPreferenceSummaryToValue)
        findPreference<Preference>("pref_api_ipstack")?.let(Companion::bindPreferenceSummaryToValue)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.pref_api, rootKey)
        setTitle(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.fitsSystemWindows = true
        setDivider(null)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun getCallbackFragment(): PreferenceFragmentCompat = this
}
