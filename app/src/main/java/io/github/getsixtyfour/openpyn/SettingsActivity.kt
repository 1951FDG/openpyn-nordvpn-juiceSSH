package io.github.getsixtyfour.openpyn

import android.content.Context
import android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
import android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE
import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.MenuItem
import android.view.View
import androidx.preference.AndroidResources
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.preference.PreferenceScreen

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            // Override home navigation button to call onBackPressed (b/35152749).
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }

    /**
     * {@inheritDoc}
     */
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return SettingsSyncPreferenceFragment::class.java.name == fragmentName
    }

    /**
     * This fragment shows settings preferences only.
     */
    class SettingsSyncPreferenceFragment : PreferenceFragment(),
            PreferenceFragment.OnPreferenceStartScreenCallback {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(false)

            findPreference("pref_server")?.let { bindPreferenceSummaryToValue(it) }
            findPreference("pref_country")?.let { bindPreferenceSummaryToValue(it) }
            findPreference("pref_max_load")?.let { bindPreferenceSummaryToValue(it) }
            findPreference("pref_top_servers")?.let { bindPreferenceSummaryToValue(it) }
            findPreference("pref_pings")?.let { bindPreferenceSummaryToValue(it) }
            findPreference("pref_nvram_client")?.let { bindPreferenceSummaryToValue(it) }

            findPreference("pref_geo_client")?.let { bindPreferenceSummaryToValue(it) }
            findPreference("pref_api_ipdata")?.let { bindPreferenceSummaryToValue(it) }
            findPreference("pref_api_ipinfo")?.let { bindPreferenceSummaryToValue(it) }
            findPreference("pref_api_ipstack")?.let { bindPreferenceSummaryToValue(it) }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.pref_settings, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            view.fitsSystemWindows = true
            super.onViewCreated(view, savedInstanceState)
            setDivider(null)
        }

        override fun onPreferenceStartScreen(caller: PreferenceFragment?, pref: PreferenceScreen?): Boolean {
            val ft = fragmentManager.beginTransaction()
            val fragment = SettingsSyncPreferenceFragment()
            val args = Bundle()
            args.putString(PreferenceFragment.ARG_PREFERENCE_ROOT, pref?.key)
            fragment.arguments = args

            ft.add(AndroidResources.ANDROID_R_LIST_CONTAINER, fragment, pref?.key)
            ft.addToBackStack(null)
            ft.commit()
            return true
        }

        override fun getCallbackFragment(): PreferenceFragment {
            return this
        }
    }

    companion object {
        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        @Suppress("WeakerAccess")
        val sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)
                // Set the summary to reflect the new value.
                preference.summary = if (index >= 0) preference.entries[index] else null
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                when {
                    stringValue.isEmpty() -> preference.summary = "N/A"
                    preference.key.equals("pref_api_ipdata", true) -> preference.summary = "Available (SSL)"
                    preference.key.equals("pref_api_ipinfo", true) -> preference.summary = "Available (SSL)"
                    preference.key.startsWith("pref_api", true) -> preference.summary = "Available"
                    preference.key.equals("pref_server", true) && !validate(stringValue) -> {
                        return@OnPreferenceChangeListener false
                    }
                    else -> preference.summary = stringValue
                }
            }
            true
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and SCREENLAYOUT_SIZE_MASK >= SCREENLAYOUT_SIZE_XLARGE
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see sBindPreferenceSummaryToValueListener
         */
        fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
            // Trigger the listener immediately with the preference's
            // current value.
            val newValue = getDefaultSharedPreferences(preference.context).getString(preference.key, "")
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue)
        }

        @Suppress("WeakerAccess")
        fun validate(str: String): Boolean {
            val regex = Regex("""^[a-z]{2}\d{1,4}$""")
            if (regex.matches(str)) {
                return hashSetOf(
                        "al", "ar", "au", "at", "az", "be", "ba", "br", "bg", "ca", "cl",
                        "cr", "hr", "cy", "cz", "dk", "eg", "ee", "fi", "fr", "ge", "de",
                        "gr", "hk", "hu", "is", "in", "id", "ie", "il", "it", "jp", "lv",
                        "lu", "mk", "my", "mx", "md", "nl", "nz", "no", "pl", "pt", "ro",
                        "ru", "rs", "sg", "sk", "si", "za", "kr", "es", "se", "ch", "tw",
                        "th", "tr", "ua", "ae", "gb", "us", "vn").contains(str.take(2))
            }

            return false
        }
    }
}
