package io.github.getsixtyfour.openpyn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.material.transition.platform.MaterialFadeThrough
import io.github.getsixtyfour.openpyn.settings.AboutPreferenceFragment
import io.github.getsixtyfour.openpyn.settings.ApiPreferenceFragment
import io.github.getsixtyfour.openpyn.settings.GeneralPreferenceFragment
import io.github.getsixtyfour.openpyn.settings.ManagementPreferenceFragment
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

        window.apply {
            allowEnterTransitionOverlap = true
            enterTransition = MaterialFadeThrough()
        }

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = WindowInsetsCompat.Type.systemBars()
            val systemBarsInsets = insets.getInsets(systemBars)
            (v.layoutParams as? MarginLayoutParams)?.updateMargins(top = systemBarsInsets.top)
            WindowInsetsCompat.Builder(insets).apply {
                setInsets(systemBars, Insets.of(systemBarsInsets.left, 0, systemBarsInsets.right, systemBarsInsets.bottom))
            }.build()
        }

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Override home navigation button to call onBackPressed
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String?): Boolean = when (fragmentName) {
        AboutPreferenceFragment::class.java.name -> true
        ApiPreferenceFragment::class.java.name -> true
        ManagementPreferenceFragment::class.java.name -> true
        GeneralPreferenceFragment::class.java.name -> true
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

        /*
        /**
         * When starting this activity, the invoking Intent can contain this extra
         * boolean that the header list should not be displayed. This is most often
         * used in conjunction with {@link #EXTRA_SHOW_FRAGMENT} to launch
         * the activity to display a specific fragment that the user has navigated
         * to.
         */
        const val EXTRA_NO_HEADERS: String = ":android:no_headers"
        /**
         * When starting this activity, the invoking Intent can contain this extra
         * string to specify which fragment should be initially displayed.
         */
        const val EXTRA_SHOW_FRAGMENT: String = ":android:show_fragment"
        /**
         * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
         * this extra can also be specified to supply a Bundle of arguments to pass
         * to that fragment when it is instantiated during the initial creation
         * of PreferenceActivityCompat.
         */
        const val EXTRA_SHOW_FRAGMENT_ARGUMENTS: String = ":android:show_fragment_args"
        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
         */
        private val sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener
            get() = Preference.OnPreferenceChangeListener { preference, value ->
                val stringValue = "$value"

                if (preference is ListPreference) {
                    // For list preferences, look up the correct display value in the preference's 'entries' list
                    val index = preference.findIndexOfValue(stringValue)
                    // Set the summary to reflect the new value
                    preference.summary = if (index >= 0) preference.entries[index] else null
                } else {
                    val ctx = preference.context
                    when {
                        stringValue.isEmpty() -> preference.summary = ctx.getString(R.string.not_set)
                        // For all other preferences, set the summary to the value's simple string representation
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
            // Set the listener to watch for value changes
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
            // Trigger the listener immediately with the preference's current value
            val newValue = preference.sharedPreferences.getString(preference.key, "")
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue)
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        fun isXLargeTablet(context: Context): Boolean =
            context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE

        fun startSettingsFragment(activity: Activity) {
            val intent = Intent(activity, SettingsActivity::class.java).apply {
                putExtra(EXTRA_SHOW_FRAGMENT, GeneralPreferenceFragment::class.java.name)
                putExtra(EXTRA_NO_HEADERS, true)
            }
            ContextCompat.startActivity(activity, intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
        }
    }
}
