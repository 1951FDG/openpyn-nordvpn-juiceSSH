package io.github.getsixtyfour.openpyn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
import android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE
import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import io.github.getsixtyfour.openpyn.fragment.AboutPreferenceFragment
import io.github.getsixtyfour.openpyn.fragment.ApiPreferenceFragment
import io.github.getsixtyfour.openpyn.fragment.SettingsPreferenceFragment
import kotlinx.android.synthetic.main.mm2d_pac_content.toolbar
import net.mm2d.preference.Header
import net.mm2d.preference.PreferenceActivityCompat

/**
 * A [PreferenceActivityCompat] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : PreferenceActivityCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setProgressToolBar(this, toolbar, showHomeAsUp = true, showTitle = true)
        /*if (onIsHidingHeaders()) {
            setContentView(R.layout.content_preference)
            val initialFragment: String? = intent.getStringExtra(EXTRA_SHOW_FRAGMENT)
            val initialArguments: Bundle? = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)

            if (!isValidFragment(initialFragment)) {
                throw IllegalArgumentException("Invalid fragment for this activity: $initialFragment")
            }

            initialFragment?.let { startPreferenceFragment(it, initialArguments) }
        }*/
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String?): Boolean = when (fragmentName) {
        SettingsPreferenceFragment::class.java.name -> true
        ApiPreferenceFragment::class.java.name -> true
        AboutPreferenceFragment::class.java.name -> true
        else -> false
    }

    /**
     * Called when the activity needs its list of headers build.  By
     * implementing this and adding at least one item to the list, you
     * will cause the activity to run in its modern fragment mode.  Note
     * that this function may not always be called; for example, if the
     * activity has been asked to display a particular fragment without
     * the header list, there is no need to build the headers.
     *
     * @param target The list in which to place the headers.
     */
    override fun onBuildHeaders(target: MutableList<Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * Called to determine if the activity should run in multi-pane mode.
     */
    override fun onIsMultiPane(): Boolean = isXLargeTablet(this)

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

    /*
    /**
     * Called to determine whether the header list should be hidden.
     */
    private fun onIsHidingHeaders(): Boolean = intent.getBooleanExtra(EXTRA_NO_HEADERS, false)

    /**
     * Start a new fragment.
     *
     * @param fragmentName The name of the fragment to start.
     * @param args Optional arguments to supply to the fragment.
     */
    private fun startPreferenceFragment(fragmentName: String, args: Bundle?) {
        val fragmentManager = supportFragmentManager
        val fragment = fragmentManager.fragmentFactory.instantiate(classLoader, fragmentName)
        fragment.arguments = args
        fragmentManager.beginTransaction().replace(R.id.prefs, fragment).commitAllowingStateLoss()
    }*/
    companion object {

        /**
         * When starting this activity, the invoking Intent can contain this extra
         * boolean that the header list should not be displayed. This is most often
         * used in conjunction with {@link #EXTRA_SHOW_FRAGMENT} to launch
         * the activity to display a specific fragment that the user has navigated
         * to.
         */
        // const val EXTRA_NO_HEADERS: String = ":android:no_headers"
        /**
         * When starting this activity, the invoking Intent can contain this extra
         * string to specify which fragment should be initially displayed.
         */
        // const val EXTRA_SHOW_FRAGMENT: String = ":android:show_fragment"
        /**
         * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
         * this extra can also be specified to supply a Bundle of arguments to pass
         * to that fragment when it is instantiated during the initial creation
         * of PreferenceActivityCompat.
         */
        // const val EXTRA_SHOW_FRAGMENT_ARGUMENTS: String = ":android:show_fragment_args"
        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        @Suppress("WeakerAccess")
        val sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener
            get() = Preference.OnPreferenceChangeListener { preference, value ->
                val stringValue = value.toString()

                if (preference is ListPreference) {
                    // For list preferences, look up the correct display value in the preference's 'entries' list.
                    val index = preference.findIndexOfValue(stringValue)
                    // Set the summary to reflect the new value.
                    preference.summary = if (index >= 0) preference.entries[index] else null
                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    when {
                        stringValue.isEmpty() -> preference.summary = "Not set"
                        preference.key.equals("pref_api_ipdata", true) -> preference.summary = "Available (SSL)"
                        preference.key.equals("pref_api_ipinfo", true) -> preference.summary = "Available (SSL)"
                        preference.key.startsWith("pref_api", true) -> preference.summary = "Available"
                        preference.key.equals("pref_server", true) && !validate(preference, stringValue) -> {
                            return@OnPreferenceChangeListener false
                        }
                        preference.key.equals("pref_management_password", true) -> preference.summary = "Password has been set"
                        else -> preference.summary = stringValue
                    }
                }
                true
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
            // Trigger the listener immediately with the preference's current value.
            val newValue = getDefaultSharedPreferences(preference.context).getString(preference.key, "")
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue)
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and SCREENLAYOUT_SIZE_MASK >= SCREENLAYOUT_SIZE_XLARGE
        }

        fun startAboutFragment(activity: Activity) {
            //val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity)
            val intent = Intent(activity, SettingsActivity::class.java).apply {
                putExtra(EXTRA_SHOW_FRAGMENT, AboutPreferenceFragment::class.java.name)
                putExtra(EXTRA_NO_HEADERS, true)
            }
            ContextCompat.startActivity(activity, intent, null)
        }

        fun startSettingsFragment(activity: Activity) {
            //val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity)
            val intent = Intent(activity, SettingsActivity::class.java).apply {
                putExtra(EXTRA_SHOW_FRAGMENT, SettingsPreferenceFragment::class.java.name)
                putExtra(EXTRA_NO_HEADERS, true)
            }
            ContextCompat.startActivity(activity, intent, null)
        }

        @Suppress("WeakerAccess")
        fun validate(preference: Preference, str: String): Boolean {
            val regex = Regex("""^[a-z]{2}\d{1,4}$""")
            if (regex.matches(str)) {
                val set = preference.context.resources.getTextArray(R.array.pref_country_values).toHashSet().apply {
                    add("uk")
                }
                return set.contains(str.take(2))
            }

            return false
        }
    }
}
