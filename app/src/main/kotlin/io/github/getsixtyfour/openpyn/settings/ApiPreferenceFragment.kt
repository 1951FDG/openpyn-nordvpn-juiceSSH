package io.github.getsixtyfour.openpyn.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.getsixtyfour.ktextension.setTitle
import io.github.getsixtyfour.openpyn.R

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

        findPreference<ListPreference>("pref_geo_client")?.run {
            summaryProvider = Preference.SummaryProvider<ListPreference> {
                when (val entry = it.entry) {
                    "IPData" -> "\uD83D\uDEE1 $entry (key required)"
                    "IPInfo" -> "\uD83D\uDEE1 $entry (key optional)"
                    "IPStack" -> "$entry (key required)"
                    null, "" -> it.context.getString(R.string.not_set)
                    else -> entry
                }
            }
        }

        findPreference<EditTextPreference>("pref_api_ipdata")?.run {
            setSummaryProvider(::provideSummary)
        }

        findPreference<EditTextPreference>("pref_api_ipinfo")?.run {
            setSummaryProvider(::provideSummary)
        }

        findPreference<EditTextPreference>("pref_api_ipstack")?.run {
            setSummaryProvider(::provideSummary)
        }
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

    companion object {
        fun provideSummary(preference: Preference): CharSequence {
            return when (preference.sharedPreferences.getString(preference.key, null)) {
                null, "" -> preference.context.getString(R.string.not_set)
                else -> preference.context.getString(R.string.key_set)
            }
        }
    }
}
