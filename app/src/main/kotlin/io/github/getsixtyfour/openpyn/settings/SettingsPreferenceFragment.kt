package io.github.getsixtyfour.openpyn.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.onRefreshItemSelected
import io.github.getsixtyfour.openpyn.settings.SettingsActivity.Companion

/**
 * This fragment shows General settings preferences only.
 */
class SettingsPreferenceFragment : PreferenceFragmentCompat(), OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        findPreference<Preference>("pref_server")?.let(Companion::bindPreferenceSummaryToValue)
        findPreference<Preference>("pref_country")?.let(Companion::bindPreferenceSummaryToValue)
        findPreference<Preference>("pref_max_load")?.let(Companion::bindPreferenceSummaryToValue)
        findPreference<Preference>("pref_top_servers")?.let(Companion::bindPreferenceSummaryToValue)
        // findPreference<Preference>("pref_pings")?.let(Companion::bindPreferenceSummaryToValue)
        findPreference<Preference>("pref_log_level")?.let(Companion::bindPreferenceSummaryToValue)
        findPreference<Preference>("pref_nvram_client")?.let(Companion::bindPreferenceSummaryToValue)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.pref_settings, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.fitsSystemWindows = true
        setDivider(null)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun getCallbackFragment(): PreferenceFragmentCompat = this

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                onRefreshItemSelected(requireActivity(), item)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val activity = requireActivity()
        val fragmentManager = activity.supportFragmentManager
        val args = pref.extras
        val fragment = fragmentManager.fragmentFactory.instantiate(activity.classLoader, pref.fragment)
        fragment.arguments = args
        fragment.setTargetFragment(caller, 0)
        fragmentManager.beginTransaction().replace((requireView().parent as View).id, fragment).addToBackStack(null).commit()

        return true
    }
}
