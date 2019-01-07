package io.github.getsixtyfour.openpyn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
import android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE
import android.os.Bundle
import android.preference.PreferenceActivity
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragment
import androidx.preference.PreferenceScreen
import com.eggheadgames.aboutbox.AboutBoxUtils
import com.eggheadgames.aboutbox.AboutBoxUtils.openHTMLPage
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.share.EmailUtil
import com.eggheadgames.aboutbox.share.ShareUtil
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

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
class AboutActivity : AppCompatPreferenceActivity() {
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
        return AboutSyncPreferenceFragment::class.java.name == fragmentName
    }

    /**
     * This fragment shows settings preferences only.
     */
    class AboutSyncPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(false)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val tv = TypedValue()
            activity.theme.resolveAttribute(androidx.preference.R.attr.preferenceTheme, tv, true)
            var theme = tv.resourceId
            if (theme == 0) {
                // Fallback to default theme.
                theme = androidx.preference.R.style.PreferenceThemeOverlay
            }
            val context = ContextThemeWrapper(activity, theme)
            val root = preferenceManager.createPreferenceScreen(context)
            val config = AboutConfig.getInstance()

            addAboutPreferences(context, root, config)

            addSupportPreferences(context, root, config)

            addOtherPreferences(context, root, config)

            preferenceScreen = root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            view.fitsSystemWindows = true
            setDivider(null)
            super.onViewCreated(view, savedInstanceState)
        }

        override fun getCallbackFragment(): PreferenceFragment {
            return this
        }

        private fun addOtherPreferences(context: ContextThemeWrapper, root: PreferenceScreen, config: AboutConfig) {
            val category = PreferenceCategory(context)
            category.title = context.getString(R.string.pref_category_other)

            root.addPreference(category)

            category.addPreference(getPreference(
                    context,
                    R.string.egab_leave_review,
                    null,
                    R.drawable.ic_star_black_24dp,
                    Preference.OnPreferenceClickListener {
                        AboutBoxUtils.openApp(activity, config.buildType, config.packageName)
                        true
                    }
            ))

            category.addPreference(getPreference(
                    context,
                    R.string.egab_share,
                    null,
                    R.drawable.ic_share_black_24dp,
                    Preference.OnPreferenceClickListener {
                        ShareUtil.share(activity)
                        true
                    }
            ))

            category.addPreference(getPreference(
                    context,
                    R.string.egab_licenses,
                    null,
                    R.drawable.ic_copyleft_green_24dp,
                    Preference.OnPreferenceClickListener {
                        val intent = Intent(activity, OssLicensesMenuActivity::class.java)
                        intent.putExtra("title", getString(R.string.menu_licenses))
                        ActivityCompat.startActivity(activity, intent, null)
                        true
                    }
            ))
        }

        private fun addSupportPreferences(context: ContextThemeWrapper, root: PreferenceScreen, config: AboutConfig) {
            val category = PreferenceCategory(context)
            category.title = context.getString(R.string.pref_category_support)

            root.addPreference(category)

            category.addPreference(getPreference(
                    context,
                    R.string.egab_submit_issue,
                    null,
                    R.drawable.ic_bug_report_black_24dp,
                    Preference.OnPreferenceClickListener {
                        openHTMLPage(activity, config.webHomePage + "/issues/new")
                        true
                    }
            ))

            category.addPreference(getPreference(
                    context,
                    R.string.egab_contact_support,
                    null,
                    R.drawable.ic_email_black_24dp,
                    Preference.OnPreferenceClickListener {
                        EmailUtil.contactUs(activity)
                        true
                    }
            ))
        }

        private fun addAboutPreferences(context: ContextThemeWrapper, root: PreferenceScreen, config: AboutConfig) {
            val category = PreferenceCategory(context)
            category.title = context.getString(R.string.pref_category_about)

            root.addPreference(category)

            category.addPreference(getPreference(
                    context,
                    R.string.egab_author,
                    config.author,
                    R.drawable.ic_person_black_24dp,
                    Preference.OnPreferenceClickListener {
                        openHTMLPage(activity, config.companyHtmlPath)
                        true
                    }
            ))

            category.addPreference(getPreference(
                    context,
                    R.string.egab_version,
                    config.version,
                    R.drawable.ic_info_outline_black_24dp,
                    Preference.OnPreferenceClickListener {
                        openHTMLPage(activity, config.webHomePage)
                        true
                    }
            ))

            category.addPreference(getPreference(
                    context,
                    R.string.egab_changelog,
                    null,
                    R.drawable.ic_history_black_24dp,
                    Preference.OnPreferenceClickListener {
                        openHTMLPage(activity, config.webHomePage + "/releases")
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
        fun launch(activity: Activity) {
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity)
            val intent = Intent(activity, AboutActivity::class.java).apply {
                putExtra(EXTRA_SHOW_FRAGMENT, AboutActivity.AboutSyncPreferenceFragment::class.java.name)
                putExtra(EXTRA_NO_HEADERS, true)
            }
            ActivityCompat.startActivity(activity, intent, options.toBundle())
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and SCREENLAYOUT_SIZE_MASK >= SCREENLAYOUT_SIZE_XLARGE
        }
    }
}
