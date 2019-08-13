package io.github.getsixtyfour.openpyn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
import android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.preference.PreferenceScreen
import com.eggheadgames.aboutbox.AboutBoxUtils
import com.eggheadgames.aboutbox.AboutBoxUtils.openHTMLPage
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.share.EmailUtil
import com.eggheadgames.aboutbox.share.ShareUtil
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
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

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
        SettingsSyncPreferenceFragment::class.java.name -> true
        ApiSyncPreferenceFragment::class.java.name -> true
        AboutSyncPreferenceFragment::class.java.name -> true
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

    /**
     * This fragment shows General settings preferences only.
     */
    class SettingsSyncPreferenceFragment : PreferenceFragmentCompat(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

        override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
            // Instantiate the new Fragment
            val activity = requireActivity()
            val fragmentManager = activity.supportFragmentManager
            val args = pref.extras
            val fragment = fragmentManager.fragmentFactory.instantiate(activity.classLoader, pref.fragment)
            fragment.arguments = args
            fragment.setTargetFragment(caller, 0)
            fragmentManager.beginTransaction().replace((view!!.parent as View).id, fragment).addToBackStack(null).commit()

            return true
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(false)

            findPreference<Preference>("pref_server")?.let { bindPreferenceSummaryToValue(it) }
            findPreference<Preference>("pref_country")?.let { bindPreferenceSummaryToValue(it) }
            findPreference<Preference>("pref_max_load")?.let { bindPreferenceSummaryToValue(it) }
            findPreference<Preference>("pref_top_servers")?.let { bindPreferenceSummaryToValue(it) }
            findPreference<Preference>("pref_pings")?.let { bindPreferenceSummaryToValue(it) }
            findPreference<Preference>("pref_log_level")?.let { bindPreferenceSummaryToValue(it) }
            findPreference<Preference>("pref_nvram_client")?.let { bindPreferenceSummaryToValue(it) }
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
    }

    /**
     * This fragment shows API settings preferences only.
     */
    class ApiSyncPreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(false)

            findPreference<Preference>("pref_geo_client")?.let { bindPreferenceSummaryToValue(it) }
            findPreference<Preference>("pref_api_ipdata")?.let { bindPreferenceSummaryToValue(it) }
            findPreference<Preference>("pref_api_ipinfo")?.let { bindPreferenceSummaryToValue(it) }
            findPreference<Preference>("pref_api_ipstack")?.let { bindPreferenceSummaryToValue(it) }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.pref_api, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            view.fitsSystemWindows = true
            setDivider(null)
            super.onViewCreated(view, savedInstanceState)
        }

        override fun getCallbackFragment(): PreferenceFragmentCompat = this
    }

    /**
     * This fragment shows About settings preferences only.
     */
    class AboutSyncPreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(false)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val activity = requireActivity()
            val root = preferenceManager.createPreferenceScreen(activity)
            val config = AboutConfig.getInstance()

            root.setTitle(R.string.title_activity_about)

            addAboutPreferences(activity, root, config)

            addSupportPreferences(activity, root, config)

            addOtherPreferences(activity, root, config)

            preferenceScreen = root
            val title: CharSequence? = preferenceScreen.title
            // Set the title of the activity
            activity.title = title
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            view.fitsSystemWindows = true
            setDivider(null)
            super.onViewCreated(view, savedInstanceState)
        }

        override fun getCallbackFragment(): PreferenceFragmentCompat {
            return this
        }

        private fun addAboutPreferences(activity: Activity, root: PreferenceScreen, config: AboutConfig) {
            val category = PreferenceCategory(activity)
            category.title = activity.getString(R.string.pref_category_about)

            root.addPreference(category)

            category.addPreference(getPreference(
                activity,
                R.string.egab_author,
                config.author,
                R.drawable.ic_person_black_24dp,
                Preference.OnPreferenceClickListener {
                    openHTMLPage(activity, config.companyHtmlPath)
                    true
                }
            ))

            category.addPreference(getPreference(
                activity,
                R.string.egab_version,
                config.version,
                R.drawable.ic_info_outline_black_24dp,
                Preference.OnPreferenceClickListener {
                    openHTMLPage(activity, config.webHomePage)
                    true
                }
            ))

            category.addPreference(getPreference(
                activity,
                R.string.egab_changelog,
                null,
                R.drawable.ic_history_black_24dp,
                Preference.OnPreferenceClickListener {
                    openHTMLPage(activity, config.webHomePage + "/releases")
                    true
                }
            ))
        }

        private fun addSupportPreferences(activity: Activity, root: PreferenceScreen, config: AboutConfig) {
            val category = PreferenceCategory(activity)
            category.title = activity.getString(R.string.pref_category_support)

            root.addPreference(category)

            category.addPreference(getPreference(
                activity,
                R.string.egab_submit_issue,
                null,
                R.drawable.ic_bug_report_black_24dp,
                Preference.OnPreferenceClickListener {
                    openHTMLPage(activity, config.webHomePage + "/issues/new")
                    true
                }
            ))

            category.addPreference(getPreference(
                activity,
                R.string.egab_contact_support,
                null,
                R.drawable.ic_email_black_24dp,
                Preference.OnPreferenceClickListener {
                    EmailUtil.contactUs(activity)
                    true
                }
            ))
        }

        private fun addOtherPreferences(activity: Activity, root: PreferenceScreen, config: AboutConfig) {
            val category = PreferenceCategory(activity)
            category.title = activity.getString(R.string.pref_category_other)

            root.addPreference(category)

            category.addPreference(getPreference(
                activity,
                R.string.egab_leave_review,
                null,
                R.drawable.ic_star_black_24dp,
                Preference.OnPreferenceClickListener {
                    AboutBoxUtils.openApp(activity, config.buildType, config.packageName)
                    true
                }
            ))

            category.addPreference(getPreference(
                activity,
                R.string.egab_share,
                null,
                R.drawable.ic_share_black_24dp,
                Preference.OnPreferenceClickListener {
                    ShareUtil.share(activity)
                    true
                }
            ))

            category.addPreference(getPreference(
                activity,
                R.string.egab_licenses,
                null,
                R.drawable.ic_copyleft_green_24dp,
                Preference.OnPreferenceClickListener {
                    OssLicensesMenuActivity.setActivityTitle(getString(R.string.title_activity_licenses))
                    val intent = Intent(activity, OssLicensesMenuActivity::class.java)
                    ContextCompat.startActivity(activity, intent, null)
                    true
                }
            ))
        }

        private fun getPreference(
            context: Context,
            titleResId: Int?,
            summary: String?,
            iconResId: Int?,
            listener: Preference.OnPreferenceClickListener?
        ): Preference {
            val preference = Preference(context)
            iconResId?.let { preference.icon = ContextCompat.getDrawable(context, it) }
            titleResId?.let { preference.title = context.getString(it) }
            summary?.let { preference.summary = it }
            listener?.let { preference.onPreferenceClickListener = listener }
            return preference
        }
    }

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
                putExtra(EXTRA_SHOW_FRAGMENT, SettingsActivity.AboutSyncPreferenceFragment::class.java.name)
                putExtra(EXTRA_NO_HEADERS, true)
            }
            ContextCompat.startActivity(activity, intent, null)
        }

        fun startSettingsFragment(activity: Activity) {
            //val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity)
            val intent = Intent(activity, SettingsActivity::class.java).apply {
                putExtra(EXTRA_SHOW_FRAGMENT, SettingsActivity.SettingsSyncPreferenceFragment::class.java.name)
                putExtra(EXTRA_NO_HEADERS, true)
            }
            ContextCompat.startActivity(activity, intent, null)
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
                    "th", "tr", "ua", "ae", "uk", "us", "vn"
                ).contains(str.take(2))
            }

            return false
        }
    }
}
